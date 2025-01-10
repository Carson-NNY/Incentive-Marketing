package cn.bugstack.domain.strategy.model.entity;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// 这里限制了泛型的类型来规范某些的规则才能被传进来
public class RuleActionEntity<T extends RuleActionEntity.RaffleEntity> {

  private String code = RuleLogicCheckTypeVO.ALLOW.getCode();
  private String info = RuleLogicCheckTypeVO.ALLOW.getInfo();

  private String ruleModel; // 过滤后得到的rule model

  private T data;

  // 静态内部类
  static public class RaffleEntity {


  }

  // 抽奖前
  @EqualsAndHashCode(callSuper = true)
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  static public class RaffleBeforeEntity extends RaffleEntity {
    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 权重值Key；用于抽奖时可以选择权重抽奖。为了这种使用:  strategyDispatch.getRandomAwardId(100001L, "4000:102,103,104,105")
     */
    private String ruleWeightValueKey;

    /**
     * 奖品ID； if黑名单, 直接返回awardId
     */
    private Integer awardId;

  }

  static public class RaffleCenterEntity extends RaffleEntity {


  }

  static public class RaffleAfterEntity extends RaffleEntity {


  }

}
