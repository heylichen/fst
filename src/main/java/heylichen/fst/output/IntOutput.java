package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public class IntOutput implements Output<Integer> {
  private Integer data;

  public static final IntOutput INSTANCE = new IntOutput();

  @Override
  public OutputType type() {
    return OutputType.INT;
  }

  @Override
  public boolean empty(Integer value) {
    return value.equals(0);
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
  public Integer getSuffix(Integer base, Integer prefix) {
    return base - prefix;
  }

  @Override
  public Integer getCommonPrefix(Integer a, Integer b) {
    return Math.min(a, b);
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
    return null;
  }
}
