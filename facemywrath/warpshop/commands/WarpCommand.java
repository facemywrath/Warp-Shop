package facemywrath.warpshop.commands;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import facemywrath.warpshop.main.Main;
import facemywrath.warpshop.util.Destination;
import facemywrath.warpshop.util.Events;
import facemywrath.warpshop.util.ItemCreator;
import facemywrath.warpshop.util.Warp;
import facemywrath.warpshop.util.WarpManager;
import net.md_5.bungee.api.chat.TextComponent;

public class WarpCommand implements CommandExecutor {

	private Main main;
	private WarpManager warpManager;
	private int maxWarps = 5;
	private int warpDelay = 5;
	private int warpCost = 4000;
	private List<World> enabledWorlds = new ArrayList<>();
	
	private List<Player> usingGui = new ArrayList<>();

	private HashMap<Player, Destination> warping = new HashMap<>();

	public WarpCommand(Main main) {
		this.main = main;
		warpManager = new WarpManager(this);
		warpManager.loadWarps();
		if(main.getConfig().contains("max-warps")) maxWarps = main.getConfig().getInt("max-warps");
		if(main.getConfig().contains("warp-cost")) warpCost = main.getConfig().getInt("warp-cost");
		if(main.getConfig().contains("warp-delay")) warpDelay = main.getConfig().getInt("warp-delay");
		if(main.getConfig().contains("enabled-worlds")) {
			List<String> worlds = main.getConfig().getStringList("enabled-worlds");
			worlds.forEach(worldName -> {
				if(Bukkit.getWorld(worldName) != null ) enabledWorlds.add(Bukkit.getWorld(worldName));
			});
		}
		Events.listen(main, PlayerMoveEvent.class, event -> {
			if(!warping.containsKey(event.getPlayer())) return;
			if(!event.getFrom().getBlock().equals(event.getTo().getBlock()))
			{
				warping.remove(event.getPlayer());
				event.getPlayer().sendMessage(trans("&cYou moved! Warp cancelled."));
			}
		});
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		if(cmd.getName().equalsIgnoreCase("warp"))
		{
			if(!(sender instanceof Player)) {
				if(args.length == 0 || args[0].equalsIgnoreCase("help"))
				{
					sender.sendMessage("Valid console commands:");
					sender.sendMessage("-/warp list");
					sender.sendMessage("-/warp delete");
					return true;
				}
				if(args[0].equalsIgnoreCase("list"))
				{
					sender.sendMessage("Valid Warps: " + warpManager.getWarps().stream().map(Warp::getName).collect(Collectors.joining(", ")));
					return true;
				}
				if(args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("d"))
				{
					if(args.length == 1)
					{
						sender.sendMessage("Invalid Syntax! /warp delete <warpname>");
						return true;
					}
					sender.sendMessage(warpManager.deleteWarp(sender, args[1], (args.length==3&&args[2].equalsIgnoreCase("-override"))?true:false));
					return true;
				}
				sender.sendMessage("Invalid Syntax! Try again.");
				return true;
			}
			Player player = (Player) sender;
			if(args.length == 0 || args[0].equalsIgnoreCase("help"))
			{
				player.sendMessage(trans("&b&lWarpShop Commands:"));
				player.sendMessage(trans("&7/goto (warp) &eto goto a warp"));
				player.sendMessage(trans("&7/warp list &eto list the warps in chat"));
				player.sendMessage(trans("&7/warp mine &eto list your warps in chat"));
				player.sendMessage(trans("&7/warp gui &eto open the warps gui"));
				player.sendMessage(trans("&7/warp delete (warp) &eto delete a warp"));
				player.sendMessage(trans("&7/warp buy (warp) &eto purchase a warp"));
				player.sendMessage(trans("&7/warp info &eto view WarpShop information"));
				return true;
			}
			if(args[0].equalsIgnoreCase("list"))
			{
				TextComponent msg = new TextComponent(ChatColor.GREEN + "Valid warps: ");
				 warpManager.getWarps().stream().sorted((warp1, warp2) -> Integer.compare(warp1.getVotes(), warp2.getVotes())).map(warp -> new ItemCreator(warp.getItem()).getTextComponent(ChatColor.AQUA + warp.getName())).forEach(text -> {msg.addExtra(text); msg.addExtra(", ");});
				 msg.setExtra(msg.getExtra().subList(0, msg.getExtra().size()-1));
				 sender.spigot().sendMessage(msg);
				return true;
			}
			if(args[0].equalsIgnoreCase("mine"))
			{
				TextComponent msg = new TextComponent(ChatColor.GREEN + "Valid warps: ");
				 warpManager.getWarps().stream().filter(warp -> warp.getOwner().equals(player)).sorted((warp1, warp2) -> Integer.compare(warp1.getVotes(), warp2.getVotes())).map(warp -> new ItemCreator(warp.getItem()).getTextComponent(ChatColor.AQUA + warp.getName())).forEach(text -> {msg.addExtra(text); msg.addExtra(", ");});
				 msg.setExtra(msg.getExtra().subList(0, msg.getExtra().size()-1));
				 sender.spigot().sendMessage(msg);
				return true;
			}
			if(args[0].equalsIgnoreCase("buy"))
			{
				if(args.length == 1)
				{
					player.sendMessage(trans("&4You must specify a warp name"));
					return true;
				}
				if(!enabledWorlds.contains(player.getWorld()))
				{
					player.sendMessage(trans("&4You can't buy warps in that world."));
					return true;
				}
				if(warpManager.getWarpCount(player.getName()) >= maxWarps)
				{
					player.sendMessage(trans("&4You have the maximum amount of warps already."));
					return true;
				}
				if(main.getEcon().getBalance(player) < warpCost)
				{
					player.sendMessage(trans("&4You don't have enough money to purchase that: &6" + main.getEcon().getBalance(player) + "/" + warpCost));
					return true;
				}
				player.sendMessage(warpManager.createWarp(player, args[1]));
				return true;
			}
			if(args[0].equalsIgnoreCase("gui"))
			{
				player.sendMessage(trans("&bOpening Warp GUI"));
				openGui(player);
				return true;
			}
			if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("information"))
			{
				player.sendMessage(trans("&b&lWarpShop Information:"));
				player.sendMessage(trans("&7Warp Cost: &6$" + warpCost));
				player.sendMessage(trans("&7Your Warps: &6" + warpManager.getWarpCount(player.getName()) + "/" + maxWarps));
				player.sendMessage(trans("&7Enabled Worlds: &6" + enabledWorlds.stream().map(World::getName).collect(Collectors.joining(", "))));
				return true;
			}
		}
		if(cmd.getName().equalsIgnoreCase("goto"))
		{
			if(!(sender instanceof Player))
			{
				sender.sendMessage("Only players can do that");
				return true;
			}
			Player player = (Player) sender;
			if(args.length == 0)
			{
				player.sendMessage(trans("&4You must specify a warp name"));
				return true;
			}
			if(!warpManager.warpExists(args[0]))
			{
				player.sendMessage(trans("&4That warp doesn't exist, capitalization matters!"));
				return true;
			}
			Warp warp = warpManager.getWarp(args[0]);
			warping.put(player, new Destination(warp, warpDelay));
			player.sendMessage(trans("&bWarping in " + warpDelay + " seconds. Don't move!"));
			main.getServer().getScheduler().runTaskLater(main, () -> {
				if(!warping.containsKey(player) && warping.get(player).equals(warp)) return;
				player.teleport(warp.getLocation());
				player.sendMessage(trans("&bYou were warped to " + warp.getName()));
				warping.remove(player);
			}, warpDelay*20L);
		}
		return true;
	}
	
	public void openGui(Player player) {
		player.openInventory(warpManager.getGui(player));
		usingGui.add(player);
	}
	
	public void closeGui(Player player) {
		if(usingGui(player))
		{
			player.closeInventory();
			usingGui.remove(player);	
		}
	}
	public boolean usingGui(Player player) { return usingGui.contains(player); }

	private String trans(String str) { return ChatColor.translateAlternateColorCodes('&', str); }

	public WarpManager getWarpManager() {
		return warpManager;
	}

	public int getWarpCost() {
		return warpCost;
	}

	public Main getMain() {
		return main;
	}

}
