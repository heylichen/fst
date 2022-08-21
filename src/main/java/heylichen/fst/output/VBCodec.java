package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public final class VBCodec {
  private VBCodec() {
  }

  /**
   * calculate how many bytes is needed to hold the encoded value
   * @param value
   * @return
   */
  public static int getEncodedBytes(int value) {
    int len = 0;
    while (value >= 128) {
      len++;
      value = value >> 7;
    }
    len++;
    return len;
  }

  public static int encode(int value, byte[] out) {
    int len = 0;
    while (value >= 128) {
      out[len] = (byte) (value & 0x7F);
      len++;
      value = value >> 7;
    }
    out[len] = (byte) (value + 128);
    len++;
    return len;
  }

  public static int encodeReverse(int value, byte[] out) {
    int len = encode(value, out);
    for (int i = 0; i < len / 2; i++) {
      swap(out, i, len - 1 - i);
    }
    return len;
  }

  public static int encodeReverse(int value, OutputStream os) throws IOException {
    byte[] buf = new byte[16];
    int len = encodeReverse(value, buf);
    os.write(buf, 0, len);
    return len;
  }


  public static LenInt decodeReverse(byte[] data) {
    //TODO
    return null;
  }

  private static void swap(byte[] out, int i, int j) {
    byte tmp = out[j];
    out[j] = out[i];
    out[i] = tmp;
  }


}
