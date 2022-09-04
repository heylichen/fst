package heylichen.fst.matcher;

public class ByteArrayInput implements RandomAccessInput {
  private final byte[] data;

  public ByteArrayInput(byte[] data) {
    this.data = data;
  }

  @Override
  public byte readByte(long offset) {
    return data[(int) offset];
  }

  @Override
  public byte[] readBytes(long startOffset, int len) {
    byte[] result = new byte[len];
    System.arraycopy(data, (int) startOffset, result, 0, len);
    return result;
  }

  public long getSize() {
    return data.length;
  }
}
