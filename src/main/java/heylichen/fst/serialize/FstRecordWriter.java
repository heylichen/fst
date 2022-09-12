package heylichen.fst.serialize;

import heylichen.fst.matcher.AlphabetUtil;
import heylichen.fst.matcher.Offset;
import heylichen.fst.matcher.RandomAccessInput;
import heylichen.fst.output.Output;
import heylichen.fst.output.Outputs;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FstRecordWriter<O> {
  @Getter
  private final FstRecordHeader header;
  private final FstRecordBody<O> body;
  private byte[] labelBytes;
  private static final int INVALID_LABEL_INDEX = 0;

  //for write
  public FstRecordWriter(boolean needOutput, boolean needStateOutput) {
    header = FstRecordHeader.newInstance(needOutput, needStateOutput);
    body = new FstRecordBody<>();
  }

  public void setNFL(boolean noAddress, boolean toFinalState, boolean lastTransition) {
    header.setNoAddress(noAddress);
    header.setLastTransition(lastTransition);
    header.setFinal(toFinalState);
  }

  public void setDelta(int delta) {
    body.setDelta(delta);
  }

  public void setOutput(Output<O> output) {
    header.setHasOutput(!Outputs.empty(output));
    body.setOutput(output);
  }

  public void setStateOutput(Output<O> stateOutput) {
    header.setHasStateOutput(!Outputs.empty(stateOutput));
    body.setStateOutput(stateOutput);
  }

  public int setLabel(char label, int labelIndex) {
    boolean indexValid = labelIndex < getCharIndexSize();
    int actualIndex = indexValid ? labelIndex : invalidLabelIndex();
    header.setLabelIndex(actualIndex);
    if (!indexValid) {
      this.labelBytes = encodeLabel(label);
    }
    return actualIndex;
  }

  public int getByteSize() {
    // 1 is for record head byte
    int size = 1;
    if (header.getLabelIndex() == 0) {
      size += labelBytes.length;
    }
    if (!header.isNoAddress()) {
      size += VBCodec.getEncodedBytes(body.getDelta());
    }
    if (header.hasOutput()) {
      size += body.getOutput().getByteValueSize();
    }
    if (header.hasStateOutput()) {
      size += body.getStateOutput().getByteValueSize();
    }
    return size;
  }

  /**
   * serialize a fst record.
   * Note that it's written in reverse order, record header written last.
   *
   * @param os
   * @throws IOException
   */
  public void write(OutputStream os) throws IOException {
    if (header.hasStateOutput()) {
      body.getStateOutput().writeByteValue(os);
    }
    if (header.hasOutput()) {
      body.getOutput().writeByteValue(os);
    }

    if (!header.isNoAddress()) {
      VBCodec.encodeReverse(body.getDelta(), os);
    }
    //label index 0 means label is not in frequent char table, need direct store
    if (header.getLabelIndex() == 0) {
      // use UTF-8 encoding
      os.write(labelBytes);
    }
    os.write(header.header & 0xFF);
  }

  public int getCharIndexSize() {
    return header.getCharIndexSize();
  }


  // -------------------- arc label related logic. keep them in one place, easier to make encode and decode
  // compatible with each other.
  public static byte[] encodeLabel(char label) {
    return AlphabetUtil.reverseEncodeUTF8(label);
  }

  public static char readLabel(RandomAccessInput input, Offset p) {
    byte[] charBytes = AlphabetUtil.reverseReadUTF8Bytes(input, p.get());
    p.subtract(charBytes.length);
    if (charBytes.length == 1) {
      return (char) charBytes[0];
    } else {
      String str = (new String(charBytes, StandardCharsets.UTF_8));
      if (str.length() != 1) {
        // only consider BMP in Unicode.
        // supplementary characters are represented as a pair of char values in Java.
        throw new IllegalArgumentException("only support Unicode BMP! chars in BMP have only one char! read more than one chars");
      }
      return str.charAt(0);
    }
  }

  public static int invalidLabelIndex() {
    return INVALID_LABEL_INDEX;
  }

  public static boolean isValid(int labelIndex) {
    return labelIndex > INVALID_LABEL_INDEX;
  }
}
