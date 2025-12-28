package me.herex.rankgift.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class SkullUtil {

    private SkullUtil() {}

    /**
     * Create a player head (1.8.8: SKULL_ITEM with durability 3) by player name.
     */
    public static ItemStack skullFromPlayerName(String ownerName) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = skull.getItemMeta();
        try {
            // Works on 1.8: setOwner(String)
            Method setOwner = meta.getClass().getMethod("setOwner", String.class);
            setOwner.invoke(meta, ownerName);
        } catch (Throwable ignored) {
            // ignore if not available
        }
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Create a custom textured head from base64 texture value.
     * base64 example starts with "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dX..."
     */
    public static ItemStack skullFromTexture(String base64Texture) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (base64Texture == null || base64Texture.trim().isEmpty()) return skull;

        ItemMeta meta = skull.getItemMeta();
        applyTextureToMeta(meta, base64Texture);
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Applies base64 texture to a SkullMeta (without compile-time GameProfile dependency).
     */
    private static void applyTextureToMeta(ItemMeta meta, String base64) {
        if (meta == null || base64 == null || base64.isEmpty()) return;

        try {
            // Try both class names (some builds shade it differently)
            Class<?> gameProfileClass = classOrNull("com.mojang.authlib.GameProfile");
            if (gameProfileClass == null) {
                gameProfileClass = classOrNull("net.minecraft.util.com.mojang.authlib.GameProfile");
            }
            if (gameProfileClass == null) return;

            Class<?> propertyClass = classOrNull("com.mojang.authlib.properties.Property");
            if (propertyClass == null) {
                propertyClass = classOrNull("net.minecraft.util.com.mojang.authlib.properties.Property");
            }
            if (propertyClass == null) return;

            // new GameProfile(UUID, String)
            Constructor<?> gpCtor = gameProfileClass.getConstructor(UUID.class, String.class);
            Object profile = gpCtor.newInstance(UUID.randomUUID(), "RankGift");

            // profile.getProperties().put("textures", new Property("textures", base64))
            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getProperties.invoke(profile);

            Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class);
            Object texturesProp = propCtor.newInstance("textures", base64);

            // PropertyMap#put(String, Property)
            Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(propertyMap, "textures", texturesProp);

            // Set CraftMetaSkull's private "profile" field
            Field profileField = getField(meta.getClass(), "profile");
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, profile);
                return;
            }

            // Fallback: some builds use a different field name
            Field prof = getFirstFieldAssignable(meta.getClass(), gameProfileClass);
            if (prof != null) {
                prof.setAccessible(true);
                prof.set(meta, profile);
            }

        } catch (Throwable t) {
            // Do not spam console; just fail gracefully
        }
    }

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
        try {
            for (Field f : clazz.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static ItemStack makeSkull(String ownerName, String texture) {
        return skullFromPlayerName(ownerName);
    }

    public static ItemStack makeSkullFromTexture(String base64Texture) {
        return skullFromTexture(base64Texture);
    }

}
