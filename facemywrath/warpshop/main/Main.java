package facemywrath.warpshop.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import facemywrath.warpshop.commands.WarpCommand;
import facemywrath.warpshop.events.VoteListener;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {
	

    private Economy econ;
    private WarpCommand cmd;
	
    public void onDisable() {
    	cmd.getWarpManager().saveVotes();
    }
    
	public void onEnable() {
		cmd = new WarpCommand(this);
		new VoteListener(this, cmd);
		this.getCommand("warp").setExecutor(cmd);
		this.getCommand("goto").setExecutor(cmd);
		this.saveResource("config.yml", false);
		this.saveResource("warps.yml", false);
        if (!setupEconomy()) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
	}
	public Economy getEcon() {
		return econ;
	}
	private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

}
