package me.herex.rankgift.cmd;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiftCommand implements CommandExecutor {
  private final RankGiftPlugin plugin;
  public GiftCommand(RankGiftPlugin plugin) { this.plugin = plugin; }

  @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) { sender.sendMessage(Text.color(plugin.msg("messages.errors.only_players"))); return true; }
    Player p = (Player) sender;
    if (args.length < 1) { p.sendMessage(Text.color("&cUsage: /gift <player>")); return true; }
    if (!plugin.isWorldAllowed(p.getWorld().getName())) { p.sendMessage(Text.color(plugin.msg("messages.errors.not_allowed_world"))); return true; }

    Player target = Bukkit.getPlayer(args[0]);
    if (target == null) { p.sendMessage(Text.color(plugin.msg("messages.errors.no_target"))); return true; }
    if (target.getUniqueId().equals(p.getUniqueId())) { p.sendMessage(Text.color(plugin.msg("messages.errors.cannot_gift_self"))); return true; }

    plugin.getGuiManager().setTarget(p, target);
    plugin.getGuiManager().open(p, "main");
    return true;
  }
}
