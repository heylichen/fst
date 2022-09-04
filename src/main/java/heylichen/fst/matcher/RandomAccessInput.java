package heylichen.fst.matcher;

/**
 * @author lichen
 * @date 2022-9-1 8:43
 */
public interface RandomAccessInput {
  byte readByte(long offset);

  byte[] readBytes(long startOffset, int len);

  long getSize();
}
