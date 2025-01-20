package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityAccountDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityCountDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityOrderDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.bugstack.infrastructure.persistent.po.RaffleActivity;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityAccount;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityCount;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityOrder;
import cn.bugstack.infrastructure.persistent.po.RaffleActivitySku;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

@Slf4j
@Repository
public class ActivityRepository implements IActivityRepository {

  @Resource
  private IRedisService redisService;
  @Resource
  private IRaffleActivityDao raffleActivityDao;
  @Resource
  private IRaffleActivitySkuDao raffleActivitySkuDao;
  @Resource
  private IRaffleActivityCountDao raffleActivityCountDao;
  @Resource
  private IRaffleActivityOrderDao raffleActivityOrderDao;
  @Resource
  private IRaffleActivityAccountDao raffleActivityAccountDao;

// 分库分表需要的:
  @Resource
  private TransactionTemplate transactionTemplate;
  @Resource
  private IDBRouterStrategy dbRouter;


  @Override
  public ActivitySkuEntity queryActivitySku(Long sku) {
    RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryActivitySku(sku);
    return ActivitySkuEntity.builder()
        .sku(raffleActivitySku.getSku())
        .activityId(raffleActivitySku.getActivityId())
        .activityCountId(raffleActivitySku.getActivityCountId())
        .stockCount(raffleActivitySku.getStockCount())
        .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
        .build();
  }

  @Override
  public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
    // 优先从缓存获取
    String cacheKey = Constants.RedisKey.ACTIVITY_KEY + activityId;
    ActivityEntity activityEntity = redisService.getValue(cacheKey);
    if (null != activityEntity) return activityEntity;
    // 从库中获取数据
    RaffleActivity raffleActivity = raffleActivityDao.queryRaffleActivityByActivityId(activityId);
    activityEntity = ActivityEntity.builder()
        .activityId(raffleActivity.getActivityId())
        .activityName(raffleActivity.getActivityName())
        .activityDesc(raffleActivity.getActivityDesc())
        .beginDateTime(raffleActivity.getBeginDateTime())
        .endDateTime(raffleActivity.getEndDateTime())
        .strategyId(raffleActivity.getStrategyId())
        .state(ActivityStateVO.valueOf(raffleActivity.getState()))
        .build();
    redisService.setValue(cacheKey, activityEntity);
    return activityEntity;
  }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
      // 优先从缓存获取
      String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + activityCountId;
      ActivityCountEntity activityCountEntity = redisService.getValue(cacheKey);
      if (null != activityCountEntity) return activityCountEntity;
      // 从库中获取数据
      RaffleActivityCount raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(activityCountId);
      activityCountEntity = ActivityCountEntity.builder()
          .activityCountId(raffleActivityCount.getActivityCountId())
          .totalCount(raffleActivityCount.getTotalCount())
          .dayCount(raffleActivityCount.getDayCount())
          .monthCount(raffleActivityCount.getMonthCount())
          .build();
      redisService.setValue(cacheKey, activityCountEntity);
      return activityCountEntity;
    }

  @Override
  public void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
    try {
      // 订单对象
      ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
      RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
      raffleActivityOrder.setSku(activityOrderEntity.getSku());
      raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
      raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
      raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
      raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
      raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
      raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
      raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
      raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
      raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
      raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
      raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

      // 账户对象
      RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
      raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
      raffleActivityAccount.setActivityId(createOrderAggregate.getActivityId());
      raffleActivityAccount.setTotalCount(createOrderAggregate.getTotalCount());
      raffleActivityAccount.setTotalCountSurplus(createOrderAggregate.getTotalCount());
      raffleActivityAccount.setDayCount(createOrderAggregate.getDayCount());
      raffleActivityAccount.setDayCountSurplus(createOrderAggregate.getDayCount());
      raffleActivityAccount.setMonthCount(createOrderAggregate.getMonthCount());
      raffleActivityAccount.setMonthCountSurplus(createOrderAggregate.getMonthCount());

      // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
      // 这个DBRouter就是用来分库分表的，key是分库分表的依据，这里是userId. whenever you want to insert a new record,
      // 我们都会根据userId来决定这个记录应该插入到哪个库的哪个表里面
      dbRouter.doRouter(createOrderAggregate.getUserId());

      // 编程式事务
      transactionTemplate.execute(status -> {
        try {
          // 写入订单
          raffleActivityOrderDao.insert(raffleActivityOrder);
          // 更新账户
          int count = raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
          // 如果count=0说明账号不存在，需要创建账号
          if (count == 0) {
            raffleActivityAccountDao.insert(raffleActivityAccount);
          }
          return 1;
        } catch (DuplicateKeyException e) {
          // 重复下单的异常处理 (重复下单可能是之前网络超时或者其他问题导致的订单已经被写入数据库但是未能返回成功信息给用户的情况)
          status.setRollbackOnly();
          log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
          throw new AppException(ResponseCode.INDEX_DUP.getCode());
        }
      });
    } finally {
      dbRouter.clear(); // in the end of the transaction, clear the router
    }
  }


}
