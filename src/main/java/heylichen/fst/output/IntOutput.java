package heylichen.fst.output;

import heylichen.fst.matcher.RandomAccessInput;
import heylichen.fst.serialize.codec.LenInt;
import heylichen.fst.serialize.codec.VBCodec;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class IntOutput implements Output<Integer>, OutputFactory<Integer> {
  private Integer data;

  public IntOutput(Integer data) {
    this.data = data;
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
    if (other == null) {
      return new IntOutput(0);
    }
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

  @Override
  public Integer getData() {
    return data;
  }

  @Override
  public Pair<Output<Integer>, Integer> readByteValue(RandomAccessInput input, long offset) {
    LenInt lenInt = VBCodec.decodeReverse(input, offset);
    Output<Integer> out = new IntOutput(lenInt.getValue());
    return Pair.of(out, lenInt.getLen());
  }

  @Override
  public void append(Output<Integer> v) {
    if (v != null) {
      data += v.getData();
    }
  }

  @Override
  public Output<Integer> appendCopy(Output<Integer> v) {
    Integer vd = v == null ? 0 : v.getData();
    return new IntOutput(data + vd);
  }

  @Override
  public void setData(Integer o) {
    data = o;
  }

  @Override
  public Output<Integer> newInstance() {
    return new IntOutput(0);
  }

  @Override
  public OutputType getOutputType() {
    return OutputType.INT;
  }

  @Override
  public Output<Integer> copy() {
    return new IntOutput(this.data);
  }

  @Override
  public String toString() {
    return data.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IntOutput intOutput = (IntOutput) o;
    return Objects.equals(data, intOutput.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }
}
