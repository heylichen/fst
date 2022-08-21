package heylichen.fst.serialize;

/**
 * with no output and no state output
 *
 * one byte data layout
 * from address low to high
 * bits       1            1         1          5
 * bit   no_address    last trans   final  label index
 */
public class NoOutputHeader extends RecordHeader {

  @Override
  public int getLabelIndex() {
    // shift is defined on integer
    // so the byte will be converted to int first then shift
    // first convert the signed integer as the integer from unsigned byte
    return  (header & 0xFF) >> 3;
  }

  @Override
  public void setLabelIndex(int index) {
    if (index > 31 || index < 0) {
      throw new IllegalArgumentException("invalid index");
    }
    header = (byte) ((header & 0xFF) | index);
  }

  @Override
  public int getCharIndexSize() {
    //because we have 5 bits to represent frequent chars
    return 32;
  }

  @Override
  boolean hasOutput() {
    return false;
  }

  @Override
  boolean hasStateOutput() {
    return false;
  }
}
