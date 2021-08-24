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

    //TODO refactoring awaiting!!!
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args == null || args.length == 0) {
                player.sendMessage("欢迎使用LH民主银行线上存储账户，此账户可存储和提取现金（绿宝石）\n 震撼功能还包括：查询余额，线上消费、贷款业务，以下为操作手册\n 输入 /bank d <金额>，存储指定金额的现金\n输入 /bank w <金额>，提取指定金额现金到当前背包中。/bank t (transfer) <玩家名称> <金额> 转账。\n输入 /bank b 查询当前余额\n将金额替换成all可进行一次性的清算。");
            } else if (args.length == 2) {

                int amount;

                // all as the second argument to work with all currency in inventory or account
                if (!args[1].equals("all")) {
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("不能接受的金额类型");
                        return true;
                    }

                    if (amount <= 0) {
                        player.sendMessage("LH民主银行: 你是想亏空国库吗？");
                        return true;
                    }
                } else {
                    if (args[0].equals("d") || args[0].equals("D")) {
                        amount = this.getCurrencyInventory(player);
                    } else if (args[0].equals("w") || args[0].equals("W")) {
                        amount = this.getBalance(player);
                    } else {
                        player.sendMessage("输入错误，请按照手册输入\n输入 /bank d <金额>，存储指定金额的现金\n输入 /bank w <金额>，提取指定金额现金到当前背包中.\n输入 /bank b 查询当前余额");
                        return true;
                    }
                }

                if (args[0].equals("d") || args[0].equals("D")) {
                    if (!this.checkInventory(player, amount)) {
                        player.sendMessage("LH民主银行: 你没有足够的现金");
                    } else {
                        ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                        player.getInventory().removeItem(itemStack);
                        this.deductBalance(player, -amount);

                        player.sendMessage("LH民主银行: 操作成功，正在打印交易凭条");
                    }
                } else if (args[0].equals("w") || args[0].equals("W")) {
                    if (checkBalance(player, amount)) {
                        ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                        //deal with full inventory
                        HashMap<Integer, ItemStack> nope = player.getInventory().addItem(itemStack);
                        if (!nope.isEmpty()) {
                            for (Map.Entry<Integer, ItemStack> entry : nope.entrySet()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
                            }
                        }
                        this.deductBalance(player, amount);

                        player.sendMessage("LH民主银行: 操作成功，正在打印交易凭条");
                    } else {
                        player.sendMessage("LH民主银行: 存款不足");
                    }
                } else {
                    player.sendMessage("输入错误，请按照手册输入\n输入 /bank d <金额>，存储指定金额的现金\n输入 /bank w <金额>，提取指定金额现金到当前背包中.\n输入 /bank b 查询当前余额");
                }
            } else if (args.length == 1 && (args[0].equals("b") || args[0].equals("B"))) {
                player.sendMessage(String.format("LH民主银行: 账户余额 -%d-", this.getBalance(player)));
            } else {
                player.sendMessage("输入错误，请按照手册输入\n输入 /bank d <金额>，存储指定金额的现金\n输入 /bank w <金额>，提取指定金额现金到当前背包中.\n输入 /bank b 查询当前余额");
            }
        }

        return true;
    }

    // checking if having enough currency
    private boolean checkInventory (Player player, int amount) {
        ItemStack currency = new ItemStack(COMMAND_CURRENCY);

        return player.getInventory().containsAtLeast(currency, amount);
    }

    private boolean checkBalance(Player player, int amount) {
        // create an account for the player
        serverBank.getServerBank().putIfAbsent(player.getName(), 0);

        return serverBank.getServerBank().get(player.getName()) >= amount;
    }

    private void deductBalance(Player player, int amount) {
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

    // getting the amount of currency in inventory
    private int getCurrencyInventory(Player player) {
        ItemStack itemStack = new ItemStack(COMMAND_CURRENCY);

        int currencyInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == COMMAND_CURRENCY) {
                currencyInInventory += item.getAmount();
            }
        }

        return currencyInInventory;
    }

}
