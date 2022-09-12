package heylichen.fst.matcher;

import heylichen.fst.serialize.codec.VBCodec;

import java.nio.charset.StandardCharsets;

public final class AlphabetUtil {
  public static final byte[][] ASCII_BYTES = new byte[128][1];

  static {
    for (int i = 0; i < 128; i++) {
      ASCII_BYTES[i][0] = (byte) i;
    }
  }

  private AlphabetUtil() {
  }

  public static byte[] reverseEncodeUTF8(char a) {
    if (a < 128) {
      return ASCII_BYTES[a];
    }
    return VBCodec.reverse(String.valueOf(a).getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] reverseReadUTF8Bytes(RandomAccessInput input, long offset) {
    long p = offset;
    byte firstByte = input.readByte(p--);
    int count = calculateUTF8ByteCount(firstByte);
    if (count == 1) {
      return ASCII_BYTES[firstByte];
    }
    byte[] result = new byte[count];
    int i = 0;
    result[i++] = firstByte;

    while (i < count) {
      result[i++] = input.readByte(p--);
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
