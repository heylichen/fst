package heylichen.fst.matcher;

public final class StringUtils {
  private StringUtils() {
  }

  public static int length(String a) {
    return a == null ? 0 : a.length();
  }

  public static boolean isEmpty(String a) {
    return length(a) == 0;
  }
}
