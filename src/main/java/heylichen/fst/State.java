package heylichen.fst;

import heylichen.fst.output.Output;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.MurmurHash2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

@Slf4j
@Getter
@Setter
public class State<O> {
  private long id;
  private boolean finalState;
  private Transitions<O> transitions;
  private Output<O> stateOutput;

  public State(long id) {
    transitions = new Transitions<>();
    this.id = id;
  }

  public Output<O> getOutput(char arc) {
    return transitions.getOutput(arc);
  }

  public void setTransition(char arc, State<O> state) {
    transitions.setTransition(arc, state);
  }

  public void setOutput(char arc, Output<O> output) {
    transitions.setOutput(arc, output);
  }

  public void prependSuffixToOutput(char arc, Output<O> suffix) {
    transitions.prependOutput(arc, suffix);
  }

  public void prependSuffixToStateOutput(Output<O> suffix) {
    if (stateOutput == null) {
      stateOutput = suffix.copy();
      return;
    }
    stateOutput.prepend(suffix);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof State)) {
      return false;
    }
    State s = (State) obj;
    if (this == s) {
      return true;
    }
    return finalState == s.finalState && Objects.equals(stateOutput, s.stateOutput) &&
        Objects.equals(transitions, s.transitions);
  }

  public void reuse(long stateId) {
    id = stateId;
    finalState = false;
    transitions.clear();
  }

  public BigInteger hash() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    transitions.foreach((Character ch, Transition<O> t) -> {
      try {
        dos.writeChar(ch);
        dos.writeLong(t.getId());
        Output<O> out = t.getOutput();
        if (out != null && !out.empty()) {
          out.writeByteValue(dos);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    if (isFinalState() && stateOutput != null && !stateOutput.empty()) {
      try {
        stateOutput.writeByteValue(dos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    byte[] bytes = bos.toByteArray();
    long v = MurmurHash2.hash64(bytes, bytes.length, 0);
    return toUnsignedBigInteger(v);
  }

  private static BigInteger toUnsignedBigInteger(long i) {
    if (i >= 0L) {
      return BigInteger.valueOf(i);
    } else {
      int upper = (int) (i >>> 32);
      int lower = (int) i;
      // return (upper << 32) + lower
      return BigInteger.valueOf(Integer.toUnsignedLong(upper))
          .shiftLeft(32)
          .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }
  }
}
