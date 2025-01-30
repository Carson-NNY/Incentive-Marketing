package cn.bugstack.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// 奖品因子, 把抽奖要考虑的因子抽象成一个实体
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleFactorEntity {
  private String userId;
  private long strategyId;
  private Date endDateTime;
}
