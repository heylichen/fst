package heylichen.fst.matcher;

public final class AlphabetUtil {
  private AlphabetUtil() {
  }

  public static byte[] readUTF8Bytes(RandomAccessInput input, long offset) {
    long p = offset;
    byte firstByte = input.readByte(p++);
    int count = calculateUTF8ByteCount(firstByte);
    byte[] result = new byte[count];

    int i = 0;
    result[i++] = firstByte;
    if (count == 1) {
      return result;
    }

    while (i < count) {
      result[i++] = input.readByte(p++);
    }
    return result;
  }

  static int calculateUTF8ByteCount(byte firstByte) {
    if (firstByte > 0) {
      return 1;
    }
    int n = 7;
    while ((firstByte & (1 << n)) != 0 && n >= 4) {
      n--;
    }
    return 7 - n;
  }

}
