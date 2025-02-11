package cn.bugstack.domain.activity.repository;

import cn.bugstack.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityAccountDayEntity;
import cn.bugstack.domain.activity.model.entity.ActivityAccountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityAccountMonthEntity;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.entity.DeliveryOrderEntity;
import cn.bugstack.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.bugstack.domain.activity.model.entity.SkuProductEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;

import java.util.Date;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动仓储接口
 * @create 2024-03-16 10:31
 */
public interface IActivityRepository {

  ActivitySkuEntity queryActivitySku(Long sku);

  ActivityEntity queryRaffleActivityByActivityId(Long activityId);

  ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

  void doSaveNoPayOrder(CreateQuotaOrderAggregate createOrderAggregate);

  void doSaveCreditPayOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate);

  void cacheActivitySkuStockCount(String cacheKey, Integer stockCount);

  boolean substractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime);

  void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO build);

  ActivitySkuStockKeyVO takeQueueValue();

  void clearQueueValue();

  void updateActivitySkuStock(Long sku);

  void clearActivitySkuStock(Long sku);

  void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate);

  UserRaffleOrderEntity queryNoUseRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);

  ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId);

  ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month);

  ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day);

  List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId);

  Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId);

  ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId);

  Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);

  void updateOrder(DeliveryOrderEntity deliveryOrderEntity);

  UnpaidActivityOrderEntity queryUnpaidActivityOrder(SkuRechargeEntity skuRechargeEntity);

  List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);
}
