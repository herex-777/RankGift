package me.herex.rankgift.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;
import org.bukkit.Color;

public final class FireworkUtil {
    private FireworkUtil() {}
    public static void launch(Location loc, int power) {
        if (loc == null) return;
        World w = loc.getWorld();
        if (w == null) return;
        Firework fw = w.spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.setPower(Math.max(0, Math.min(2, power)));
        meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.LIME).flicker(true).build());
        meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.BLUE).flicker(true).build());
        fw.setFireworkMeta(meta);
    }
}
