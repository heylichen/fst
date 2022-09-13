package heylichen.fst.serialize;

import heylichen.fst.serialize.codec.VBCodec;

import java.io.IOException;
import java.io.OutputStream;

public class JumpTableWriter {
  // for check if it's a jump table tag
  public static final byte JUMP_TAG_CHECK_MASK = (byte) (0b1111_1100 & 0xFF);
  // for getting last 2 bits, indicate jump table element size: 1 or 2 bytes.
  public static final byte JUMP_ELE_SIZE_MASK = ~JUMP_TAG_CHECK_MASK;

  // jump table tag byte
  // invalid in UTF-8 encoding, indicate a jump table, differentiate from normal record with label
  public static final byte JUMP_ELE_ONE_BYTE = (byte) (0b1111_1101 & 0xFF);
  public static final byte JUMP_ELE_TWO_BYTE = (byte) (0b1111_1110 & 0xFF);
  // header byte for jump table, label index is 0.
  // to differentiate with normal record header byte, need to check another byte
  // before this header byte: jump table tag byte
  public static final byte JUMP_TABLE_HEAD = (byte) (0b0000_0111 & 0xFF);

  private final long[] jumpTable;
  private final int jumpTableElementSize;
  private final long lastAddress;

  public JumpTableWriter(long[] jumpTable, long accessibleAddress) {
    this.jumpTable = jumpTable;
    normalizeJumpTable(accessibleAddress);
    jumpTableElementSize = calculateElementSize(jumpTable);
    lastAddress = accessibleAddress;
  }

  private void normalizeJumpTable(long accessibleAddress) {
    for (int i = 0; i < jumpTable.length; i++) {
      jumpTable[i] = accessibleAddress - jumpTable[i];
    }
  }

  private int calculateElementSize(long[] jumpTable) {
    int eleSize = 1;
    for (int i = 0; i < jumpTable.length; i++) {
      if (jumpTable[i] > 0xFF) {
        eleSize = 2;
        break;
      }
    }
    return eleSize;
  }

  public int calculateTotalSize() {
    return 2 + VBCodec.getEncodedBytes(jumpTable.length) + jumpTable.length * jumpTableElementSize;
  }

  /**
   * write jump table, has 4 parts:
   * 1) elements
   * 2) length
   * 3) one byte jumpTableTag
   * 4) one byte header tag
   *
   * @param os
   * @throws IOException
   */
  public void write(OutputStream os) throws IOException {
    writeJumpTableElements(os);
    VBCodec.encodeReverse(jumpTable.length, os);
    //invalid in UTF-8 encoding, indicate a jump table, differentiate from normal record with label
    os.write(jumpTableElementSize > 1 ? JUMP_ELE_TWO_BYTE : JUMP_ELE_ONE_BYTE);
    os.write(JUMP_TABLE_HEAD);
  }

  public String dump(long stateId) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("stateId=%d jumpTable, elements format is index:\t delta(address)", stateId));

    dumpJumpTableElements(sb);

    sb.append("len=").append(jumpTable.length);
    sb.append("\ttag=").
        append(toBinary(jumpTableElementSize > 1 ? JUMP_ELE_TWO_BYTE : JUMP_ELE_ONE_BYTE))
        .append("\theader=").append(toBinary(JUMP_TABLE_HEAD)).append("\n\n");
    return sb.toString();
  }

  private String toBinary(byte tag) {
    return Integer.toBinaryString(tag & 0xFF).toUpperCase();
  }

  private void writeJumpTableElements(OutputStream os) throws IOException {
    int byteCount = jumpTableElementSize * jumpTable.length;
    boolean twoBytes = jumpTableElementSize == 2;
    byte[] bytes = new byte[byteCount];
    int byteOffset = 0;
    for (long l : jumpTable) {
      bytes[byteOffset++] = (byte) (l & 0xFF);
      if (twoBytes) {
        bytes[byteOffset++] = (byte) (l >>> 8 & 0xFF);
      }
    }
    os.write(bytes);
  }

  private void dumpJumpTableElements(StringBuilder sb) {
    int width = String.valueOf(lastAddress).length();
    int deltaWidth = String.valueOf(jumpTable[jumpTable.length - 1]).length();
    String format = "%" + deltaWidth + "d(" + "%" + width + "d)";

    for (int i = 0; i < jumpTable.length; i++) {
      if ((i ) % 8 == 0) {
        sb.append('\n').append(String.format("%d:\t", i ));
      }
      long delta = jumpTable[i];
      long address = lastAddress - delta;
      String ele = String.format(format, delta, address);
      sb.append(ele).append('\t');

    }
    sb.append('\n');
  }

}
