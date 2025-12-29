package me.herex.rankgift.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.herex.rankgift.RankGiftPlugin;
import org.bukkit.OfflinePlayer;

public class RankGiftExpansion extends PlaceholderExpansion {
  private final RankGiftPlugin plugin;
  public RankGiftExpansion(RankGiftPlugin plugin) { this.plugin = plugin; }

  @Override public String getIdentifier() { return "rankgift"; }
  @Override public String getAuthor() { return "RankGift"; }
  @Override public String getVersion() { return plugin.getDescription().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override
  public String onRequest(OfflinePlayer player, String params) {
    if (player == null) return "";
    if (params.equalsIgnoreCase("ranks_gifted_total")) {
      return String.valueOf(plugin.getDataStore().getRanksGifted(player.getUniqueId()));
    }
    if (params.equalsIgnoreCase("ranks_gifted_now")) {
      return String.valueOf(plugin.getSessionGifted(player.getUniqueId()));
    }
    return null;
  }
}
