package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityAccountEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;

//Key Components:
//
//    "IRaffleActivityAccountQuotaService":
//    An interface that defines operations related to raffle orders. In DDD, this is likely a domain service since the logic does not belong to a specific entity. It operates on multiple entities or aggregates.
//    "RaffleActivityService":
//    This is an implementation of IRaffleOrder. It's annotated as a Spring component (like @Service), making it available for dependency injection. This handles the business logic tied to raffle activity orders.
//    "AbstractRaffleActivityPartake":
//    A possible base class or shared implementation for raffle-related activities. It might define shared behaviors or enforce certain rules across multiple implementations.
public interface IRaffleActivityAccountQuotaService {

  /**
   * 创建 sku 账户充值订单，给用户增加抽奖次数
   * <p>
   * 1. 在【打卡、签到、分享、对话、积分兑换】等行为动作下，创建出活动订单，给用户的活动账户【日、月】充值可用的抽奖次数。
   * 2. 对于用户可获得的抽奖次数，比如首次进来就有一次，则是依赖于运营配置的动作，在前端页面上。用户点击后，可以获得一次抽奖次数。
   *
   * @param skuRechargeEntity 活动商品充值实体对象
   * @return 活动ID
   */

  String createOrder(SkuRechargeEntity skuRechargeEntity);

  Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId);

  ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId);

  Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);
}
