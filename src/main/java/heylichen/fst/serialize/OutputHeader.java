package heylichen.fst.serialize;

/**
 * with output but no state output
 * <p>
 * one byte data layout
 * from address high to low
 * bits       4           1             1          1            1
 * bit   label index  has output      final   last trans    no_address
 */
public class OutputHeader extends RecordHeader {
  public static final int OUTPUT_FLAG = 0b0000_1000;

  public OutputHeader() {
  }

  public OutputHeader(byte header) {
    super(header);
  }

  @Override
  public int getLabelIndex() {
    // shift is defined on integer
    // so the byte will be converted to int first then shift
    // first convert the signed integer as the integer from unsigned byte
    return (header & 0xFF) >> 4;
  }

  @Override
  public void setLabelIndex(int index) {
    if (index > 15 || index < 0) {
      throw new IllegalArgumentException("invalid index");
    }
    header = (byte) ((header & 0xFF) | index << 4);
  }

  @Override
  public int getCharIndexSize() {
    //because we have 4 bits to represent frequent chars
    return 16;
  }

  @Override
  public boolean hasOutput() {
    return (header & OUTPUT_FLAG) != 0;
  }

  void setHasOutput(boolean has) {
    header = (byte) (has ? (header & 0xFF | OUTPUT_FLAG) : (header & 0xFF & ~OUTPUT_FLAG));
  }

  void setHasStateOutput(boolean has) {
  }


  @Override
  public boolean hasStateOutput() {
    return false;
  }
}
