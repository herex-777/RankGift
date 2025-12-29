package me.herex.rankgift.cmd;

import me.herex.rankgift.RankGiftPlugin;
import me.herex.rankgift.util.Text;
import org.bukkit.command.*;

public class RankGiftCommand implements CommandExecutor {
  private final RankGiftPlugin plugin;
  public RankGiftCommand(RankGiftPlugin plugin){ this.plugin = plugin; }

  @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("rankgift.admin")) { sender.sendMessage(Text.color("&cNo permission.")); return true; }
      plugin.reloadAll();
      sender.sendMessage(Text.color(plugin.msg("messages.success.reload")));
      return true;
    }
    sender.sendMessage(Text.color("&cUsage: /rankgift reload"));
    return true;
  }
}
