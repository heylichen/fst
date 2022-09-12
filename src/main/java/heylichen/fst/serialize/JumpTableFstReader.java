package heylichen.fst.serialize;

import heylichen.fst.matcher.FstReader;
import heylichen.fst.matcher.Offset;
import heylichen.fst.matcher.RandomAccessInput;
import heylichen.fst.matcher.RecordMetaBytes;
import heylichen.fst.output.OutputFactory;
import heylichen.fst.serialize.codec.LenInt;
import heylichen.fst.serialize.codec.VBCodec;

import java.io.IOException;
import java.util.function.Function;

import static heylichen.fst.serialize.JumpTableWriter.JUMP_ELE_SIZE_MASK;
import static heylichen.fst.serialize.JumpTableWriter.JUMP_TAG_CHECK_MASK;

/**
 * Fst reader that knows how to read jump table.
 * For jump table bytes layout see JumpTableWriter.
 *
 * @param <O>
 */
public class JumpTableFstReader<O> extends FstReader<O> {

  public JumpTableFstReader(RandomAccessInput input, OutputFactory<O> outputFactor) throws IOException {
    super(input, outputFactor);
  }

  public static boolean isJumpTableHeaderByte(byte headByte) {
    return headByte == JumpTableWriter.JUMP_TABLE_HEAD;
  }

  public static boolean hasJumpTable(byte headByte, byte previousByte) {
    return isJumpTableHeaderByte(headByte) && (previousByte & JUMP_TAG_CHECK_MASK) == JUMP_TAG_CHECK_MASK;
  }

  public static int getJumpTableEleBytes(byte eleByte) {
    return (eleByte & JUMP_ELE_SIZE_MASK);
  }

  public void readRecordHead(Offset p, RecordMetaBytes meta) {
    byte recordHeaderByte = input.readByte(p.getAndAdd(-1));
    meta.setRecordHeaderByte(recordHeaderByte);
    if (!JumpTableFstReader.isJumpTableHeaderByte(recordHeaderByte)) {
      //can not have jump table, fast return.
      meta.setRecordHeader(newRecordHeader(recordHeaderByte));
      meta.setHasJumpTable(false);
      return;
    }

    byte previousByte = input.readByte(p.get());
    meta.setPreviousByte(previousByte);

    boolean hasJumpTable = JumpTableFstReader.hasJumpTable(recordHeaderByte, previousByte);
    meta.setHasJumpTable(hasJumpTable);
    if (hasJumpTable) {
      //advance read position for previous byte
      p.subtract(1);
      meta.setJumpTableEleSize(JumpTableFstReader.getJumpTableEleBytes(previousByte));
    } else {
      meta.setRecordHeader(newRecordHeader(recordHeaderByte));
    }
  }

  public boolean lookupInJumpTable(Offset transitionAddress, Offset p, RecordMetaBytes meta, char ch) {
    int jumpTableEleSize = meta.getJumpTableEleSize();
    LenInt lenInt = VBCodec.decodeReverse(input, p.get());

    int eleCount = lenInt.getValue();
    p.subtract(lenInt.getLen());
    p.subtract((long) eleCount * jumpTableEleSize);

    long jumpTableStart = p.get() + 1;
    int jumpTableByteSize = 2 + lenInt.getLen() + eleCount * jumpTableEleSize;
    long arcsBaseAddress = transitionAddress.get() - jumpTableByteSize;
    Function<Long, Character> getArcFunction = (Long index) -> {
      long off = arcsBaseAddress - lookupJumpTable(jumpTableStart, index.intValue(), jumpTableEleSize);
      Offset offP = new Offset(off);
      byte flag = input.readByte(offP.getAndAdd(-1));
      FstRecordHeader rh = newRecordHeader(flag);
      return readArc(rh, offP);
    };

    long foundIndex = binarySearchIndexForChar(0, eleCount,
        (Long index) -> getArcFunction.apply(index) < ch);

    if (foundIndex < eleCount && getArcFunction.apply(foundIndex) == ch) {
      long offset = lookupJumpTable(jumpTableStart, (int) foundIndex, jumpTableEleSize);
      transitionAddress.subtract((offset + jumpTableByteSize));
    } else {
      //no arc found in transitions of current state
      return false;
    }
    return true;
  }

  private int lookupJumpTable(long jumpTableStart, int i, int elementSize) {
    int value = input.readByte(jumpTableStart + i * elementSize) & 0xFF;
    if (elementSize == 2) {
      value += (input.readByte(jumpTableStart + i * elementSize + 1) & 0xFF) << 8;
    }
    return value;
  }

  public void readPassJumpTable(Offset p, RecordMetaBytes recordMetaBytes) {
    int jumpTableEleSize = recordMetaBytes.getJumpTableEleSize();
    LenInt lenInt = VBCodec.decodeReverse(input, p.get());

    int eleCount = lenInt.getValue();
    p.subtract(lenInt.getLen());
    p.subtract((long) eleCount * jumpTableEleSize);
  }

  /**
   * char is in less function
   *
   * @param first
   * @param last
   * @param less
   * @return
   */
  private long binarySearchIndexForChar(long first, long last, Function<Long, Boolean> less) {
    long len = last - first;

    while (len > 0) {
      long half = len >> 1;
      long middle = first + half;
      if (less.apply(middle)) {
        first = middle;
        first++;
        len = len - half - 1;
      } else {
        len = half;
      }
    }
    return first;
  }

}
