package facemywrath.warpshop.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class Warp {

	private OfflinePlayer owner;
	private Location location;
	private int votes = 0;
	private ItemStack item;
	private String name;
	private List<OfflinePlayer> votedPlayers = new ArrayList<>();
	
	public Warp(String name, OfflinePlayer owner, Location loc, int votes, List<String> votedPlayers) {
		this.owner = owner;
		this.name = name;
		this.location = loc;
		this.votes = votes;
		this.votedPlayers = votedPlayers.stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
		DecimalFormat df = new DecimalFormat("#.00"); 
		item = new ItemCreator(Material.SKULL_ITEM).durability((short) 3).amount(votes==0?1:votes).setSkullOwner(owner.getName()).name("&bWarp: "+name).lore("&7Owner: " + owner.getName(), "&7Votes: " + votes, "&7Location: ", "  &7World: " + location.getWorld().getName(), "  &7X: " + df.format(location.getX()), "  &7Y: " + df.format(location.getY()), "  &7Z: " + df.format(location.getZ())).build();
	}

	public String getName() {
		return name;
	}

	public OfflinePlayer getOwner() {
		return owner;
	}

	public Location getLocation() {
		return location;
	}

	public int getVotes() {
		return votes;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public ItemStack getItem(Player player) {
		ItemStack newItem = item.clone();
		List<String> lore = newItem.getItemMeta().getLore();
		if(votedPlayers.contains(player))
			lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lYou voted for this already."));
		return new ItemCreator(item.clone()).lore(lore).build();
	}
	
	public void incrementVotes() {
		this.votes++;
		DecimalFormat df = new DecimalFormat("#.00"); 
		item = new ItemCreator(Material.SKULL_ITEM).durability((short) 3).amount(votes==0?1:votes).setSkullOwner(owner.getName()).name("&bWarp: "+name).lore("&7Owner: " + owner.getName(), "&7Votes: " + votes, "&7Location: ", "  &7World: " + location.getWorld().getName(), "  &7X: " + df.format(location.getX()), "  &7Y: " + df.format(location.getY()), "  &7Z: " + df.format(location.getZ())).build();
	
	}

	public List<OfflinePlayer> getVotedPlayers() {
		return this.votedPlayers;
	}

	public List<String> getVotedPlayerNames() {
		return this.votedPlayers.stream().map(OfflinePlayer::getName).collect(Collectors.toList());
	}

	public void addPlayerVote(Player player) {
		votedPlayers.add(player);
	}
	
}
