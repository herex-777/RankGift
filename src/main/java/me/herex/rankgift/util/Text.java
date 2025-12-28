package me.herex.rankgift.util;

import java.util.List;

public final class Text {
  private Text() {}
  public static String color(String s) {
    return s == null ? null : s.replace('&','§');
  }
  public static void colorInPlace(List<String> list) {
    if (list == null) return;
    for (int i=0;i<list.size();i++) list.set(i, color(list.get(i)));
  }
}
