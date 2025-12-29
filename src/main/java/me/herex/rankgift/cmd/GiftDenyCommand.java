package me.herex.rankgift.cmd;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.Text;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class GiftDenyCommand implements CommandExecutor {
  private final RankGiftPlugin plugin;
  public GiftDenyCommand(RankGiftPlugin plugin){ this.plugin = plugin; }

  @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) { sender.sendMessage(Text.color(plugin.msg("messages.errors.only_players"))); return true; }
    Player p = (Player) sender;
    if (!plugin.isWorldAllowed(p.getWorld().getName())) { p.sendMessage(Text.color(plugin.msg("messages.errors.not_allowed_world"))); return true; }

    if (!plugin.getGiftManager().hasPending(p)) { p.sendMessage(Text.color(plugin.msg("messages.errors.no_pending_gift"))); return true; }
    plugin.getGiftManager().deny(p);
    return true;
  }
}
