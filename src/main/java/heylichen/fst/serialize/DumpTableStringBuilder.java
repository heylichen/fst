package heylichen.fst.serialize;

import heylichen.fst.output.Output;

public class DumpTableStringBuilder {
  private final boolean needOutput;
  private final StringBuilder sb;

  private static final char NEW_LINE = '\n';
  private static final char TAB = '\t';
  private static final char SPACE = ' ';

  public DumpTableStringBuilder(boolean needOutput) {
    this.needOutput = needOutput;
    sb = new StringBuilder();
  }

  public String getTableHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("stateToState\tAddress \tArc#idx\tN F L\tNextAddr");
    if (needOutput) {
      sb.append("\tOutput\tStOuts");
    }
    sb.append("\tSize(jt)\n");

    sb.append("------------\t--------\t-------\t-----\t--------");
    if (needOutput) {
      sb.append("\t------\t------");
    }
    sb.append("\t--------\n");
    return sb.toString();
  }

  public void appendStateId(long stateId, long toStateId) {
    sb.append(String.format("%5d->%-5d", stateId, toStateId)).append(TAB);
  }

  public void appendAddress(Long addr) {
    sb.append(String.format("%-8d", addr))
        .append(TAB);
  }

  public void appendArc(char label, int labelIndex) {
    sb.append(String.format("'%c' #%-2d", label,labelIndex)).append(TAB);
  }

  public void appendNFL(boolean noAddress, boolean isFinalTrans, boolean isLastTrans) {
    sb.append(noAddress ? "↑" : SPACE).append(SPACE)
        .append(isFinalTrans ? "F" : SPACE).append(SPACE)
        .append(isLastTrans ? "L" : SPACE)
        .append(TAB);
  }

  public void appendNextAddr(boolean hasAddr, long nextAddr) {
    if (hasAddr) {
      String na = nextAddr > 0 ? String.valueOf(nextAddr) : "x";
      sb.append(String.format("%-8s", na));
    } else {
      sb.append(String.format("%-8s", SPACE));
    }
  }

  public <O> void appendOut(Output<O> out, Output<O> stateOut) {
    appendOut(out);
    appendOut(stateOut);
  }

  private <O> void appendOut(Output<O> out) {
    if (!needOutput) {
      return;
    }
    sb.append(TAB);
    if (out != null && !out.empty()) {
      sb.append(String.format("%-6s", out));
    } else {
      sb.append(String.format("%-6s", SPACE));
    }
  }

  public void appendByteSize(int size, int jumpTableSize) {
    sb.append(TAB).append(size);
    if (jumpTableSize > 0) {
      sb.append(" (").append(jumpTableSize).append(")");
    }
    sb.append(NEW_LINE);
  }

  public String buildTransitionRecord() {
    String str = sb.toString();
    sb.delete(0, sb.length());
    return str;
  }
}
