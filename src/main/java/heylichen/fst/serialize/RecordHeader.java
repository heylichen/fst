package heylichen.fst.serialize;

public abstract class RecordHeader {
  public static final int NO_ADDRESS = 0b1000_0000;
  public static final int LAST_TRANSITION = 0b0100_0000;
  public static final int FINAL = 0b0010_0000;

  protected byte header;

  public RecordHeader() {
  }

  public RecordHeader(byte header) {
    this.header = header;
  }

  public abstract int getLabelIndex();

  public abstract void setLabelIndex(int index);

  public abstract int getCharIndexSize();

  public boolean hasJumpTable() {
    return header == (byte) 0xFF || header == (byte) 0xFE;
  }

  public int getJumpTableElementSize() {
    return (header == (byte) 0xFF || header == (byte) 0xFD) ? 2 : 1;
  }

  public byte getJumpTableTag(boolean need2Bytes) {
    return need2Bytes ? (byte) 0xFF : (byte) 0xFE;
  }

  public boolean isNoAddress() {
    return (header & NO_ADDRESS) != 0;
  }

  public boolean isLastTransition() {
    return (header & LAST_TRANSITION) != 0;
  }

  public boolean isFinal() {
    return (header & FINAL) != 0;
  }

  abstract boolean hasOutput();

  abstract boolean hasStateOutput();

  public static int getCharIndexSize(boolean hasOutput, boolean hasStateOutput) {
    return !hasOutput ? 32 : (hasStateOutput ? 8 : 16);
  }
}
