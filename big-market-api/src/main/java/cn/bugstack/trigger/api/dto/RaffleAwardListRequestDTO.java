package cn.bugstack.trigger.api.dto;

import lombok.Data;

/**
 * 抽奖奖品列表, request对象
 */
@Data
public class RaffleAwardListRequestDTO {

  @Deprecated
  private Long strategyId;

  //活动id
  private Long activityId;

  private String userId;
}
