package heylichen.fst.serialize;

import heylichen.fst.output.OutputType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FstHeader {
  private Flags flags;
  private long startAddress;
  private char charIndex[];
  private boolean needOutput;
  private boolean needStateOutput;

  public FstHeader() {
    flags = new Flags();
  }

  public FstHeader(OutputType outputType, boolean needStateOutput, long startAddress,
                   Map<Character, Integer> charToIndexMap
  ) {
    this.startAddress = startAddress;
    this.needOutput = outputType != OutputType.NONE;
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

  public boolean read(RandomAccessInput input, long byteCodeSize) throws IOException {
    long remaining = byteCodeSize;
    if (remaining < 1) {
      return false;
    }
    input.seek(byteCodeSize - 1);
    flags.data = input.readBackward();
    initNeedOutput();

    remaining -= 1;
    if (remaining < 8) {
      //we are going to read a long
      return false;
    }

    byte[] startAddrBytes = input.readBackward(8);
    this.startAddress = BytesCodec.decodeLong(startAddrBytes);
    remaining -= 8;

    int size = getCharIndexSize();
    if (remaining < size) {
      return false;
    }

    byte[] charIndexBytes = input.readBackward(size);
    String tempStr =  new String(charIndexBytes, StandardCharsets.UTF_8);
    this.charIndex = tempStr.toCharArray();

    initNeedOutput();
    return true;
  }

  private void initNeedOutput() {
    this.needOutput = flags.getOutputType() != OutputType.NONE;
    this.needStateOutput = flags.hasStateOutput();
  }

  public void write(OutputStream os) throws IOException {
    os.write(String.valueOf(charIndex).getBytes(StandardCharsets.UTF_8));
    os.write(BytesCodec.encode(startAddress));
    os.write(flags.data);
  }

  private int getCharIndexSize() {
    return RecordHeader.getCharIndexSize(needOutput, needStateOutput);
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
}
