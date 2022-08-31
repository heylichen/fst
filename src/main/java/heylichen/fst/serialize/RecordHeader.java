package heylichen.fst.serialize;

import heylichen.fst.output.Output;

/**
 *  * one byte data layout
 *  * from address high to low
 *  * bits   5             1          1            1
 *  * bit  custom     final   last trans    no_address
 */
public abstract class RecordHeader {
  public static final int NO_ADDRESS = 0b0000_0001;
  public static final int LAST_TRANSITION = 0b0000_0010;
  public static final int FINAL = 0b0000_0100;

  protected byte header;

  public RecordHeader() {
    header = 0;
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

  public void setNoAddress(boolean noAddress) {
    header = (byte) (noAddress ? (header & 0xFF | NO_ADDRESS) : (header & 0xFF & ~NO_ADDRESS));
  }

  public boolean isLastTransition() {
    return (header & LAST_TRANSITION) != 0;
  }

  public void setLastTransition(boolean last) {
    header = (byte) (last ? (header & 0xFF | LAST_TRANSITION) : (header & 0xFF & ~LAST_TRANSITION));
  }

  public boolean isFinal() {
    return (header & FINAL) != 0;
  }

  public void setFinal(boolean isFinal) {
    header = (byte) (isFinal ? (header & 0xFF | FINAL) : (header & 0xFF & ~FINAL));
  }

  abstract boolean hasOutput();

  abstract boolean hasStateOutput();

  void setHasOutput(boolean has) {
  }

  void setHasStateOutput(boolean has) {
  }

  public static int getCharIndexSize(boolean hasOutput, boolean hasStateOutput) {
    return !hasOutput ? 32 : (hasStateOutput ? 8 : 16);
  }
}
