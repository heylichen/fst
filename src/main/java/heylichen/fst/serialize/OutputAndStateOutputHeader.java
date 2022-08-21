package heylichen.fst.serialize;

/**
 * with output and state output
 *
 * one byte data layout
 * from address low to high
 * bits       1            1         1         1             1               3
 * bit   no_address    last trans   final  has output has state output  label index
 */
public class OutputAndStateOutputHeader extends RecordHeader {

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
    header = (byte) ((header & 0xFF) | index);
  }

  @Override
  public int getCharIndexSize() {
    //because we have only 3 bits to represent frequent chars
    return 8;
  }

  @Override
  boolean hasOutput() {
    return true;
  }

  @Override
  boolean hasStateOutput() {
    return true;
  }
}
