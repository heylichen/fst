package heylichen.fst.matcher;

import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import heylichen.fst.serialize.FstHeader;
import heylichen.fst.serialize.FstRecordBody;
import heylichen.fst.serialize.FstRecordHeader;
import heylichen.fst.serialize.FstRecordWriter;
import heylichen.fst.serialize.codec.LenInt;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.function.BiConsumer;

public class FstReader<O> {
  protected final RandomAccessInput input;
  @Getter
  protected final FstHeader fstHeader;
  @Getter
  private final OutputFactory<O> outputFactory;

  public FstReader(RandomAccessInput input, OutputFactory<O> outputFactory) throws IOException {
    this.input = input;
    this.outputFactory = outputFactory;
    fstHeader = new FstHeader();
    fstHeader.read(input, input.getSize());
  }

  /**
   * read current transition record format. Must correspond to FstRecordWriter written layout.
   * left to right, from low address to high address.
   * field in [] is optional.
   * [stateOutput] | [output] | [delta] | [arc label] | recordHeaderByte
   *
   * @param recordHeader
   * @param p
   * @return
   */
  protected FstRecordBody<O> readRecord(FstRecordHeader recordHeader, Offset p) {
    char arc = readArc(recordHeader, p);
    // delta is position diff between current transition and next state
    // if current state is last state, delta is 0
    int deltaToNextState = 0;
    if (!recordHeader.isNoAddress()) {
      LenInt lenInt = VBCodec.decodeReverse(input, p.get());
      deltaToNextState = lenInt.getValue();
      p.subtract(lenInt.getLen());
    }

    Output<O> outputSuffix = null;
    if (recordHeader.hasOutput()) {
      Pair<Output<O>, Integer> pair = outputFactory.readByteValue(input, p.get());
      outputSuffix = pair.getKey();
      p.subtract(pair.getValue());
    }

    Output<O> stateOutput = null;
    if (recordHeader.hasStateOutput()) {
      Pair<Output<O>, Integer> pair = outputFactory.readByteValue(input, p.get());
      stateOutput = pair.getKey();
      p.subtract(pair.getValue());
    }

    return new FstRecordBody<>(arc, deltaToNextState, outputSuffix, stateOutput);
  }

  protected char readArc(FstRecordHeader recordHeader, Offset p) {
    int index = recordHeader.getLabelIndex();
    if (FstRecordWriter.isValid(index)) {
      return fstHeader.getChar(index);
    } else {
      return FstRecordWriter.readLabel(input, p);
    }
  }

  protected boolean needOutput() {
    return fstHeader.isNeedOutput();
  }

  protected FstRecordHeader newRecordHeader(byte recordHeaderByte) {
    return FstRecordHeader.newInstance(fstHeader.isNeedOutput(), fstHeader.isNeedStateOutput(), recordHeaderByte);
  }

  protected Output<O> newOutput() {
    return needOutput() ? outputFactory.newInstance() : null;
  }

  // -------------helper methods
  public VisitContext<O> prefixVisitContext(String prefix, BiConsumer<String, Output<O>> accept, Automaton automaton) {
    return contextBuilder()
        .withAutomaton(automaton)
        .withConsumer(accept)
        .withPrefix(prefix).build();
  }

  public VisitContext<O> noPrefixVisitContext(BiConsumer<String, Output<O>> accept, Automaton automaton) {
    return contextBuilder()
        .withPrefix("")
        .withAutomaton(automaton)
        .withConsumer(accept).build();
  }

  private VisitContext.VisitContextBuilder<O> contextBuilder() {
    VisitContext.VisitContextBuilder<O> builder = VisitContext.builder();
    builder.withPartialOutput(newOutput())
        .withPartialKey("");
    return builder;
  }
}
