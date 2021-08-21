package cn.leisik.teleportation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CommandLtp implements CommandExecutor {
    private final static int COMMAND_EXP= 5;
    private final static Material COMMAND_CURRENCY = Material.EMERALD;
    private final BankData serverBank;

    public CommandLtp (BankData serverBank) {
        this.serverBank = serverBank;
    }
    /***
     * This method is called, when somebody uses our command.
     * One should use the command as follows
     * @param sender the player who type the command
     * @param command
     * @param label
     * @param args
     * @return Boolean
     * @note we only support teleporting ourselves!!!
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            //player issued help command
            if (args == null || args.length == 0) {
                //TODO tell the player how to use
                this.greetingToPlayer(player, 1);
                return true;
            }

            Location targetLocation = null;

            if (this.checkCommandExp(player)) {
                //coords
                if (args.length == 3) {
                    //user input handler
                    try {
                        targetLocation = player.getLocation();
                        double x = Double.parseDouble(args[0]);
                        double y = Double.parseDouble(args[1]);
                        double z = Double.parseDouble(args[2]);
                        targetLocation.setX(x);
                        targetLocation.setY(y);
                        targetLocation.setZ(z);
                    } catch (NumberFormatException e) {
                        player.sendMessage("请检查你输入的坐标格式是否正确");
                        return true;
                    }

                    player.teleport(targetLocation);
                    this.applyCommandExp(player);

                    this.greetingToPlayer(player, 4);
                } else if (args.length == 1) {
                    //find player first
                    String targetPlayerName = args[0];
                    Player targetPlayer = Bukkit.getServer().getPlayer(targetPlayerName);
                    if (targetPlayer != null) {
                        if (targetPlayer.getName().equals(player.getName())) {
                            this.greetingToPlayer(player, 5);
                        } else {
                            player.teleport(targetPlayer);
                            this.applyCommandExp(player);

                            this.greetingToPlayer(player, 4);
                        }
                    } else {
                        this.greetingToPlayer(player, 2);
                    }
                }
            } else {
                this.greetingToPlayer(player, 3);
            }
        }

        //if the player use our command correctly, return a True
        return true;
    }

    private boolean checkCommandExp (Player player) {
        int balance = this.getBalance(player);
        if (balance >= COMMAND_EXP) {
            return true;
        }

        Inventory inventory = player.getInventory();
        ItemStack currency = new ItemStack(COMMAND_CURRENCY);

        return inventory.containsAtLeast(currency, COMMAND_EXP - balance);
    }

    //TODO exception handler
    //make sure to call checkCommandExp first
    private void applyCommandExp (Player player) {
        int balance = this.getBalance(player);

        if (balance < COMMAND_EXP) {
            int remainedExp = COMMAND_EXP - balance;
            ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, remainedExp);
            player.getInventory().removeItem(itemStack);
            this.reduceBalance(player, balance);
        } else {
            this.reduceBalance(player, COMMAND_EXP);
        }

    }

    private void reduceBalance (Player player, int amount) {
        // create an account for the player
        serverBank.getServerBank().putIfAbsent(player.getName(), 0);

        int ori = serverBank.getServerBank().get(player.getName());
        serverBank.getServerBank().put(player.getName(), ori - amount);
    }

    private int getBalance (Player player) {
        // create an account for the player
        serverBank.getServerBank().putIfAbsent(player.getName(), 0);

        return this.serverBank.getServerBank().get(player.getName());
    }

    /**
     * 1: help string
     * 2: targetPlayer not found
     * 3: out of currency
     * 4: successful teleportation
     * 5: tp to same player
     */
    private void greetingToPlayer (Player player, int greetingCode) {
        if (greetingCode == 1) {
            player.sendMessage("输入 /ltp 玩家名 可以tp至玩家。\n输入 /ltp x y z 可以tp到指定位置。\n输入 /ltp 显示该条信息。每次传送成功消耗5个绿宝石");
        } else if (greetingCode == 2) {
            player.sendMessage("找不到你要传送的玩家。");
        } else if (greetingCode == 3) {
            player.sendMessage(String.format("你的银行账户与背包余额总和不足 %d :)", COMMAND_EXP));
        } else if (greetingCode == 4) {
            player.sendMessage("传送完成。LH 公共交通期待下次与你再见。");
        } else if (greetingCode == 5) {
            player.sendMessage("不能原地传送。");
        }
    }
}
