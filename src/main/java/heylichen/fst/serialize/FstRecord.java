package heylichen.fst.serialize;

import heylichen.fst.output.IntOutput;
import heylichen.fst.output.OutputOperation;
import heylichen.fst.output.VBCodec;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FstRecord<T> {
  private RecordHeader header;
  private char label;
  private int delta;
  private boolean needOutput;
  private boolean needStateOutput;
  private T output;
  private T stateOutput;
  private final IntOutput intOutput = IntOutput.INSTANCE;

  public int getByteSize(OutputOperation<T> outputOperation) {
    // 1 is for record head byte
    int size = 1;
    if (header.getLabelIndex() == 0) {
      size++;
    }
    if (!header.isNoAddress()) {
      size += VBCodec.getEncodedBytes(delta);
    }
    if (needOutput && header.hasOutput()) {
      size += outputOperation.getByteValueSize(output);
    }
    if (needStateOutput && header.hasStateOutput()) {
      size += outputOperation.getByteValueSize(stateOutput);
    }
    return size;
  }

  /**
   * serialize a fst record.
   * Note that it's written in reverse order, record header written last.
   * @param os
   * @param outputOperation
   * @throws IOException
   */
  public void write(OutputStream os, OutputOperation<T> outputOperation) throws IOException {
    if (needOutput) {
      if (needStateOutput && header.hasStateOutput()) {
        outputOperation.writeByteValue(os, stateOutput);
      }
      if (header.hasOutput()) {
        outputOperation.writeByteValue(os, output);
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
