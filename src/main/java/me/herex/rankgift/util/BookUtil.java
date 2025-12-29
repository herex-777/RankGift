package me.herex.rankgift.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight JSON book GUI helper.
 *
 * This is intentionally written with reflection so it works on older Spigot versions
 * (1.8+) without hard NMS imports.
 */
public final class BookUtil {
  private BookUtil() {}

  public static Page newPage() { return new Page(); }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  private static String jsonText(String text) {
    return "{\"text\":\"" + escape(text) + "\"}";
  }

  private static String jsonClickable(String text, String command, String hover) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"text\":\"").append(escape(text)).append("\"");
    if (command != null) {
      sb.append(",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"")
        .append(escape(command))
        .append("\"}");
    }
    if (hover != null) {
      sb.append(",\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"")
        .append(escape(hover))
        .append("\"}}}");
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Opens a written book to the player.
   * Pages are JSON (for click events) when supported.
   */
  public static void openBook(Player player, String title, String author, List<Page> pages) {
    try {
      // Newer Spigot versions have Player#openBook(ItemStack)
      try {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(title);
        meta.setAuthor(author);

        // Try Spigot BookMeta API with BaseComponents
        boolean usedSpigot = false;
        try {
          Method spigotMethod = meta.getClass().getMethod("spigot");
          Object spigotMeta = spigotMethod.invoke(meta);
          // addPage(BaseComponent[])
          Class<?> baseComp = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
          Class<?> baseCompArray = java.lang.reflect.Array.newInstance(baseComp, 0).getClass();
          Method addPageMethod = spigotMeta.getClass().getMethod("addPage", baseCompArray);
          for (Page p : pages) {
            Object tc = Class.forName("net.md_5.bungee.api.chat.TextComponent").getConstructor(String.class).newInstance(p.build());
            Object arr = java.lang.reflect.Array.newInstance(Class.forName("net.md_5.bungee.api.chat.BaseComponent"), 1);
            java.lang.reflect.Array.set(arr, 0, tc);
            addPageMethod.invoke(spigotMeta, arr);
          }
          usedSpigot = true;
        } catch (Throwable ignored) {
          // fall back
        }

        if (!usedSpigot) {
          // Plain pages (no click events)
          List<String> plain = new ArrayList<String>();
          for (Page p : pages) plain.add(p.asPlainText());
          meta.addPage(plain.toArray(new String[0]));
        }
        book.setItemMeta(meta);
        Method openBookMethod = player.getClass().getMethod("openBook", ItemStack.class);
        openBookMethod.invoke(player, book);
        return;
      } catch (Throwable ignored) {
        // Continue to legacy (1.8) method
      }

      // Legacy (1.8–1.13): put book in hand, send MC|BOpen packet, restore item.
      String bukkitVersion = Bukkit.getBukkitVersion();
      String packageName = Bukkit.getServer().getClass().getPackage().getName();
      String version;
      if (packageName.equals("org.bukkit.craftbukkit")) {
        String[] parts = bukkitVersion.split("-")[0].split("\\.");
        version = "v" + parts[0] + "_" + parts[1] + "_R1";
      } else {
        version = packageName.substring(packageName.lastIndexOf('.') + 1);
      }

      // Build NMS written book ItemStack with NBT pages
      Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
      Object entityPlayer = craftPlayer.getMethod("getHandle").invoke(player);
      Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
      Class<?> nmsItem = Class.forName("net.minecraft.server." + version + ".Item");
      Class<?> nbtTagCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
      Class<?> nbtTagList = Class.forName("net.minecraft.server." + version + ".NBTTagList");
      Class<?> nbtBase = Class.forName("net.minecraft.server." + version + ".NBTBase");

      Object bookNms = nmsItemStack.getConstructor(nmsItem, int.class, int.class)
        .newInstance(nmsItem.getField("WRITTEN_BOOK").get(null), 1, 0);
      Object tag = nbtTagCompound.getConstructor().newInstance();
      nbtTagCompound.getMethod("setString", String.class, String.class).invoke(tag, "title", title);
      nbtTagCompound.getMethod("setString", String.class, String.class).invoke(tag, "author", author);
      Object pageList = nbtTagList.getConstructor().newInstance();
      Method addMethod = nbtTagList.getMethod("add", nbtBase);
      Class<?> nbtTagString = Class.forName("net.minecraft.server." + version + ".NBTTagString");
      for (Page p : pages) {
        Object pageText = nbtTagString.getConstructor(String.class).newInstance(p.build());
        addMethod.invoke(pageList, pageText);
      }
      nbtTagCompound.getMethod("set", String.class, nbtBase).invoke(tag, "pages", pageList);
      nmsItemStack.getMethod("setTag", nbtTagCompound).invoke(bookNms, tag);

      // Swap item in hand
      PlayerInventory inv = player.getInventory();
      ItemStack old;
      boolean isMainHand = false;
      try {
        Method getMain = inv.getClass().getMethod("getItemInMainHand");
        Method setMain = inv.getClass().getMethod("setItemInMainHand", ItemStack.class);
        old = (ItemStack) getMain.invoke(inv);
        ItemStack bukkitCopy = (ItemStack) Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack")
          .getMethod("asBukkitCopy", nmsItemStack)
          .invoke(null, bookNms);
        setMain.invoke(inv, bukkitCopy);
        isMainHand = true;
      } catch (NoSuchMethodException ex) {
        Method getHand = inv.getClass().getMethod("getItemInHand");
        Method setHand = inv.getClass().getMethod("setItemInHand", ItemStack.class);
        old = (ItemStack) getHand.invoke(inv);
        ItemStack bukkitCopy = (ItemStack) Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack")
          .getMethod("asBukkitCopy", nmsItemStack)
          .invoke(null, bookNms);
        setHand.invoke(inv, bukkitCopy);
      }

      // Send open book packet
      Class<?> packetDataSerializer = Class.forName("net.minecraft.server." + version + ".PacketDataSerializer");
      Class<?> packet = Class.forName("net.minecraft.server." + version + ".Packet");
      Class<?> packetPlayOutCustomPayload = Class.forName("net.minecraft.server." + version + ".PacketPlayOutCustomPayload");
      Object serializer = packetDataSerializer.getConstructor(ByteBuffer.class).newInstance(ByteBuffer.allocate(0));
      Object openPacket = packetPlayOutCustomPayload.getConstructor(String.class, packetDataSerializer)
        .newInstance("MC|BOpen", serializer);
      Object conn = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
      conn.getClass().getMethod("sendPacket", packet).invoke(conn, openPacket);

      // Restore old item
      if (isMainHand) {
        inv.getClass().getMethod("setItemInMainHand", ItemStack.class).invoke(inv, old);
      } else {
        inv.getClass().getMethod("setItemInHand", ItemStack.class).invoke(inv, old);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      try {
        player.sendMessage(ChatColor.RED + "Failed to open book GUI.");
      } catch (Throwable ignored) {}
    }
  }

  public static final class Page {
    private final List<String> components = new ArrayList<String>();
    private final StringBuilder plain = new StringBuilder();

    public Page addText(String text) {
      components.add(jsonText(text));
      plain.append(text);
      return this;
    }

    public Page addClickable(String text, String command, String hover) {
      components.add(jsonClickable(text, command, hover));
      plain.append(text);
      return this;
    }

    public Page addRaw(String json) {
      components.add(json);
      return this;
    }

    public String build() {
      return "{\"text\":\"\",\"extra\":[" + String.join(",", components) + "]}";
    }

    public String asPlainText() {
      return plain.toString();
    }
  }
}
