package me.herex.rankgift;

import me.herex.rankgift.cmd.*;
import me.herex.rankgift.economy.CurrencyManager;
import me.herex.rankgift.gift.GiftManager;
import me.herex.rankgift.gui.GuiListener;
import me.herex.rankgift.gui.GuiManager;
import me.herex.rankgift.storage.*;
import me.herex.rankgift.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RankGiftPlugin extends JavaPlugin {

  private DataStore dataStore;
  private CurrencyManager currencyManager;
  private GiftManager giftManager;
  private GuiManager guiManager;

  @Override public void onEnable() {
    saveDefaultConfig();
    initStore();
    this.currencyManager = new CurrencyManager(this);
    this.giftManager = new GiftManager(this);
    this.guiManager = new GuiManager(this);

    Bukkit.getPluginManager().registerEvents(new GuiListener(this, guiManager), this);

    getCommand("gift").setExecutor(new GiftCommand(this));
    getCommand("giftaccept").setExecutor(new GiftAcceptCommand(this));
    getCommand("giftdeny").setExecutor(new GiftDenyCommand(this));
    getCommand("rankgift").setExecutor(new RankGiftCommand(this));

    getLogger().info("RankGift enabled.");
  }

  @Override public void onDisable() {
    if (dataStore != null) dataStore.close();
  }

  public void reloadAll() {
    reloadConfig();
    initStore();
    this.currencyManager = new CurrencyManager(this);
    this.guiManager.reload();
  }

  private void initStore() {
    if (dataStore != null) { try { dataStore.close(); } catch (Throwable ignored) {} }
    StorageType st = StorageType.fromString(getConfig().getString("database.type", "file"));
    if (st == StorageType.MYSQL) dataStore = new SqlDataStore(this, StorageType.MYSQL);
    else if (st == StorageType.SQLITE) dataStore = new SqlDataStore(this, StorageType.SQLITE);
    else dataStore = new FileDataStore(this);
  }

  public DataStore getDataStore() { return dataStore; }
  public CurrencyManager getCurrencyManager() { return currencyManager; }
  public GiftManager getGiftManager() { return giftManager; }
  public GuiManager getGuiManager() { return guiManager; }

  public boolean isWorldAllowed(String world) {
    List<String> wl = getConfig().getStringList("gift.whitelisted_worlds");
    if (wl == null || wl.isEmpty()) return true;
    return wl.contains(world);
  }

  public String rankDisplay(String rankKey) {
    String disp = getConfig().getString("ranks." + rankKey.toLowerCase() + ".display-name", rankKey);
    return Text.color(disp);
  }

  public String msg(String path) {
    return Text.color(getConfig().getString(path, ""));
  }

  public String applyTokens(Player viewer, String text, me.herex.rankgift.gui.GuiItemDef item, me.herex.rankgift.gui.GuiManager.PendingSelection pending) {
    if (text == null) return "";
    String t = text;

    String targetName = guiManager != null ? guiManager.getTargetName(viewer) : "";
    long bal = currencyManager != null ? currencyManager.getBalance(viewer) : 0L;
    long ranksGifted = dataStore != null ? dataStore.getRanksGifted(viewer.getUniqueId()) : 0L;

    long cost = 0L;
    String rankDisplay = "";
    if (pending != null) {
      cost = pending.cost;
      rankDisplay = rankDisplay(pending.rankKey);
    } else if (item != null) {
      cost = item.cost;
      if (item.rank != null) rankDisplay = rankDisplay(item.rank);
    }

    t = t.replace("{target_name}", targetName)
         .replace("{sender_name}", viewer == null ? "" : viewer.getName())
         .replace("{currency}", currencyManager != null ? currencyManager.currencyName() : "Gold")
         .replace("{balance}", String.valueOf(bal))
         .replace("{ranks_gifted}", String.valueOf(ranksGifted))
         .replace("{cost}", String.valueOf(cost))
         .replace("{rank_display}", rankDisplay);

    return applyPlaceholders(viewer, t);
  }

  public String applyPlaceholders(Player viewer, String text) {
    try {
      if (text == null) return "";
      if (viewer == null) return text;
      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
      Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      return (String) papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, viewer, text);
    } catch (Throwable t) {
      return text;
    }
  }
}
