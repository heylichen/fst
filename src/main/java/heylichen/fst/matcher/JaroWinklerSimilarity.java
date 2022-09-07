package heylichen.fst.matcher;

/**
 * @author lichen
 * @date 2022-9-6 19:21
 */
public class JaroWinklerSimilarity {
  private static final double ONE_THIRD = 1.0 / 3.0;
  private static final double P = 0.1;
  private final double p;

  public JaroWinklerSimilarity() {
    p = P;
  }

  public JaroWinklerSimilarity(double p) {
    this.p = p;
  }

  public double getJaroSim(String a, String b) {
    boolean aEmpty = isEmpty(a);
    boolean bEmpty = isEmpty(b);
    if (aEmpty && bEmpty) {
      return 1;
    } else if (aEmpty != bEmpty) {
      return 0;
    }
    String commonAb = getCommonChars(a, b);
    if (commonAb.length() == 0) {
      return 0;
    }
    String commonBa = getCommonChars(b, a);
    int t = 0;
    int end = Math.min(commonAb.length(), commonBa.length());
    for (int i = 0; i < end; i++) {
      if (commonAb.charAt(i) != commonBa.charAt(i)) {
        t++;
      }
    }
    t = t / 2;

    double m = commonAb.length();
    return ONE_THIRD * (m / a.length() + m / b.length() + (m - t) / m);
  }

  public double getJaroWinklerSim(String a, String b) {
    double simJ = getJaroSim(a, b);
    int l = Math.min(4, getCommonPrefixLen(a, b));
    return simJ + l * p * (1 - simJ);
  }

  private int getCommonPrefixLen(String a, String b) {
    if (isEmpty(a) || isEmpty(b)) {
      return 0;
    }
    int end = Math.min(a.length(), b.length());
    int i = 0;
    for (; i < end; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        break;
      }
    }
    return i;
  }

  private boolean isEmpty(String a) {
    return a == null || a.length() == 0;
  }

  private String getCommonChars(String a, String b) {
    int range = maxRange(a, b);
    StringBuilder sb = new StringBuilder(a.length());
    for (int i = 0; i < a.length(); i++) {
      int from = Math.max(0, i - range);
      int to = Math.min(b.length(), i + range + 1);
      char charA = a.charAt(i);
      for (int j = from; j < to; j++) {
        if (charA == b.charAt(j)) {
          sb.append(charA);
          break;
        }
      }
    }
    return sb.toString();
  }

  private int maxRange(String a, String b) {
    return Math.max(a.length(), b.length()) / 2 - 1;
  }


}
