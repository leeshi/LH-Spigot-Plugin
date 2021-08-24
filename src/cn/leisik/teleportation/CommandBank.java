package cn.leisik.teleportation;

import org.bukkit.Bukkit;
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
    private final BankData bankData;
    private final static String HELP_MESSAGE = "欢迎使用LH民主银行线上存储账户，此账户可存储和提取现金（绿宝石）\n 震撼功能还包括：" +
            "查询余额，线上消费、贷款业务，以下为操作手册\n 输入 /bank d <金额>，存储指定金额的现金\n" +
            "输入 /bank w <金额>，提取指定金额现金到当前背包中。\n输入/bank t (transfer) <玩家名称> <金额> 转账。\n" +
            "输入 /bank b 查询当前余额\n将金额替换成all可进行一次性的清算。";
    private final static String SUCCESSFUL_MESSAGE = "LH民主银行: 操作成功，正在打印交易凭条";
    private final static String INSUFFICIENT_BALANCE_MESSAGE = "LH民主银行：你的余额不足，请重试。";
    private final static String INSUFFICIENT_INVENTORY_MESSAGE = "LH民主银行：你的背包内货币不足，请重试。";
    private final static String UNSUPPORTED_AMOUNT_MESSAGE = "LH民主银行：你输入的金额不符合规范，请重试。";
    private final static String ACCOUNT_NOT_FOUND_MESSAGE = "LH民主银行：找不到目标账户，请重试。";
    private final static String TRANSACTION_RECEIVED_FORMAT = "LH民主银行：你刚刚收到一笔来自<%s>的%d LHD的转账，请查收。";
    private final static String BALANCE_MESSAGE_FORMAT = "LH民主银行：您目前的余额为 %d LHD。";

    public CommandBank(BankData bankData) {
        this.bankData = bankData;
    }

    //TODO autosave bankdata
    //TODO use player's name to deal with their accounts not Player object reference
    //TODO split onCommand into different functions
    //TODO transaction record
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof  Player) {
            Player player = (Player) sender;

            if (args == null || args.length == 0) {
                player.sendMessage(HELP_MESSAGE);
            } else if (args[0].equals("w") || args[0].equals("withdraw")) {
                // using subcommand incorrectly
                if (args.length != 2) {
                    player.sendMessage(HELP_MESSAGE);
                    return true;
                }
                int amount;

                // getting the amount player want to manipulate
                if (args[1].equals("all")) {
                    amount = this.getBalance(player.getName());
                } else {
                    try {
                        amount = Integer.parseInt(args[1]);
                        // dealing with sub 0 input
                        if (amount <= 0) {
                            player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                            return  true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                        return true;
                    }
                }

                if (!isSufficientBalance(player, amount)) {
                    player.sendMessage(INSUFFICIENT_BALANCE_MESSAGE);
                    return true;
                }

                ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                //dealing with full inventory
                HashMap<Integer, ItemStack> nope = player.getInventory().addItem(itemStack);
                if (!nope.isEmpty()) {
                    for (Map.Entry<Integer, ItemStack> entry : nope.entrySet()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
                    }
                }
                this.deductBalance(player, amount);

                player.sendMessage(SUCCESSFUL_MESSAGE);

            } else if (args[0].equals("d") || args[0].equals("deposit")) {
                // using subcommand incorrectly
                if (args.length != 2) {
                    player.sendMessage(HELP_MESSAGE);
                    return true;
                }

                int amount;
                if (args[1].equals("all")) {
                    amount = this.getCurrencyInventory(player);
                } else {
                    try {
                        amount = Integer.parseInt(args[1]);
                        if (amount <= 0) {
                            player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                            return  true;
                        }
                    } catch (NumberFormatException e) {
                        // got invalid data
                        player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                        return true;
                    }
                }

                if (!this.isSufficientInventory(player, amount)) {
                    player.sendMessage(INSUFFICIENT_INVENTORY_MESSAGE);
                    return true;
                }
                ItemStack itemStack = new ItemStack(COMMAND_CURRENCY, amount);
                player.getInventory().removeItem(itemStack);
                this.deductBalance(player, -amount);

                player.sendMessage(SUCCESSFUL_MESSAGE);

            } else if (args[0].equals("t") || args[0].equals("transfer")) {
                if (args.length != 3) {
                    player.sendMessage(HELP_MESSAGE);
                    return true;
                }

                // target account doesn't exit
                if (!this.exitsAccount(args[1])) {
                    player.sendMessage(ACCOUNT_NOT_FOUND_MESSAGE);
                    return true;
                }

                int amount;
                if (args[2].equals("all")) {
                    amount = this.getCurrencyInventory(player);
                } else {
                    try {
                        amount = Integer.parseInt(args[2]);
                        if (amount <= 0) {
                            player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                            return  true;
                        }
                    } catch (NumberFormatException e) {
                        // got invalid data
                        player.sendMessage(UNSUPPORTED_AMOUNT_MESSAGE);
                        return true;
                    }
                }

                if (!this.isSufficientBalance(player, amount)) {
                    player.sendMessage(INSUFFICIENT_BALANCE_MESSAGE);
                    return true;
                }

               // transaction begins
                this.transfer(player.getName(), args[1], amount);
                player.sendMessage(SUCCESSFUL_MESSAGE);
                // send notification to target player
                Player targetPlayer = Bukkit.getServer().getPlayer(args[1]);

                if (targetPlayer != null) {
                    targetPlayer.sendMessage(String.format(TRANSACTION_RECEIVED_FORMAT, player.getName(), amount));
                }

            } else if (args[0].equals("b") || args[0].equals("balance")) {
                player.sendMessage(String.format(BALANCE_MESSAGE_FORMAT, this.getBalance(player.getName())));
            }
        }

        return true;
    }

    // checking if having enough currency
    private boolean isSufficientInventory(Player player, int amount) {
        ItemStack currency = new ItemStack(COMMAND_CURRENCY);

        return player.getInventory().containsAtLeast(currency, amount);
    }

    private boolean isSufficientBalance(Player player, int amount) {
        // create an account for the player
        bankData.getServerBank().putIfAbsent(player.getName(), 0);

        return bankData.getServerBank().get(player.getName()) >= amount;
    }

    private void deductBalance(Player player, int amount) {
        // create an account for the player
        bankData.getServerBank().putIfAbsent(player.getName(), 0);

        int ori = bankData.getServerBank().get(player.getName());
        bankData.getServerBank().put(player.getName(), ori - amount);
    }

    private int getBalance (String playerName) {
        // create an account for the player
        bankData.getServerBank().putIfAbsent(playerName, 0);

        return this.bankData.getServerBank().get(playerName);
    }

    // getting the amount of currency in inventory
    private int getCurrencyInventory(Player player) {
        int currencyInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == COMMAND_CURRENCY) {
                currencyInInventory += item.getAmount();
            }
        }

        return currencyInInventory;
    }

    private void transfer(String accountFrom, String accountTo, int amount) {
        int fromBalance = this.getBalance(accountFrom);
        int toBalance = this.getBalance(accountTo);

        this.bankData.getServerBank().put(accountFrom, fromBalance - amount);
        this.bankData.getServerBank().put(accountTo, toBalance + amount);

    }

    private boolean exitsAccount(String account) {
        return this.bankData.getServerBank().containsKey(account);
    }

}
