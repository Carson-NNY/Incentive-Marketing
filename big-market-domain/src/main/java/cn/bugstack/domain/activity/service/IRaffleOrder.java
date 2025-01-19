package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivityShopCartEntity;

//Key Components:
//
//    "IRaffleOrder":
//    An interface that defines operations related to raffle orders. In DDD, this is likely a domain service since the logic does not belong to a specific entity. It operates on multiple entities or aggregates.
//    "RaffleActivityService":
//    This is an implementation of IRaffleOrder. It's annotated as a Spring component (like @Service), making it available for dependency injection. This handles the business logic tied to raffle activity orders.
//    "AbstractRaffleActivity":
//    A possible base class or shared implementation for raffle-related activities. It might define shared behaviors or enforce certain rules across multiple implementations.
public interface IRaffleOrder {

  /**
   * 以sku创建抽奖活动订单，获得参与抽奖资格（可消耗的次数）
   *
   * @param activityShopCartEntity 活动sku实体，通过sku领取活动。
   * @return 活动参与记录实体
   */
  ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

}
