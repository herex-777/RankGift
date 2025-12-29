package me.herex.rankgift.economy;

import me.herex.rankgift.RankGiftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CurrencyManager {
  private final RankGiftPlugin plugin;
  private final EconomyType type;
  private final String placeholder;
  private final String withdrawCmd;
  private final String depositCmd;

  public CurrencyManager(RankGiftPlugin plugin) {
    this.plugin = plugin;
    this.type = EconomyType.fromString(plugin.getConfig().getString("economy.type", "PLACEHOLDER+COMMAND"));
    this.placeholder = plugin.getConfig().getString("economy.balance-placeholder", "%farepixel_gold%");
    this.withdrawCmd = plugin.getConfig().getString("economy.withdraw-command", "");
    this.depositCmd = plugin.getConfig().getString("economy.deposit-command", "");
  }

  public String currencyName() {
    return plugin.getConfig().getString("economy.currency-name", "Gold");
  }

  public long getBalance(Player p) {
    if (p == null) return 0L;
    if (type == EconomyType.INTERNAL) return plugin.getDataStore().getInternalGold(p.getUniqueId());
    if (type == EconomyType.COMMAND) return 0L; // can't read; GUI will show 0 unless placeholder also enabled
    // placeholder read
    String v = plugin.applyPlaceholders(p, placeholder);
    return parseLongSafe(v);
  }

  public boolean withdraw(Player p, long amount) {
    if (p == null) return false;
    if (amount <= 0) return true;

    if (type == EconomyType.INTERNAL) {
      long bal = plugin.getDataStore().getInternalGold(p.getUniqueId());
      if (bal < amount) return false;
      plugin.getDataStore().setInternalGold(p.getUniqueId(), bal - amount);
      return true;
    }

    if (type == EconomyType.PLACEHOLDER || type == EconomyType.PLACEHOLDER_COMMAND) {
      long bal = getBalance(p);
      if (bal < amount) return false;
    }

    if ((type == EconomyType.COMMAND || type == EconomyType.PLACEHOLDER_COMMAND) && withdrawCmd != null && withdrawCmd.trim().length() > 0) {
      String cmd = withdrawCmd.replace("%player%", p.getName()).replace("%amount%", String.valueOf(amount));
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
      return true;
    }

    // PLACEHOLDER only: read-only, can't withdraw
    return false;
  }

  public void deposit(Player p, long amount) {
    if (p == null) return;
    if (amount <= 0) return;

    if (type == EconomyType.INTERNAL) {
      long bal = plugin.getDataStore().getInternalGold(p.getUniqueId());
      plugin.getDataStore().setInternalGold(p.getUniqueId(), bal + amount);
      return;
    }

    if ((type == EconomyType.COMMAND || type == EconomyType.PLACEHOLDER_COMMAND) && depositCmd != null && depositCmd.trim().length() > 0) {
      String cmd = depositCmd.replace("%player%", p.getName()).replace("%amount%", String.valueOf(amount));
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
  }

  private static long parseLongSafe(String s) {
    if (s == null) return 0L;
      String cleaned = s.replaceAll("[^0-9\\-\\.]", "");
      if (cleaned.matches("-?\\d+(\\.\\d+)?")) {
          try { return (long) Double.parseDouble(cleaned); } catch (Exception ignored) {}
    }
    return 0L;
  }
}
