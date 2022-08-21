package heylichen.fst.serialize;

public final class BytesCodec {
  private BytesCodec() {
  }

  public static long decodeLong(byte[] bytes) {
    long v = 0;
    for (int i = 0; i < 8; i++) {
      v += ((long) (bytes[i] & 0xFF)) << (8 * i);
    }
    return v;
  }

  public static byte[] encode(int v) {
    byte[] b = new byte[4];
    for (int i = 0; i < 4; i++) {
      b[i] = (byte) (v & 0xFF);
    }
    return b;
  }

  public static byte[] encode(long v) {
    byte[] b = new byte[8];
    for (int i = 0; i < 8; i++) {
      b[i] = (byte) (v & 0xFF);
      v = v >> 8;
    }
    return b;
  }

}
