package cn.bugstack.domain.rebate.model.aggregate;

import cn.bugstack.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.bugstack.domain.rebate.model.entity.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行为返利聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorRebateAggregate {

  private String userId;

  private BehaviorRebateOrderEntity behaviorRebateOrderEntity;

  private TaskEntity taskEntity;


}
