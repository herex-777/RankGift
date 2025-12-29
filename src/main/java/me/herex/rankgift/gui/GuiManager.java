package me.herex.rankgift.gui;

import me.herex.rankgift.RankGiftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager {

    private final RankGiftPlugin plugin;
    private final Map<String, GuiDefinition> guis = new HashMap<String, GuiDefinition>();

    private final Map<UUID, UUID> targetBySender = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, PendingSelection> pendingBySender = new ConcurrentHashMap<UUID, PendingSelection>();

    public GuiManager(RankGiftPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        guis.clear();
        guis.putAll(GuiConfigLoader.load(plugin));
    }

    public boolean open(Player viewer, String guiId) {
        if (viewer == null || guiId == null) return false;
        GuiDefinition def = guis.get(guiId.toLowerCase(Locale.ENGLISH));
        if (def == null) return false;
        viewer.openInventory(GuiBuilder.build(plugin, this, viewer, def));
        return true;
    }

    public void setTarget(Player sender, Player target) {
        if (sender == null) return;
        if (target == null) targetBySender.remove(sender.getUniqueId());
        else targetBySender.put(sender.getUniqueId(), target.getUniqueId());
    }

    public Player getTargetPlayer(Player sender) {
        UUID u = sender == null ? null : targetBySender.get(sender.getUniqueId());
        return u == null ? null : Bukkit.getPlayer(u);
    }

    public String getTargetName(Player sender) {
        Player t = getTargetPlayer(sender);
        return t == null ? "" : t.getName();
    }

    public void setPending(Player sender, PendingSelection sel) {
        if (sender == null) return;
        if (sel == null) pendingBySender.remove(sender.getUniqueId());
        else pendingBySender.put(sender.getUniqueId(), sel);
    }

    public PendingSelection getPending(Player sender) {
        return sender == null ? null : pendingBySender.get(sender.getUniqueId());
    }

    public void clearPending(Player sender) {
        if (sender != null) pendingBySender.remove(sender.getUniqueId());
    }

    public RankGiftPlugin plugin() { return plugin; }

    public static class PendingSelection {
        public final String rankKey;
        public final long cost;
        public final String duration;
        public PendingSelection(String rankKey, long cost, String duration) {
            this.rankKey = rankKey;
            this.cost = cost;
            this.duration = duration;
        }
    }
}