package heylichen.fst.serialize;

import heylichen.fst.CharTransition;
import heylichen.fst.State;
import heylichen.fst.Transition;
import heylichen.fst.Transitions;
import heylichen.fst.output.Output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * generate graphviz dot dsl to draw FST diagram
 *
 * @param <O>
 */
public class FstDotWriter<O> implements FstWriter<O> {
  private final OutputStream os;

  public FstDotWriter(OutputStream os) throws IOException {
    this.os = os;
    init();
  }

  private void init() throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("digraph{\n")
        .append("  rankdir = LR;\n");
    os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void write(State<O> state, char previousArc) throws IOException {
    StringBuilder sb = new StringBuilder();
    if (state.isFinalState()) {
      Output<O> stateOutput = state.getStateOutput();
      String stateOutputStr = stateOutput == null ? "" : stateOutput.toString();
      sb.append(" s").append(state.getId()).append("  [ shape = doublecircle, xlabel = \"")
          .append(stateOutputStr).append("\" ];\n");

    } else {
      sb.append(" s").append(state.getId()).append(" [ shape = circle ];\n");
    }

    Transitions<O> transitions = state.getTransitions();
    if (transitions != null && transitions.getCharTransitions() != null) {
      for (CharTransition<O> charTransition : transitions.getCharTransitions()) {
        Transition<O> t = charTransition.getTransition();
        sb.append("  s").append(state.getId())
            .append("->s").append(t.getId()).append(" [ label = \"")
            .append(charTransition.getCh());
        if (t.getOutput() != null && !t.getOutput().empty()) {
          sb.append(" (").append(t.getOutput().toString()).append(")");
        }
        sb.append("\" fontcolor = red ];\n");
      }
    }
    os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void close() throws IOException {
    os.write("}\n".getBytes(StandardCharsets.UTF_8));
  }
}
