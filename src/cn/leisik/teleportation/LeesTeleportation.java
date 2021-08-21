package cn.leisik.teleportation;

import org.bukkit.plugin.java.JavaPlugin;

public class LeesTeleportation extends JavaPlugin {
    private BankData serverBank;
    //enabling
    @Override
    public void onEnable() {
        serverBank = BankData.loadData();
        this.getCommand("ltp").setExecutor(new CommandLtp(serverBank));
        this.getCommand("bank").setExecutor(new CommandBank(serverBank));
    }

    //disabling
    @Override
    public void onDisable() {
        this.serverBank.saveData();
    }
}
