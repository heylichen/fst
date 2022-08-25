package heylichen.fst.serialize;

import heylichen.fst.output.IntOutput;
import heylichen.fst.output.Output;
import heylichen.fst.output.OutputOperation;
import heylichen.fst.output.VBCodec;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class FstRecord<T> {
  private RecordHeader header;
  private char label;
  private int delta;
  private boolean needOutput;
  private boolean needStateOutput;
  private Output<T> output;
  private Output<T> stateOutput;
  private final IntOutput intOutput = IntOutput.INSTANCE;

  public FstRecord(boolean needOutput, boolean needStateOutput) {
    this.needOutput = needOutput;
    this.needStateOutput = needStateOutput;
    if (!needOutput) {
      header = new NoOutputHeader();
    } else if (needStateOutput) {
      header = new OutputAndStateOutputHeader();
    } else {
      header = new OutputHeader();
    }
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
    if (needOutput && header.hasOutput()) {
      size += output.getByteValueSize();
    }
    if (needStateOutput && header.hasStateOutput()) {
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
    if (needOutput) {
      if (needStateOutput && header.hasStateOutput()) {
        stateOutput.writeByteValue(os);
      }
      if (header.hasOutput()) {
        output.writeByteValue(os);
      }
    }
    if (!header.isNoAddress()) {
      intOutput.writeByteValue(os, delta);
    }
    //label index 0 means label is not in frequent char table, need direct store
    if (header.getLabelIndex() == 0) {
      // use UTF-8 encoding
      os.write(String.valueOf(label).getBytes(StandardCharsets.UTF_8));
    }
    os.write(header.header & 0xFF);
  }

}
