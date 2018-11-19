package facemywrath.warpshop.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import facemywrath.warpshop.commands.WarpCommand;
import facemywrath.warpshop.main.Main;
import facemywrath.warpshop.util.Events;
import facemywrath.warpshop.util.Warp;
import net.md_5.bungee.api.ChatColor;

public class VoteListener {


	public VoteListener(Main main, WarpCommand cmd) {
		Events.listen(main, InventoryClickEvent.class, event -> {
			if(event.getClickedInventory() == null) return;
			Player player = (Player) event.getWhoClicked();
			if(!cmd.usingGui(player)) return;	
			event.setCancelled(true);
			if(event.getCurrentItem() == null) return;
			if(event.getSlot() == 3)
			{
				cmd.getWarpManager().incrementSorting(player);
				return;
			}
			if(event.getSlot() == 5)
			{
				cmd.getWarpManager().toggleOrder(player);
				return;
			}
			if(event.getSlot() == 47)
			{
				if(event.getCurrentItem().getType() == Material.NETHER_STAR) cmd.getWarpManager().decrementPage(player);
				return;
			}
			if(event.getSlot() == 51)
			{
				if(event.getCurrentItem().getType() == Material.NETHER_STAR) cmd.getWarpManager().incrementPage(player);
				return;
			}
			if(cmd.getWarpManager().getWarp(event.getCurrentItem()) == null) return;
			Warp warp = cmd.getWarpManager().getWarp(event.getCurrentItem());
			if(warp.getVotedPlayers().contains(player))
			{
				player.sendMessage(ChatColor.DARK_RED + "You've already voted for " + ChatColor.AQUA + warp.getName());
				return;
			}
			warp.addPlayerVote(player);
			warp.incrementVotes();
			cmd.openGui(player);
		});
	}

}
