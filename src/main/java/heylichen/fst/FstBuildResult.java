package heylichen.fst;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FstBuildResult {
  private ResultCode code;

  public FstBuildResult() {
  }

  public FstBuildResult(ResultCode code) {
    this.code = code;
  }

  public boolean isSuccess() {
    return ResultCode.SUCCESS == code;
  }

}
