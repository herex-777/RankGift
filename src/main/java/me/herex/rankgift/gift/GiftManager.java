package me.herex.rankgift.gift;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.BookUtil;
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

    String rankDisplay = plugin.rankDisplay(rankKey);

    String msgTarget = plugin.msg("messages.success.gift_received")
      .replace("{sender_name}", sender.getName())
      .replace("{sender_prefix}", plugin.getVaultPrefix(sender))
      .replace("{rank_display}", rankDisplay);
    target.sendMessage(Text.color(msgTarget));

    // Broadcast a simple announcement in whitelisted worlds
    String broadcast = plugin.msg("messages.broadcast.gift_request");
    if (broadcast != null && !broadcast.trim().isEmpty()) {
      broadcast = broadcast
        .replace("{sender_name}", sender.getName())
      .replace("{sender_prefix}", plugin.getVaultPrefix(sender))
        .replace("{target_name}", target.getName())
        .replace("{rank_display}", rankDisplay);
      plugin.broadcastToGiftWorlds(Text.color(broadcast));
    }

    // Book GUI accept/deny (only for players in whitelisted worlds)
    boolean bookEnabled = plugin.getConfig().getBoolean("gift.bookgui.enabled", true);
    if (bookEnabled && plugin.isWorldAllowed(target.getWorld().getName())) {
      new BukkitRunnable() {
        @Override public void run() {
          // Still pending?
          GiftRequest cur = pendingByTarget.get(target.getUniqueId());
          if (cur == null || cur.getCreatedAt() != req.getCreatedAt()) return;

          String title = Text.color(plugin.getConfig().getString("gift.bookgui.title", "Gift Request"));
          String author = Text.color(plugin.getConfig().getString("gift.bookgui.author", "RankGift"));

          String header = Text.color("&c[" + sender.getName() + "] &7wants\n&7to gift you ") + rankDisplay + Text.color("&7!\n&7Will you accept?\n\n");
          BookUtil.Page page = BookUtil.newPage()
            .addText(header)
            .addClickable(Text.color("&aYES"), "/giftaccept", Text.color("&aAccept the gift"))
            .addText("\n")
            .addClickable(Text.color("&cNO"), "/giftdeny", Text.color("&cDeny the gift"));
          List<BookUtil.Page> pages = new ArrayList<BookUtil.Page>();
          pages.add(page);
          BookUtil.openBook(target, title, author, pages);
        }
      }.runTaskLater(plugin, 2L);
    }

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
    String rankDisplay = plugin.rankDisplay(req.getRankKey());

    String senderPrefix = sender != null ? plugin.getVaultPrefix(sender) : "";
    String targetPrefix = plugin.getVaultPrefix(target);

    if (sender != null) {
      String msg = plugin.msg("messages.gift_response.denied_sender");
      if (msg == null || msg.trim().isEmpty()) msg = plugin.msg("messages.success.gift_denied");
      sender.sendMessage(Text.color(msg
        .replace("{target_name}", target.getName())
        .replace("{target_prefix}", targetPrefix)
        .replace("{rank_display}", rankDisplay)
      ));
    }

    String msgT = plugin.msg("messages.gift_response.denied_target");
    if (msgT == null || msgT.trim().isEmpty()) msgT = plugin.msg("messages.success.gift_denied");
    target.sendMessage(Text.color(msgT
      .replace("{sender_name}", sender != null ? sender.getName() : "CONSOLE")
      .replace("{sender_prefix}", senderPrefix)
      .replace("{rank_display}", rankDisplay)
    ));
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
    String rankDisplay = plugin.rankDisplay(req.getRankKey());

    // Execute rank commands
    for (String cmd : def.getCommands()) {
      String c = cmd
        .replace("%sender%", sender != null ? sender.getName() : "CONSOLE")
        .replace("%target%", target.getName())
        .replace("%duration%", req.getDuration() == null ? "" : req.getDuration());
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    // Update stats
    long newTotal = 0L;
    if (sender != null) {
      long count = plugin.getDataStore().getRanksGifted(sender.getUniqueId());
      newTotal = count + 1;
      plugin.getDataStore().setRanksGifted(sender.getUniqueId(), newTotal);
      plugin.incrementSessionGifted(sender.getUniqueId());
    }

    String senderPrefix = sender != null ? plugin.getVaultPrefix(sender) : "";
    String targetPrefix = plugin.getVaultPrefix(target);

    // Broadcast accept announcement to players inside whitelisted worlds
    String line1 = plugin.msg("messages.broadcast.gift_accepted");
    if (line1 != null && !line1.trim().isEmpty()) {
      String out1 = line1
        .replace("{sender_name}", sender != null ? sender.getName() : "CONSOLE")
        .replace("{target_name}", target.getName())
        .replace("{sender_prefix}", senderPrefix)
        .replace("{target_prefix}", targetPrefix)
        .replace("{rank_display}", rankDisplay)
        .replace("{ranks_gifted_total}", String.valueOf(newTotal));
      for (Player pl : Bukkit.getOnlinePlayers()) {
        if (plugin.isWorldAllowed(pl.getWorld().getName())) {
          pl.sendMessage(Text.color(out1));
        }
      }
    }

    String line2 = plugin.msg("messages.broadcast.gift_accepted_stats");
    if (line2 != null && !line2.trim().isEmpty()) {
      String out2 = line2
        .replace("{sender_name}", sender != null ? sender.getName() : "CONSOLE")
        .replace("{target_name}", target.getName())
        .replace("{sender_prefix}", senderPrefix)
        .replace("{target_prefix}", targetPrefix)
        .replace("{rank_display}", rankDisplay)
        .replace("{ranks_gifted_total}", String.valueOf(newTotal));
      for (Player pl : Bukkit.getOnlinePlayers()) {
        if (plugin.isWorldAllowed(pl.getWorld().getName())) {
          pl.sendMessage(Text.color(out2));
        }
      }
    }

    // Receiver "final" message (like the screenshot) when accepted
    String recvFinal = plugin.msg("messages.success.gift_received_final");
    if (recvFinal != null && !recvFinal.trim().isEmpty()) {
      target.sendMessage(Text.color(recvFinal
        .replace("{sender_name}", sender != null ? sender.getName() : "CONSOLE")
        .replace("{sender_prefix}", senderPrefix)
        .replace("{rank_display}", rankDisplay)
        .replace("{ranks_gifted_total}", String.valueOf(newTotal))
      ));
    }

    // Response messages
    String msgTarget = plugin.msg("messages.success.gift_accepted");
    if (msgTarget != null) {
      target.sendMessage(Text.color(msgTarget
        .replace("{sender_name}", sender != null ? sender.getName() : "CONSOLE")
        .replace("{sender_prefix}", senderPrefix)
        .replace("{rank_display}", rankDisplay)
      ));
    }

    if (sender != null) {
      String msgSender = plugin.msg("messages.success.gift_accepted_sender");
      if (msgSender == null || msgSender.trim().isEmpty()) msgSender = plugin.msg("messages.success.gift_accepted");
      if (msgSender != null) {
        sender.sendMessage(Text.color(msgSender
          .replace("{target_name}", target.getName())
          .replace("{target_prefix}", targetPrefix)
          .replace("{rank_display}", rankDisplay)
          .replace("{ranks_gifted_total}", String.valueOf(newTotal))
        ));
      }
    }

    // fireworks (world restricted)
    if (plugin.getConfig().getBoolean("gift.fireworks.enabled", true)) {
      List<String> worlds = plugin.getConfig().getStringList("gift.fireworks.worlds");
      if (worlds == null || worlds.isEmpty() || worlds.contains(target.getWorld().getName())) {
        int power = plugin.getConfig().getInt("gift.fireworks.power", 1);
        FireworkUtil.launch(target.getLocation(), power);
      }
    }
  }

}
