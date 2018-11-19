package facemywrath.warpshop.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import facemywrath.warpshop.commands.WarpCommand;
import net.md_5.bungee.api.ChatColor;

public class WarpManager {

	private HashMap<Player, Sorting> sorting = new HashMap<>();
	private List<Player> reversed = new ArrayList<>();
	private HashMap<String, Warp> warps = new HashMap<>();
	private WarpCommand cmd;
	private HashMap<Player, Integer> pages = new HashMap<>();

	private enum Sorting {
		VOTE_COUNT,WARP_NAME,OWNER_NAME,WORLD_NAME;
	}

	public WarpManager(WarpCommand cmd) { 
		this.cmd = cmd; 
		Animation<WarpManager> saveAnimation = new Animation(cmd.getMain()).addFrame(wm -> {
			WarpManager warpm = (WarpManager) wm;
			warpm.saveVotes();
			System.out.println("Warp Votes Saved");
		}, 300L).setLooping(true, 6000L);
		saveAnimation.animate(this);
	}


	public String createWarp(Player player, String name)
	{
		File warpsFile = new File(cmd.getMain().getDataFolder(), "warps.yml");
		if(!warpsFile.exists()) return "There is no warps file. Administration must restart the server.";
		FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
		if(config.contains(name)) return "A warp by that name already exists.";
		config.set(name + ".Owner", player.getName());
		config.set(name + ".Votes.Count", 0);
		config.set(name + ".Votes.Players", new ArrayList<String>());
		config.set(name + ".Location.World", player.getWorld().getName());
		ConfigurationSection section = config.getConfigurationSection(name + ".Location");
		section.set("X", player.getLocation().getX());
		section.set("Y", player.getLocation().getY());
		section.set("Z", player.getLocation().getZ());
		section.set("Yaw", player.getLocation().getYaw());
		section.set("Pitch", player.getLocation().getPitch());
		try {
			config.save(warpsFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!warps.containsKey(name))
			this.warps.put(name, new Warp(name, player, player.getLocation().clone(), 0, new ArrayList<String>()));
		cmd.getMain().getEcon().withdrawPlayer(player, cmd.getWarpCost());
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCharged &6$" + cmd.getWarpCost() + ". &aNew balance: " + cmd.getMain().getEcon().getBalance(player)));
		return "Warp " + name + " created.";
	}

	public Inventory getGui(Player player) {
		Inventory gui = Bukkit.createInventory(null, 54);
		if(!pages.containsKey(player)) pages.put(player, 1);
		for(int i = 0; i < gui.getSize(); i++) {
			boolean sec1 = i > 9 && i < 17;
			boolean sec2 = i > 18 && i < 26;
			boolean sec3 = i > 27 && i < 35;
			boolean sec4 = i > 36 && i < 44;
			if(!sec1 && !sec2 && !sec3 && !sec4)
				gui.setItem(i, new ItemCreator(Material.STAINED_GLASS_PANE).name(" ").build());
		}
		if(!sorting.containsKey(player)) sorting.put(player, Sorting.VOTE_COUNT);
		List<ItemStack> items = new ArrayList<>();
		switch(sorting.get(player))
		{
		case WARP_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getName().compareTo(warp2.getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case OWNER_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getOwner().getName().compareTo(warp2.getOwner().getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case WORLD_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getLocation().getWorld().getName().compareTo(warp2.getLocation().getWorld().getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case VOTE_COUNT:
			items = warps.values().stream().sorted((warp1, warp2) -> Integer.compare(warp2.getVotes(), warp1.getVotes())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			Collections.reverse(items);
			break;
		}
		if(reversed.contains(player))
			Collections.reverse(items);
		List<ItemStack> pageItems = items.stream().skip((pages.get(player)-1)*36).collect(Collectors.toList());
		for(int i = 0; i < 36; i++) {
			if(pageItems.size() > i)
				gui.addItem(pageItems.get(i));
		}
		boolean firstPage = pages.get(player) == 1;
		boolean lastPage = pageItems.size() <= 36;
		gui.setItem(47, new ItemCreator(firstPage?Material.END_CRYSTAL:Material.NETHER_STAR).name(firstPage?"You're on the first page already.":"Back a page").build());
		gui.setItem(51, new ItemCreator(lastPage?Material.END_CRYSTAL:Material.NETHER_STAR).name(lastPage?"You're on the last page already.":"Next Page").build());
		gui.setItem(3, new ItemCreator(Material.EMERALD).name("&9Sorting: " + StringUtils.capitaliseAllWords(sorting.get(player).toString().toLowerCase().replaceAll("_", " "))).lore("&eSorting methods:", "  &7&oVote count", "  &7&oWarp Name", "  &7&oOwner name", "  &7&oWorld name").build());
		gui.setItem(5, new ItemCreator(Material.EMERALD).name("&9Order: " + (reversed.contains(player)?"Descending":"Ascending")).lore("&7Click to reverse the order").build());
		return gui;
	}

	public void incrementPage(Player player) {
		pages.put(player, pages.get(player)+1);
		updateGui(player, player.getOpenInventory().getTopInventory());
	}

	public void decrementPage(Player player) {
		pages.put(player, pages.get(player)-1);
		updateGui(player, player.getOpenInventory().getTopInventory());
	}

	public void updateGui(Player player, Inventory gui) {
		gui.clear();
		if(!pages.containsKey(player)) pages.put(player, 1);
		for(int i = 0; i < gui.getSize(); i++) {
			boolean sec1 = i > 9 && i < 17;
			boolean sec2 = i > 18 && i < 26;
			boolean sec3 = i > 27 && i < 35;
			boolean sec4 = i > 36 && i < 44;
			if(!sec1 && !sec2 && !sec3 && !sec4)
				gui.setItem(i, new ItemCreator(Material.STAINED_GLASS_PANE).name(" ").build());
		}
		if(!sorting.containsKey(player)) sorting.put(player, Sorting.VOTE_COUNT);
		List<ItemStack> items = new ArrayList<>();
		switch(sorting.get(player))
		{
		case WARP_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getName().compareTo(warp2.getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case OWNER_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getOwner().getName().compareTo(warp2.getOwner().getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case WORLD_NAME:
			items = warps.values().stream().sorted((warp1, warp2) -> warp1.getLocation().getWorld().getName().compareTo(warp2.getLocation().getWorld().getName())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			break;
		case VOTE_COUNT:
			items = warps.values().stream().sorted((warp1, warp2) -> Integer.compare(warp2.getVotes(), warp1.getVotes())).map(warp -> warp.getItem(player)).collect(Collectors.toList());
			Collections.reverse(items);
			break;
		}
		if(reversed.contains(player))
			Collections.reverse(items);
		List<ItemStack> pageItems = items.stream().skip((pages.get(player)-1)*36).collect(Collectors.toList());
		for(int i = 0; i < 36; i++) {
			if(pageItems.size() > i)
				gui.addItem(pageItems.get(i));
		}
		boolean firstPage = pages.get(player) == 1;
		boolean lastPage = pageItems.size() <= 36;
		gui.setItem(47, new ItemCreator(firstPage?Material.END_CRYSTAL:Material.NETHER_STAR).name(firstPage?"You're on the first page already.":"Back a page").build());
		gui.setItem(51, new ItemCreator(lastPage?Material.END_CRYSTAL:Material.NETHER_STAR).name(lastPage?"You're on the last page already.":"Next Page").build());
		gui.setItem(3, new ItemCreator(Material.EMERALD).name("&9Sorting: " + StringUtils.capitaliseAllWords(sorting.get(player).toString().toLowerCase().replaceAll("_", " "))).lore("&eSorting methods:", "  &7&oVote count", "  &7&oWarp Name", "  &7&oOwner name", "  &7&oWorld name").build());
		gui.setItem(5, new ItemCreator(Material.EMERALD).name("&9Order: " + (reversed.contains(player)?"Descending":"Ascending")).lore("&7Click to reverse the order").build());
	}

	public void toggleOrder(Player player) {
		if(reversed.contains(player)) reversed.remove(player); else reversed.add(player);
		updateGui(player, player.getOpenInventory().getTopInventory());
	}

	public void incrementSorting(Player player) {
		if(!sorting.containsKey(player) || sorting.get(player) == Sorting.WORLD_NAME) {
			sorting.put(player, Sorting.VOTE_COUNT);
			updateGui(player, player.getOpenInventory().getTopInventory());
			return;
		}
		sorting.put(player, Sorting.values()[sorting.get(player).ordinal()+1]);
		updateGui(player, player.getOpenInventory().getTopInventory());
		return;
	}

	public Warp getWarp(ItemStack item) {
		for(Warp warp : warps.values())
		{
			if(ChatColor.stripColor(warp.getItem().getItemMeta().getDisplayName()).equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()))) return warp;
		}
		return null;
	}

	public String deleteWarp(CommandSender player, String name, boolean override)
	{
		File warpsFile = new File(cmd.getMain().getDataFolder(), "warps.yml");
		if(!warpsFile.exists()) return "There is no warps file. Administration must restart the server.";
		FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
		if(!config.contains(name)) return "No warp by that name exists.";
		if(!config.getString(name + ".Owner").equals(player.getName()) && !override){
			if(player.isOp() || player.hasPermission("warpshop.override")) return "That is not your warp. Add a -override at the end of the command to do it anyway.";
			else return "That is not your warp.";
		}
		String owner = config.getString(name + ".Owner");
		OfflinePlayer target = Bukkit.getOfflinePlayer(owner);
		int votes = config.getInt(name + ".Votes.Count");
		config.set(name + ".Owner", null);
		config.set(name + ".Votes.Count", null);
		config.set(name + ".Votes.Players", null);
		config.set(name + ".Votes", null);
		ConfigurationSection section = config.getConfigurationSection(name + ".Location");
		Location loc = new Location(Bukkit.getWorld(section.getString("World")), section.getDouble("X"), section.getDouble("Y"), section.getDouble("Z"));
		section.set("World", null);
		section.set("X", null);
		section.set("Y", null);
		section.set("Z", null);
		section.set("Yaw", null);
		section.set("Pitch", null);
		config.set(name + ".Location", null);
		config.set(name, null);
		try {
			config.save(warpsFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(warps.containsKey(name))
			this.warps.remove(name);
		return "Warp " + name + " deleted.";
	}

	public boolean warpExists(String warpName) {
		return warps.containsKey(warpName);
	}

	public Warp getWarp(String warpName) {
		return warps.get(warpName);
	}

	public int getWarpCount(String name){
		return (int) warps.values().stream().filter(warp -> warp.getOwner().getName().equalsIgnoreCase(name)).map(Warp::getVotes).count();
	}

	public void saveVotes() {
		File warpsFile = new File(cmd.getMain().getDataFolder(), "warps.yml");
		if(!warpsFile.exists()) System.out.println("There is no warps file. Administration must restart the server.");
		FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
		for(Warp warp : warps.values())
		{
			config.set(warp.getName() + ".Votes.Count", warp.getVotes());
			config.set(warp.getName() + ".Votes.Players", warp.getVotedPlayers());
		}
		try {
			config.save(warpsFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void loadWarps() {
		File warpsFile = new File(cmd.getMain().getDataFolder(), "warps.yml");
		if(!warpsFile.exists()) System.out.println("There is no warps file. Administration must restart the server.");
		FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
		if(config.getKeys(false).isEmpty()) return;
		for(String warpName : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(warpName);
			String owner = section.getString("Owner");
			OfflinePlayer target = Bukkit.getOfflinePlayer(owner);
			int votes = section.getInt("Votes.Count");	
			List<String> players = section.getStringList("Votes.Players");
			section = section.getConfigurationSection("Location");
			Location loc = new Location(Bukkit.getWorld(section.getString("World")), section.getDouble("X"), section.getDouble("Y"), section.getDouble("Z"));
			if(!warps.containsKey(warpName))
				warps.put(warpName, new Warp(warpName, target, loc, votes, players));
		}
	}

	public Collection<Warp> getWarps() {
		return warps.values();
	}

}
