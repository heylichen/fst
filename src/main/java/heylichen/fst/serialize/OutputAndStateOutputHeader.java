package heylichen.fst.serialize;

/**
 * with output and state output
 * <p>
 * one byte data layout
 * from address high to low
 * bits       3            1                  1             1          1            1
 * bit   label index  has state output    has output      final   last trans    no_address
 */
public class OutputAndStateOutputHeader extends RecordHeader {
  public static final int OUTPUT_FLAG = 0b0000_1000;
  public static final int STATE_OUTPUT_FLAG = 0b0001_0000;

  @Override
  public int getLabelIndex() {
    // shift is defined on integer
    // so the byte will be converted to int first then shift
    // first convert the signed integer as the integer from unsigned byte
    return (header & 0xFF) >> 5;
  }

  @Override
  public void setLabelIndex(int index) {
    if (index > 7 || index < 0) {
      throw new IllegalArgumentException("invalid index");
    }
    header = (byte) ((header & 0xFF) | index << 5);
  }

  @Override
  public int getCharIndexSize() {
    //because we have only 3 bits to represent frequent chars
    return 8;
  }

  @Override
  boolean hasOutput() {
    return (header & OUTPUT_FLAG) != 0;
  }

  void setHasOutput(boolean has) {
    header = (byte) (has ? (header & 0xFF | OUTPUT_FLAG) : (header & 0xFF & ~OUTPUT_FLAG));
  }

  void setHasStateOutput(boolean has) {
    header = (byte) (has ? (header & 0xFF | STATE_OUTPUT_FLAG) : (header & 0xFF & ~STATE_OUTPUT_FLAG));
  }


  @Override
  boolean hasStateOutput() {
    return (header & STATE_OUTPUT_FLAG) != 0;
  }
}
