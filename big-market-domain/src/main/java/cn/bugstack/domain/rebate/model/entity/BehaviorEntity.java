package cn.bugstack.domain.rebate.model.entity;

import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行为实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorEntity {

  private String userId;

  private BehaviorTypeVO behaviorTypeVO;

  // 业务ID, 保证业务的唯一性
  private String outBusinessNo;
}
