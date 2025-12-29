package me.herex.rankgift.gui;

import me.herex.rankgift.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {
  private final me.herex.rankgift.RankGiftPlugin plugin;
  private final GuiManager gm;

  public GuiListener(me.herex.rankgift.RankGiftPlugin plugin, GuiManager gm) {
    this.plugin = plugin;
    this.gm = gm;
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    InventoryHolder holder = e.getInventory().getHolder();
    if (!(holder instanceof GuiHolder)) return;
    e.setCancelled(true);

    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();

    GuiHolder gh = (GuiHolder) holder;
    GuiItemDef item = gh.def.itemsBySlot.get(e.getRawSlot());
    if (item == null) return;

    switch (item.action) {
      case CLOSE:
        p.closeInventory();
        return;

      case OPEN_GUI:
        if (item.opens != null) gm.open(p, item.opens);
        return;

      case RUN_COMMAND:
        if (item.commands != null) {
          for (String cmd : item.commands) {
            String c = cmd.replace("%player%", p.getName())
                          .replace("{target_name}", gm.getTargetName(p));
            if ("PLAYER".equalsIgnoreCase(item.as)) {
              p.performCommand(c.startsWith("/") ? c.substring(1) : c);
            } else {
              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.startsWith("/") ? c.substring(1) : c);
            }
          }
        }
        return;

      case SELECT_RANK:
        if (item.rank == null) return;
        long cost = item.cost;
        // store selection
        gm.setPending(p, new GuiManager.PendingSelection(item.rank, cost, item.duration));
        gm.open(p, "confirm");
        return;

      case CONFIRM_DENY:
        gm.clearPending(p);
        gm.open(p, "rank_select");
        return;

      case CONFIRM_ACCEPT:
        GuiManager.PendingSelection sel = gm.getPending(p);
        if (sel == null) return;
        Player target = gm.getTargetPlayer(p);
        if (target == null) {
          p.sendMessage(Text.color(plugin.msg("messages.errors.no_target")));
          p.closeInventory();
          gm.clearPending(p);
          return;
        }

        // world check handled in commands too; still safe here
        if (!plugin.isWorldAllowed(p.getWorld().getName())) {
          p.sendMessage(Text.color(plugin.msg("messages.errors.not_allowed_world")));
          return;
        }

        // withdraw
        if (!plugin.getCurrencyManager().withdraw(p, sel.cost)) {
          p.sendMessage(Text.color(plugin.msg("messages.errors.not_enough_currency").replace("{currency}", plugin.getCurrencyManager().currencyName())));
          return;
        }

        plugin.getGiftManager().send(p, target, sel.rankKey, sel.duration, sel.cost);
        p.sendMessage(Text.color(plugin.msg("messages.success.gift_sent").replace("{target_name}", target.getName())));
        gm.clearPending(p);
        p.closeInventory();
        return;

      default:
        return;
    }
  }
}
