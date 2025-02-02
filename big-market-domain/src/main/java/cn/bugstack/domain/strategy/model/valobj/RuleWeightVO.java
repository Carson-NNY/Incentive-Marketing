package cn.bugstack.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * weight 值的信息
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleWeightVO {

  private String ruleValue;

  private Integer weight;

  // 奖品配置
  private List<Integer> awardIds;

  private List<Award> awardList;

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Award {
    private Integer awardId;
    private String awardTitle;
  }
}
