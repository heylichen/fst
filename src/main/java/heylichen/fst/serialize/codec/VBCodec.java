package heylichen.fst.serialize.codec;

import heylichen.fst.matcher.RandomAccessInput;

import java.io.IOException;
import java.io.OutputStream;

public final class VBCodec {
  private VBCodec() {
  }

  /**
   * calculate how many bytes is needed to hold the encoded value
   *
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


  public static LenInt decodeReverse(RandomAccessInput input, long offset) {
    long p = offset;
    int bytes = 0;
    int value = 0;

    int currentByteValue;
    while ((currentByteValue = (int) input.readByte(p) & 0xFF) < 128) {
      value += currentByteValue << (7 * bytes);
      p--;
      bytes++;
    }
    value += ((int) input.readByte(p) & 0xFF - 128) << (7 * bytes);
    bytes++;
    return new LenInt(bytes, value);
  }

  private static void swap(byte[] out, int i, int j) {
    byte tmp = out[j];
    out[j] = out[i];
    out[i] = tmp;
  }


  public static int encode(long value, byte[] out) {
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

  public static int encodeReverse(long value, byte[] out) {
    int len = encode(value, out);
    for (int i = 0; i < len / 2; i++) {
      swap(out, i, len - 1 - i);
    }
    return len;
  }

  public static LenLong decodeLongReverse(RandomAccessInput input, long offset) {
    long p = offset;
    int bytes = 0;
    long value = 0;

    int currentByteValue;
    while ((currentByteValue = (int) input.readByte(p) & 0xFF) < 128) {
      value += currentByteValue << (7 * bytes);
      p--;
      bytes++;
    }
    value += ((int) input.readByte(p) & 0xFF - 128) << (7 * bytes);
    bytes++;
    return new LenLong(bytes, value);
  }

}
