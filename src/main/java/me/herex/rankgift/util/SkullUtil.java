package me.herex.rankgift.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Skull utility for Spigot 1.8.x
 * Supports:
 *  - Player name skulls
 *  - Custom base64 textured skulls (from config)
 *  - No compile-time authlib dependency
 */
public final class SkullUtil {

    private SkullUtil() {}

    /**
     * MAIN METHOD (use this everywhere)
     *
     * @param ownerName player name (can be null)
     * @param base64Texture base64 texture (can be null)
     */
    public static ItemStack makeSkull(String ownerName, String base64Texture) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = skull.getItemMeta();
        if (meta == null) return skull;

        // Prefer texture if provided
        if (base64Texture != null && base64Texture.trim().length() > 10) {
            applyTexture(meta, base64Texture.trim());
        }
        // Fallback to player owner
        else if (ownerName != null && !ownerName.trim().isEmpty()) {
            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(ownerName.trim());
            }
        }

        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Apply base64 texture using reflection (1.8 compatible)
     */
    private static void applyTexture(ItemMeta meta, String base64) {
        try {
            // GameProfile
            Class<?> gameProfileClass = classOrNull("com.mojang.authlib.GameProfile");
            if (gameProfileClass == null) {
                gameProfileClass = classOrNull("net.minecraft.util.com.mojang.authlib.GameProfile");
            }
            if (gameProfileClass == null) return;

            // Property
            Class<?> propertyClass = classOrNull("com.mojang.authlib.properties.Property");
            if (propertyClass == null) {
                propertyClass = classOrNull("net.minecraft.util.com.mojang.authlib.properties.Property");
            }
            if (propertyClass == null) return;

            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64);

            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getProperties.invoke(profile);

            Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(propertyMap, "textures", property);

            // Inject profile into SkullMeta
            Field profileField = getField(meta.getClass(), "profile");
            if (profileField == null) {
                profileField = getFirstFieldAssignable(meta.getClass(), gameProfileClass);
            }

            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }

        } catch (Throwable t) {
            Bukkit.getLogger().warning("[RankGift] Failed to apply skull texture: " + t.getMessage());
        }
    }

    /* ===================== REFLECTION HELPERS ===================== */

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field getFirstFieldAssignable(Class<?> clazz, Class<?> type) {
        for (Field field : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}
