package heylichen.fst.serialize;

import heylichen.fst.output.Output;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * a transition record of one state
 *
 * @param <T>
 */
@Getter
@Setter
public class FstRecord<T> {
  private RecordHeader header;
  private char label;
  // delta is position diff between current transition and next state
  // if current state is last state, delta is 0
  private int delta;
  private Output<T> output;
  private Output<T> stateOutput;

  public FstRecord(boolean needOutput, boolean needStateOutput) {
    header = RecordHeader.newInstance(needOutput, needStateOutput);
  }

  public FstRecord(char label, int delta, Output<T> output, Output<T> stateOutput) {
    this.label = label;
    this.delta = delta;
    this.output = output;
    this.stateOutput = stateOutput;
  }

  public int getByteSize() {
    // 1 is for record head byte
    int size = 1;
    if (header.getLabelIndex() == 0) {
      size++;
    }
    if (!header.isNoAddress()) {
      size += VBCodec.getEncodedBytes(delta);
    }
    if (header.hasOutput()) {
      size += output.getByteValueSize();
    }
    if (header.hasStateOutput()) {
      size += stateOutput.getByteValueSize();
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
      stateOutput.writeByteValue(os);
    }
    if (header.hasOutput()) {
      output.writeByteValue(os);
    }

    if (!header.isNoAddress()) {
      VBCodec.encodeReverse(delta, os);
    }
    //label index 0 means label is not in frequent char table, need direct store
    if (header.getLabelIndex() == 0) {
      // use UTF-8 encoding
      os.write(String.valueOf(label).getBytes(StandardCharsets.UTF_8));
    }
    os.write(header.header & 0xFF);
  }

  public boolean isStateOutputEmpty() {
    return stateOutput == null || stateOutput.empty();
  }
}
