package heylichen.fst.serialize;

import heylichen.fst.matcher.RandomAccessInput;
import heylichen.fst.output.OutputType;
import heylichen.fst.serialize.codec.LenInt;
import heylichen.fst.serialize.codec.LenLong;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * store meta info for the whole FST.
 */
public class FstHeader {
  private Flags flags;
  @Getter
  private long startAddress;
  private char charIndex[];
  @Getter
  private boolean needOutput;
  @Getter
  private boolean needStateOutput;

  public FstHeader() {
    flags = new Flags();
  }

  public FstHeader(OutputType outputType, boolean needStateOutput, long startAddress,
                   Map<Character, Integer> charToIndexMap
  ) {
    this.startAddress = startAddress;
    this.needOutput = outputType != null && outputType != OutputType.NONE;
    this.needStateOutput = needStateOutput;
    flags = new Flags();
    flags.setOutputType(outputType);
    flags.setNeedStateOutput(needStateOutput);

    int size = getCharIndexSize();
    charIndex = new char[size];
    for (Map.Entry<Character, Integer> entry : charToIndexMap.entrySet()) {
      int index = entry.getValue();
      if (index > 0 && index < size) {
        charIndex[index] = entry.getKey();
      }
    }
  }

  /**
   * read header
   * from low address to high address layout:
   * char index map | char index map bytes count | startAddress | flag byte
   * @param input
   * @param byteCodeSize
   * @return
   * @throws IOException
   */
  public void read(RandomAccessInput input, long byteCodeSize) throws IOException {
    long remaining = byteCodeSize;
    if (remaining < 1) {

      throw new IllegalArgumentException("invalid input, failed to read fst header!");
    }
    long p = byteCodeSize - 1;
    flags.data = input.readByte(p);
    p--;
    initNeedOutput();

    remaining -= 1;
    if (remaining < 8) {
      //we are going to read a long
      throw new IllegalArgumentException("invalid input, failed to read fst header!");
    }

    LenLong lenLong = VBCodec.decodeLongReverse(input, p);
    this.startAddress = lenLong.getValue();
    p -= lenLong.getLen();
    remaining -= lenLong.getLen();

    LenInt lenInt = VBCodec.decodeReverse(input, p);
    int charIndexMapSize = lenInt.getValue();
    p -= lenInt.getLen();

    byte[] charIndexBytes = input.readBytes(p - charIndexMapSize + 1, charIndexMapSize);
    String tempStr = new String(charIndexBytes, StandardCharsets.UTF_8);
    this.charIndex = tempStr.toCharArray();
  }


  private void initNeedOutput() {
    this.needOutput = flags.getOutputType() != OutputType.NONE;
    this.needStateOutput = flags.hasStateOutput();
  }

  /**
   *  write header as bytes into os
   *  from low address to high address layout:
   *  char index map | char index map bytes count | startAddress | flag byte
   *  write the char index map bytes count because in UTF-8, a char may be more than one byte.
   * @param os
   * @throws IOException
   */
  public void write(OutputStream os) throws IOException {
    byte[] charIndexBytes = String.valueOf(charIndex).getBytes(StandardCharsets.UTF_8);
    os.write(charIndexBytes);
    VBCodec.encodeReverse(charIndexBytes.length, os);
    VBCodec.encodeReverse(startAddress, os);
    os.write(flags.data);
  }

  private int getCharIndexSize() {
    return FstRecordHeader.getCharIndexSize(needOutput, needStateOutput);
  }

  /**
   * from address low to high
   * bits       3                 1             4
   * bit   output type   need state output   reserved
   */
  private static class Flags {
    private byte data;

    public void setOutputType(OutputType ot) {
      int typeValue = ot.getValue();
      if (typeValue < 0 || typeValue > 7) {
        throw new IllegalArgumentException("output type value must be <=7");
      }
      data = (byte) ((data & 0xFF) | typeValue);
    }

    public void setNeedStateOutput(boolean need) {
      int needFlag = need ? 1 : 0;
      needFlag = needFlag << 3;
      data = (byte) ((data & 0xFF) | needFlag);
    }

    public OutputType getOutputType() {
      int v = (data & 0xFF) & 0b0000_0111;
      return OutputType.parse(v);
    }

    public boolean hasStateOutput() {
      return ((data & 0xFF) & 0b0000_1000) != 0;
    }
  }

  public OutputType getFlagOutputType() {
    return flags.getOutputType();
  }

  public char getChar(int index) {
    return charIndex[index];
  }
}
