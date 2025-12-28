package me.herex.rankgift.gift;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.FireworkUtil;
import me.herex.rankgift.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GiftManager {
  private final RankGiftPlugin plugin;
  private final Map<UUID, GiftRequest> pendingByTarget = new ConcurrentHashMap<UUID, GiftRequest>();

  public GiftManager(RankGiftPlugin plugin) {
    this.plugin = plugin;
  }

  public boolean hasPending(Player target) {
    return target != null && pendingByTarget.containsKey(target.getUniqueId());
  }

  public GiftRequest getPending(Player target) {
    if (target == null) return null;
    return pendingByTarget.get(target.getUniqueId());
  }

  public RankDef getRank(String key) {
    if (key == null) return null;
    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ranks." + key.toLowerCase());
    if (sec == null) return null;
    String disp = sec.getString("display-name", key);
    List<String> cmds = sec.getStringList("commands");
    return new RankDef(key.toLowerCase(), Text.color(disp), cmds);
  }

  public void send(Player sender, Player target, String rankKey, String duration, long cost) {
    GiftRequest req = new GiftRequest(sender.getUniqueId(), target.getUniqueId(), rankKey, duration, cost, System.currentTimeMillis());
    pendingByTarget.put(target.getUniqueId(), req);

    String msgTarget = plugin.msg("messages.success.gift_received")
      .replace("{sender_name}", sender.getName())
      .replace("{rank_display}", plugin.rankDisplay(rankKey));
    target.sendMessage(Text.color(msgTarget));

    int expire = plugin.getConfig().getInt("gift.expiration_time", 60);
    if (expire > 0) {
      new BukkitRunnable() {
        @Override public void run() {
          GiftRequest cur = pendingByTarget.get(target.getUniqueId());
          if (cur != null && cur.getCreatedAt() == req.getCreatedAt()) {
            pendingByTarget.remove(target.getUniqueId());
            target.sendMessage(Text.color(plugin.msg("messages.errors.gift_expired")));
          }
        }
      }.runTaskLater(plugin, expire * 20L);
    }
  }

  public void deny(Player target) {
    if (target == null) return;
    GiftRequest req = pendingByTarget.remove(target.getUniqueId());
    if (req == null) return;

    Player sender = Bukkit.getPlayer(req.getSender());
    if (sender != null) sender.sendMessage(Text.color(plugin.msg("messages.success.gift_denied")));
    target.sendMessage(Text.color(plugin.msg("messages.success.gift_denied")));
  }

  public void accept(Player target) {
    if (target == null) return;
    GiftRequest req = pendingByTarget.remove(target.getUniqueId());
    if (req == null) return;

    RankDef def = getRank(req.getRankKey());
    if (def == null) {
      target.sendMessage(Text.color("&cRank definition missing for: " + req.getRankKey()));
      return;
    }

    Player sender = Bukkit.getPlayer(req.getSender());
    for (String cmd : def.getCommands()) {
      String c = cmd
        .replace("%sender%", sender != null ? sender.getName() : "CONSOLE")
        .replace("%target%", target.getName())
        .replace("%duration%", req.getDuration() == null ? "" : req.getDuration());
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    // stats
    if (sender != null) {
      long count = plugin.getDataStore().getRanksGifted(sender.getUniqueId());
      plugin.getDataStore().setRanksGifted(sender.getUniqueId(), count + 1);
    }

    // fireworks (world restricted)
    if (plugin.getConfig().getBoolean("gift.fireworks.enabled", true)) {
      List<String> worlds = plugin.getConfig().getStringList("gift.fireworks.worlds");
      if (worlds == null || worlds.isEmpty() || worlds.contains(target.getWorld().getName())) {
        int power = plugin.getConfig().getInt("gift.fireworks.power", 1);
        FireworkUtil.launch(target.getLocation(), power);
      }
    }

    target.sendMessage(Text.color(plugin.msg("messages.success.gift_accepted")));
    if (sender != null) sender.sendMessage(Text.color(plugin.msg("messages.success.gift_accepted")));
  }
}
