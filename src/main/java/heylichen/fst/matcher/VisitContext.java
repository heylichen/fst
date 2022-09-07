package heylichen.fst.matcher;

import heylichen.fst.output.Output;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiConsumer;

@Setter
@Getter
class VisitContext<O> {
  private String partialKey;
  private Output<O> partialOutput;
  private BiConsumer<String, Output<O>> consumer;
  private Automaton automaton;
  private String prefix;

  public VisitContext(String partialKey, Output<O> partialOutput, BiConsumer<String, Output<O>> consumer, Automaton automaton, String prefix) {
    this.partialKey = partialKey;
    this.partialOutput = partialOutput;
    this.consumer = consumer;
    this.automaton = automaton;
    this.prefix = prefix;
  }

  public VisitContext copy() {
    return new VisitContext(partialKey, partialOutput,
        consumer, automaton, prefix);
  }

  public Automaton copyAutomaton() {
    return automaton.copy();
  }

  public void accept(String s, Output<O> output) {
    consumer.accept(s, output);
  }

  public static <O> VisitContextBuilder<O> builder() {
    return VisitContextBuilder.newInstance();
  }

  public static final class VisitContextBuilder<O> {
    private String partialKey;
    private Output<O> partialOutput;
    private BiConsumer<String, Output<O>> consumer;
    private Automaton automaton;
    private String prefix;

    private VisitContextBuilder() {
    }

    public static <O> VisitContextBuilder<O> newInstance() {
      return new VisitContextBuilder();
    }

    public VisitContextBuilder<O> withPartialKey(String partialKey) {
      this.partialKey = partialKey;
      return this;
    }

    public VisitContextBuilder<O> withPartialOutput(Output<O> partialOutput) {
      this.partialOutput = partialOutput;
      return this;
    }

    public VisitContextBuilder<O> withConsumer(BiConsumer<String, Output<O>> consumer) {
      this.consumer = consumer;
      return this;
    }

    public VisitContextBuilder<O> withAutomaton(Automaton automaton) {
      this.automaton = automaton;
      return this;
    }

    public VisitContextBuilder<O> withPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public VisitContext<O> build() {
      return new VisitContext<>(partialKey, partialOutput, consumer, automaton, prefix);
    }
  }
}
