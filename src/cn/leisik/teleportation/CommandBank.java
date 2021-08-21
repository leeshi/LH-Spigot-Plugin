package cn.leisik.teleportation;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CommandBank implements CommandExecutor {
    private final static Material COMMAND_CURRENCY = Material.EMERALD;
    private final BankData serverBank;

    public CommandBank(BankData serverBank) {
        this.serverBank = serverBank;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args == null || args.length == 0) {
                player.sendMessage("Type /bank d (deposit) x to deposit emeralds into your bank.\nType /bank w (withdraw) x to withdraw.\nType /bank b (balance) to check your balance.");
            } else if (args.length == 2) {

                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("数字输入错误");
                    return true;
                }

                if (amount <= 0) {
                    player.sendMessage("Central Bank of LH: Don't ever try to steal money from CBLH!");
                    return true;
                }

                if (args[0].equals("d")) {
                    if (!this.checkInventory(player, amount)) {
                        player.sendMessage("Central Bank of LH: You don't have enough emeralds.");
                    } else {
                        ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                        player.getInventory().removeItem(itemStack);
                        this.reduceBalance(player, -amount);

                        player.sendMessage("Central Bank of LH: Operation successful.");
                    }
                } else if (args[0].equals("w")) {
                    if (checkBalance(player, amount)) {
                        ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                        //deal with full inventory
                        HashMap<Integer, ItemStack> nope = player.getInventory().addItem(itemStack);
                        if (!nope.isEmpty()) {
                            for (Map.Entry<Integer, ItemStack> entry : nope.entrySet()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
                            }
                        }
                        this.reduceBalance(player, amount);

                        player.sendMessage("Central Bank of LH: Operation successful.");
                    } else {
                        player.sendMessage("Central Bank of LH: Insufficient  balance.");
                    }
                }
            } else if (args.length == 1 && args[0].equals("b")) {
                player.sendMessage(String.format("Central Bank of LH: Account balance -> %d", this.getBalance(player)));
            } else {
                player.sendMessage("Type /bank deposit x to deposit emeralds into your bank.\nType /bank withdraw x to withdraw.\nType /bank account to check your balance.");
            }
        }

        return true;
    }

    private boolean checkInventory (Player player, int amount) {
        ItemStack currency = new ItemStack(COMMAND_CURRENCY);

        return player.getInventory().containsAtLeast(currency, amount);
    }

    private boolean checkBalance(Player player, int amount) {
        // create an account for the player
        serverBank.getServerBank().putIfAbsent(player.getName(), 0);

        return serverBank.getServerBank().get(player.getName()) >= amount;
    }

    private void reduceBalance(Player player, int amount) {
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

}
