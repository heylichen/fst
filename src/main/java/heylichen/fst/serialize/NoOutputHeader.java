package heylichen.fst.serialize;

/**
 * with no output and no state output
 * <p>
 * one byte data layout
 * from address high to low
 * bits       5         1          1            1
 * bit   label index  final   last trans    no_address
 */
public class NoOutputHeader extends FstRecordHeader {
  public NoOutputHeader() {
  }

  public NoOutputHeader(byte header) {
    super(header);
  }

  @Override
  public int getLabelIndex() {
    // shift is defined on integer
    // so the byte will be converted to int first then shift
    // first convert the signed integer as the integer from unsigned byte
    return (header & 0xFF) >> 3;
  }

  @Override
  public void setLabelIndex(int index) {
    if (index > 31 || index < 0) {
      throw new IllegalArgumentException("invalid index");
    }
    header = (byte) ((header & 0xFF) | index << 3);
  }

  @Override
  public int getCharIndexSize() {
    //because we have 5 bits to represent frequent chars
    return 32;
  }

  @Override
  public boolean hasOutput() {
    return false;
  }

  @Override
  public boolean hasStateOutput() {
    return false;
  }
}
