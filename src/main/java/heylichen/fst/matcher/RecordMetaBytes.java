package heylichen.fst.matcher;

import heylichen.fst.serialize.FstRecordHeader;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordMetaBytes {
  private byte recordHeaderByte;
  //the byte just before recordHeaderByte
  private byte previousByte;

  private boolean hasJumpTable;
  private int jumpTableEleSize;
  // recordHeader will not be null only if no jump table
  private FstRecordHeader recordHeader;
}
