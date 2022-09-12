package heylichen.fst.output;

public final class Outputs {
  private Outputs() {
  }

  public static <O> boolean empty(Output<O> out) {
    return out == null || out.empty();
  }
}
