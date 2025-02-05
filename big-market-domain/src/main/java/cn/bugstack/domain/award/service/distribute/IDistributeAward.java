package cn.bugstack.domain.award.service.distribute;

import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;

/**
 * 分发奖品接口
 */
public interface IDistributeAward {

  void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);
}
