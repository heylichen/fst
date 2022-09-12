package heylichen.fst.serialize;

/**
 * * one byte data layout, hold meta info for one transition record.
 * * from address high to low
 * * bits   5             1          1            1
 * * bit  custom     final   last trans    no_address
 */
public abstract class FstRecordHeader {
  private static final int NO_ADDRESS = 0b0000_0001;
  private static final int LAST_TRANSITION = 0b0000_0010;
  private static final int FINAL = 0b0000_0100;

  private static final int INVALID_LABEL_INDEX = 0;

  protected byte header;

  public FstRecordHeader() {
    header = 0;
  }

  public FstRecordHeader(byte header) {
    this.header = header;
  }

  public static FstRecordHeader newInstance(boolean needOutput, boolean needStateOutput) {
    FstRecordHeader header;
    if (!needOutput) {
      header = new NoOutputHeader();
    } else if (needStateOutput) {
      header = new OutputAndStateOutputHeader();
    } else {
      header = new OutputHeader();
    }
    return header;
  }

  public static FstRecordHeader newInstance(boolean needOutput, boolean needStateOutput, byte data) {
    FstRecordHeader header;
    if (!needOutput) {
      header = new NoOutputHeader(data);
    } else if (needStateOutput) {
      header = new OutputAndStateOutputHeader(data);
    } else {
      header = new OutputHeader(data);
    }
    return header;
  }

  public abstract int getLabelIndex();

  public abstract void setLabelIndex(int index);

  public abstract int getCharIndexSize();

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

  public abstract boolean hasOutput();

  public abstract boolean hasStateOutput();

  void setHasOutput(boolean has) {
  }

  void setHasStateOutput(boolean has) {
  }

  public static int getCharIndexSize(boolean hasOutput, boolean hasStateOutput) {
    return !hasOutput ? 32 : (hasStateOutput ? 8 : 16);
  }

}
