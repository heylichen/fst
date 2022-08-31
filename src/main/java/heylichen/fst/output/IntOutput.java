package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public class IntOutput implements Output<Integer> {
  private Integer data;

  public static final IntOutput INSTANCE = new IntOutput(null);

  public IntOutput(Integer data) {
    this.data = data;
  }

  @Override
  public OutputType type() {
    return OutputType.INT;
  }

  @Override
  public boolean empty() {
    return data.equals(0);
  }

  @Override
  public Integer getInitValue() {
    return 0;
  }

  @Override
  public void prepend(Output<Integer> preValue) {
    this.data += preValue.getData();
  }

  @Override
  public Output<Integer> getSuffix(Output<Integer> prefix) {
    return new IntOutput(data - prefix.getData());
  }

  @Override
  public Output<Integer> getCommonPrefix(Output<Integer> other) {
    return new IntOutput(Math.min(data, other.getData()));
  }

  @Override
  public int getByteValueSize() {
    return VBCodec.getEncodedBytes(data);
  }

  @Override
  public void writeByteValue(OutputStream os) throws IOException {
    VBCodec.encodeReverse(data, os);
  }

  public void writeByteValue(OutputStream os, Integer value) throws IOException {
    VBCodec.encodeReverse(value, os);
  }

  @Override
  public Integer getData() {
    return data;
  }
}
