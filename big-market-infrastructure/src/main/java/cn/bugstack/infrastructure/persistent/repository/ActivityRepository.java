package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.activity.event.ActivitySkuStockZeroMessageEvent;
import cn.bugstack.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityAccountDayEntity;
import cn.bugstack.domain.activity.model.entity.ActivityAccountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityAccountMonthEntity;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.entity.DeliveryOrderEntity;
import cn.bugstack.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.bugstack.domain.activity.model.entity.SkuProductEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.model.valobj.UserRaffleOrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityAccountDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityAccountDayDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityAccountMonthDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityCountDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivityOrderDao;
import cn.bugstack.infrastructure.persistent.dao.IRaffleActivitySkuDao;
import cn.bugstack.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.bugstack.infrastructure.persistent.dao.IUserRaffleOrderDao;
import cn.bugstack.infrastructure.persistent.po.RaffleActivity;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityAccount;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityAccountDay;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityAccountMonth;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityCount;
import cn.bugstack.infrastructure.persistent.po.RaffleActivityOrder;
import cn.bugstack.infrastructure.persistent.po.RaffleActivitySku;
import cn.bugstack.infrastructure.persistent.po.UserCreditAccount;
import cn.bugstack.infrastructure.persistent.po.UserRaffleOrder;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
  @Resource
  private IRaffleActivityAccountMonthDao raffleActivityAccountMonthDao;
  @Resource
  private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;
  @Resource
  private IUserRaffleOrderDao userRaffleOrderDao;
  @Resource
  private IUserCreditAccountDao userCreditAccountDao;

// 分库分表需要的:
  @Resource
  private TransactionTemplate transactionTemplate;
  @Resource
  private IDBRouterStrategy dbRouter;

  @Resource
  private EventPublisher eventPublisher;
  @Resource
  private ActivitySkuStockZeroMessageEvent activitySkuStockZeroMessageEvent;

  @Override
  public ActivitySkuEntity queryActivitySku(Long sku) {
    RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryActivitySku(sku);
    return ActivitySkuEntity.builder()
        .sku(raffleActivitySku.getSku())
        .activityId(raffleActivitySku.getActivityId())
        .activityCountId(raffleActivitySku.getActivityCountId())
        .stockCount(raffleActivitySku.getStockCount())
        .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
        .productAmount(raffleActivitySku.getProductAmount())
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
  public void doSaveNoPayOrder(CreateQuotaOrderAggregate createOrderAggregate) {
    // 这里加锁的意义在于在高并发情况下, 有可能下面的操作会因为我们的系统设计(异步消息+多线程处理)导致两个异步消息被不同的机器同时消费:
    // 例如: 一个用户 分别进行了签到, 然后同时又支付去购买更多次数. 这些操作的底层数据库更新不会立即执行因为我们使用了异步消息.
    // 由于签到和make payment是两个不同的服务, 所以存在可能性这两个异步消息同时到这里, 而且打比方 这个用户是新的用户, 数据库中没有他的 raffleActivityAccount.
    // 这种情况下, 由于我们使用了异步消息, 两个消息同时到这里, 会导致两个线程同时尝试创建账户, 但是由于数据库的唯一索引, 会导致其中一个线程失败抛异常.
    // 我们加锁的目的就是为了避免这种情况, 保证同一个用户的操作是串行的, 也就是说, 一个用户的操作必须等到上一个操作完成, 这样就避免的唯一索引异常
    RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK + createOrderAggregate.getUserId() + Constants.UNDERLINE + createOrderAggregate.getActivityId());

    try {
      lock.lock(3, TimeUnit.SECONDS);
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
      raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());
      raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
      raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

      // 账户对象 - 总账户
      RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
      raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
      raffleActivityAccount.setActivityId(createOrderAggregate.getActivityId());
      raffleActivityAccount.setTotalCount(createOrderAggregate.getTotalCount());
      raffleActivityAccount.setTotalCountSurplus(createOrderAggregate.getTotalCount());
      raffleActivityAccount.setDayCount(createOrderAggregate.getDayCount());
      raffleActivityAccount.setDayCountSurplus(createOrderAggregate.getDayCount());
      raffleActivityAccount.setMonthCount(createOrderAggregate.getMonthCount());
      raffleActivityAccount.setMonthCountSurplus(createOrderAggregate.getMonthCount());


      // 账户对象 - 月
      RaffleActivityAccountMonth raffleActivityAccountMonth = new RaffleActivityAccountMonth();
      raffleActivityAccountMonth.setUserId(createOrderAggregate.getUserId());
      raffleActivityAccountMonth.setActivityId(createOrderAggregate.getActivityId());
      raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonth.currentMonth());
      raffleActivityAccountMonth.setMonthCount(createOrderAggregate.getMonthCount());
      raffleActivityAccountMonth.setMonthCountSurplus(createOrderAggregate.getMonthCount());

      // 账户对象 - 日
      RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
      raffleActivityAccountDay.setUserId(createOrderAggregate.getUserId());
      raffleActivityAccountDay.setActivityId(createOrderAggregate.getActivityId());
      raffleActivityAccountDay.setDay(RaffleActivityAccountDay.currentDay());
      raffleActivityAccountDay.setDayCount(createOrderAggregate.getDayCount());
      raffleActivityAccountDay.setDayCountSurplus(createOrderAggregate.getDayCount());

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
          RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount);
          if (null == raffleActivityAccountRes) {
            raffleActivityAccountDao.insert(raffleActivityAccount);
          } else {
            raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
          }

          // 更新月账户
          raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);

          // 更新日账户
          raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);

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
      lock.unlock();
    }
  }

  @Override
  public void doSaveCreditPayOrder(CreateQuotaOrderAggregate createOrderAggregate) {
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
      raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());
      raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
      raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

      // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
      dbRouter.doRouter(createOrderAggregate.getUserId());

      // 编程式事务
      transactionTemplate.execute(status -> {
        try {
          // 只插入订单, 不更新账户因为我们需要等payment
          raffleActivityOrderDao.insert(raffleActivityOrder);
          return 1;
        } catch (DuplicateKeyException e) {
          status.setRollbackOnly();
          log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
          throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
        }
      });
    } finally {
      dbRouter.clear();
    }
  }

  @Override
  public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
    if (redisService.isExists(cacheKey)) return;
    redisService.setAtomicLong(cacheKey, stockCount);
  }

  @Override
  public boolean substractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime) {
    Object value = redisService.getValue(cacheKey);
    long surplus = redisService.decr(cacheKey);
    if (surplus == 0) {
      // 库存消耗没了后 发送MQ消息, 更新数据库库存
      eventPublisher.publish(activitySkuStockZeroMessageEvent.topic(), activitySkuStockZeroMessageEvent.buildEventMessage(sku));
      return false;
    } else if (surplus < 0) {
      redisService.setAtomicLong(cacheKey, 0);
      return false;
    }

    String lockKey = cacheKey + Constants.UNDERLINE + surplus;
    //expireMillis: The expiration time for the lock, ensuring it will automatically release after the specified duration
    long expireMillis = endDateTime.getTime() - System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
    Boolean lock = redisService.setNx(lockKey, expireMillis, TimeUnit.MICROSECONDS);
    if (!lock) {
      log.info("活动sku库存加锁失败 {}", lockKey);
    }
    return lock;
  }

  @Override
  public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
    String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUEUE_KEY;
    RBlockingQueue<ActivitySkuStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
    RDelayedQueue<ActivitySkuStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
    //The delayedQueue.offer(activitySkuStockKeyVO, 3, TimeUnit.SECONDS) method is part of your logic to manage task execution timing within a distributed system. Specifically:
    // After the 3-second delay, the item is moved from the delayed queue into the blocking queue.
    //    This is useful for scenarios where you want to:
    //        Reduce immediate contention or load on downstream resources (e.g., database updates).
    //        Avoid excessive updates in a short period of time, smoothing out the workload.
    //
    //The delayed queue ensures that tasks are queued for processing after a specific delay, which adds a layer of control over the timing of task availability.
    delayedQueue.offer(activitySkuStockKeyVO,3, TimeUnit.SECONDS);
  }

  @Override
  public ActivitySkuStockKeyVO takeQueueValue() {
    String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUEUE_KEY;
    RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
    return destinationQueue.poll();
  }

  @Override
  public void clearQueueValue() {
    String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUEUE_KEY;
    RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
    destinationQueue.clear();
  }

  @Override
  public void updateActivitySkuStock(Long sku) {
    raffleActivitySkuDao.updateActivitySkuStock(sku);
  }

  @Override
  public void clearActivitySkuStock(Long sku) {
    raffleActivitySkuDao.clearActivitySkuStock(sku);
  }

  @Override
  public void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate) {
    try {
      String userId = createPartakeOrderAggregate.getUserId();
      Long activityId = createPartakeOrderAggregate.getActivityId();
      ActivityAccountEntity activityAccountEntity = createPartakeOrderAggregate.getActivityAccountEntity();
      ActivityAccountMonthEntity activityAccountMonthEntity = createPartakeOrderAggregate.getActivityAccountMonthEntity();
      ActivityAccountDayEntity activityAccountDayEntity = createPartakeOrderAggregate.getActivityAccountDayEntity();
      UserRaffleOrderEntity userRaffleOrderEntity = createPartakeOrderAggregate.getUserRaffleOrderEntity();

      // 统一切换路由，以下事务内的所有操作，都走一个路由
      dbRouter.doRouter(userId);
      transactionTemplate.execute(status -> {
       try{
         // 1. 更新总账户
         int totalCount = raffleActivityAccountDao.updateActivityAccountSubtractionQuota(
             RaffleActivityAccount.builder()
                 .userId(userId)
                 .activityId(activityId)
                 .build());
          if (totalCount != 1) {
            status.setRollbackOnly();
            log.warn("写入创建参与活动记录，更新总账户额度不足，异常 userId: {} activityId: {}", userId, activityId);
            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
          }

          // 2. 更新月额度
         if (createPartakeOrderAggregate.isExistAccountMonth()) {
           int updateMonthCount = raffleActivityAccountMonthDao.updateActivityAccountMonthSubtractionQuota(
               RaffleActivityAccountMonth.builder()
                   .userId(userId)
                   .activityId(activityId)
                   .month(activityAccountMonthEntity.getMonth())
                   .build());
            if (updateMonthCount != 1) {
              // 如果更新失败，说明月账户额度不足
              status.setRollbackOnly();
              log.warn("写入创建参与活动记录，更新月账户额度不足，异常 userId: {} activityId: {}", userId, activityId);
              throw new AppException(ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getInfo());
            }
         } else {
           raffleActivityAccountMonthDao.insertActivityAccountMonth(RaffleActivityAccountMonth.builder()
               .userId(activityAccountMonthEntity.getUserId())
               .activityId(activityAccountMonthEntity.getActivityId())
               .month(activityAccountMonthEntity.getMonth())
               .monthCount(activityAccountMonthEntity.getMonthCount())
               .monthCountSurplus(activityAccountMonthEntity.getMonthCountSurplus() - 1)
               .build());
           // 新创建月账户，则更新总账表中月镜像额度
           raffleActivityAccountDao.updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccount.builder()
               .userId(userId)
               .activityId(activityId)
               .monthCountSurplus(activityAccountEntity.getMonthCountSurplus())
               .build());
         }

         // 3. 创建或更新日账户，true - 存在则更新，false - 不存在则插入
         if (createPartakeOrderAggregate.isExistAccountDay()) {
           int updateDayCount = raffleActivityAccountDayDao.updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDay.builder()
               .userId(userId)
               .activityId(activityId)
               .day(activityAccountDayEntity.getDay())
               .build());
           if (1 != updateDayCount) {
             // 未更新成功则回滚
             status.setRollbackOnly();
             log.warn("写入创建参与活动记录，更新日账户额度不足，异常 userId: {} activityId: {} day: {}", userId, activityId, activityAccountDayEntity.getDay());
             throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getInfo());
           }
         } else {
           raffleActivityAccountDayDao.insertActivityAccountDay(RaffleActivityAccountDay.builder()
               .userId(activityAccountDayEntity.getUserId())
               .activityId(activityAccountDayEntity.getActivityId())
               .day(activityAccountDayEntity.getDay())
               .dayCount(activityAccountDayEntity.getDayCount())
               .dayCountSurplus(activityAccountDayEntity.getDayCountSurplus() - 1)
               .build());
           // 新创建日账户，则更新总账表中日镜像额度
           raffleActivityAccountDao.updateActivityAccountDaySurplusImageQuota(RaffleActivityAccount.builder()
               .userId(userId)
               .activityId(activityId)
               .dayCountSurplus(activityAccountEntity.getDayCountSurplus())
               .build());
         }

         // 4. 写入参与活动订单
         userRaffleOrderDao.insert(UserRaffleOrder.builder()
             .userId(userRaffleOrderEntity.getUserId())
             .activityId(userRaffleOrderEntity.getActivityId())
             .activityName(userRaffleOrderEntity.getActivityName())
             .strategyId(userRaffleOrderEntity.getStrategyId())
             .orderId(userRaffleOrderEntity.getOrderId())
             .orderTime(userRaffleOrderEntity.getOrderTime())
             .orderState(userRaffleOrderEntity.getOrderState().getCode())
             .build());
         return 1;
       } catch (DuplicateKeyException e) { // if我们已经有了这个订单，就不需要再创建了
         status.setRollbackOnly();
         log.error("写入创建参与活动记录，唯一索引冲突 userId: {} activityId: {}", userId, activityId, e);
         throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
        }
      });
    } finally {
      dbRouter.clear();
    }
  }

  @Override
  public UserRaffleOrderEntity queryNoUseRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
    UserRaffleOrder userRaffleOrder = new UserRaffleOrder();
    userRaffleOrder.setUserId(partakeRaffleActivityEntity.getUserId());
    userRaffleOrder.setActivityId(partakeRaffleActivityEntity.getActivityId());
    UserRaffleOrder userRaffleOrderRes = userRaffleOrderDao.queryNoUsedRaffleOrder(userRaffleOrder);
    if (null == userRaffleOrderRes) return null;

    //封装返回值
    UserRaffleOrderEntity userRaffleOrderEntity = new UserRaffleOrderEntity();
    userRaffleOrderEntity.setUserId(userRaffleOrderRes.getUserId());
    userRaffleOrderEntity.setActivityId(userRaffleOrderRes.getActivityId());
    userRaffleOrderEntity.setActivityName(userRaffleOrderRes.getActivityName());
    userRaffleOrderEntity.setStrategyId(userRaffleOrderRes.getStrategyId());
    userRaffleOrderEntity.setOrderId(userRaffleOrderRes.getOrderId());
    userRaffleOrderEntity.setOrderTime(userRaffleOrderRes.getOrderTime());
    userRaffleOrderEntity.setOrderState(UserRaffleOrderStateVO.valueOf(userRaffleOrderRes.getOrderState()));
    return userRaffleOrderEntity;
  }

  @Override
  public ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId) {
    // 1. 查询账户
    RaffleActivityAccount raffleActivityAccountReq = new RaffleActivityAccount();
    raffleActivityAccountReq.setUserId(userId);
    raffleActivityAccountReq.setActivityId(activityId);
    RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserId(raffleActivityAccountReq);
    if (null == raffleActivityAccountRes) return null;
    // 2. 转换对象
    return ActivityAccountEntity.builder()
        .userId(raffleActivityAccountRes.getUserId())
        .activityId(raffleActivityAccountRes.getActivityId())
        .totalCount(raffleActivityAccountRes.getTotalCount())
        .totalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus())
        .dayCount(raffleActivityAccountRes.getDayCount())
        .dayCountSurplus(raffleActivityAccountRes.getDayCountSurplus())
        .monthCount(raffleActivityAccountRes.getMonthCount())
        .monthCountSurplus(raffleActivityAccountRes.getMonthCountSurplus())
        .build();
  }

  @Override
  public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month) {
    // 1. 查询账户
    RaffleActivityAccountMonth raffleActivityAccountMonthReq = new RaffleActivityAccountMonth();
    raffleActivityAccountMonthReq.setUserId(userId);
    raffleActivityAccountMonthReq.setActivityId(activityId);
    raffleActivityAccountMonthReq.setMonth(month);
    RaffleActivityAccountMonth raffleActivityAccountMonthRes = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthReq);
    if (null == raffleActivityAccountMonthRes) return null;
    // 2. 转换对象
    return ActivityAccountMonthEntity.builder()
        .userId(raffleActivityAccountMonthRes.getUserId())
        .activityId(raffleActivityAccountMonthRes.getActivityId())
        .month(raffleActivityAccountMonthRes.getMonth())
        .monthCount(raffleActivityAccountMonthRes.getMonthCount())
        .monthCountSurplus(raffleActivityAccountMonthRes.getMonthCountSurplus())
        .build();
  }

  @Override
  public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day) {
    // 1. 查询账户
    RaffleActivityAccountDay raffleActivityAccountDayReq = new RaffleActivityAccountDay();
    raffleActivityAccountDayReq.setUserId(userId);
    raffleActivityAccountDayReq.setActivityId(activityId);
    raffleActivityAccountDayReq.setDay(day);
    RaffleActivityAccountDay raffleActivityAccountDayRes = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);
    if (null == raffleActivityAccountDayRes) return null;
    // 2. 转换对象
    return ActivityAccountDayEntity.builder()
        .userId(raffleActivityAccountDayRes.getUserId())
        .activityId(raffleActivityAccountDayRes.getActivityId())
        .day(raffleActivityAccountDayRes.getDay())
        .dayCount(raffleActivityAccountDayRes.getDayCount())
        .dayCountSurplus(raffleActivityAccountDayRes.getDayCountSurplus())
        .build();
  }

  @Override
  public List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId) {
    List<RaffleActivitySku> raffleActivitySkus = raffleActivitySkuDao.queryActivitySkuListByActivityId(activityId);
    List<ActivitySkuEntity> activitySkuEntities = new ArrayList<>(raffleActivitySkus.size());
    for (RaffleActivitySku raffleActivitySku:raffleActivitySkus){
      ActivitySkuEntity activitySkuEntity = new ActivitySkuEntity();
      activitySkuEntity.setSku(raffleActivitySku.getSku());
      activitySkuEntity.setActivityCountId(raffleActivitySku.getActivityCountId());
      activitySkuEntity.setStockCount(raffleActivitySku.getStockCount());
      activitySkuEntity.setStockCountSurplus(raffleActivitySku.getStockCountSurplus());
      activitySkuEntities.add(activitySkuEntity);
    }
    return activitySkuEntities;
  }

  @Override
  public Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId) {
    RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
    raffleActivityAccountDay.setUserId(userId);
    raffleActivityAccountDay.setActivityId(activityId);
    raffleActivityAccountDay.setDay(raffleActivityAccountDay.currentDay());
    Integer dayPartakeCount = raffleActivityAccountDayDao.queryRaffleActivityAccountDayPartakeCount(raffleActivityAccountDay);
    return null == dayPartakeCount ? 0 : dayPartakeCount;
  }

  @Override
  public ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId) {
    RaffleActivityAccount raffleActivityAccount = raffleActivityAccountDao.queryActivityAccountByUserId(RaffleActivityAccount.builder()
        .userId(userId)
        .activityId(activityId)
        .build());

    // 如果用户没参与过活动(没领取过活动次数), 则返回一个默认的账户对象
    if (null == raffleActivityAccount) {
      return ActivityAccountEntity.builder()
          .activityId(activityId)
          .userId(userId)
          .totalCount(0)
          .totalCountSurplus(0)
          .monthCount(0)
          .monthCountSurplus(0)
          .dayCount(0)
          .dayCountSurplus(0)
          .build();
    }

    RaffleActivityAccountMonth raffleActivityAccountMonth = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(RaffleActivityAccountMonth.builder()
        .userId(userId)
        .activityId(activityId)
        .build());

    RaffleActivityAccountDay raffleActivityAccountDay = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(RaffleActivityAccountDay.builder()
        .userId(userId)
        .activityId(activityId)
        .build());

    ActivityAccountEntity activityAccountEntity = new ActivityAccountEntity();
    activityAccountEntity.setUserId(userId);
    activityAccountEntity.setActivityId(activityId);
    activityAccountEntity.setTotalCount(raffleActivityAccount.getTotalCount());
    activityAccountEntity.setTotalCountSurplus(raffleActivityAccount.getTotalCountSurplus());

    if (null == raffleActivityAccountDay) {
      activityAccountEntity.setDayCount(raffleActivityAccount.getDayCount());
      activityAccountEntity.setDayCountSurplus(raffleActivityAccount.getDayCount());
    } else { // 如果不为空, 就直接设置数据库中的值
      activityAccountEntity.setDayCount(raffleActivityAccountDay.getDayCount());
      activityAccountEntity.setDayCountSurplus(raffleActivityAccountDay.getDayCountSurplus());
    }

    if (null == raffleActivityAccountMonth) {
      activityAccountEntity.setMonthCount(raffleActivityAccount.getMonthCount());
      activityAccountEntity.setMonthCountSurplus(raffleActivityAccount.getMonthCount());
    } else {
      activityAccountEntity.setMonthCount(raffleActivityAccountMonth.getMonthCount());
      activityAccountEntity.setMonthCountSurplus(raffleActivityAccountMonth.getMonthCountSurplus());
    }

    return activityAccountEntity;
  }

  @Override
  public Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId) {
    RaffleActivityAccount raffleActivityAccount = raffleActivityAccountDao.queryActivityAccountByUserId(RaffleActivityAccount.builder()
        .userId(userId)
        .activityId(activityId)
        .build());
    return raffleActivityAccount.getTotalCount() - raffleActivityAccount.getTotalCountSurplus();
  }

  @Override
  public void updateOrder(DeliveryOrderEntity deliveryOrderEntity) {
    RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_UPDATE_LOCK + deliveryOrderEntity.getUserId());
    try{
      lock.lock(3, TimeUnit.SECONDS);
    // 通过userId + OutBusinessNo 来查询出 raffle_activity_order中的未支付订单
      RaffleActivityOrder raffleActivityOrderReq = new RaffleActivityOrder();
      raffleActivityOrderReq.setUserId(deliveryOrderEntity.getUserId());
      raffleActivityOrderReq.setOutBusinessNo(deliveryOrderEntity.getOutBusinessNo());
      RaffleActivityOrder raffleActivityOrderRes = raffleActivityOrderDao.queryRaffleActivityOrder(raffleActivityOrderReq);

      if (null == raffleActivityOrderRes) {
        if(lock.isLocked()) lock.unlock();
        return;
      }

      // 账户对象 - 总
      RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
      raffleActivityAccount.setUserId(raffleActivityOrderRes.getUserId());
      raffleActivityAccount.setActivityId(raffleActivityOrderRes.getActivityId());
      raffleActivityAccount.setTotalCount(raffleActivityOrderRes.getTotalCount());
      raffleActivityAccount.setTotalCountSurplus(raffleActivityOrderRes.getTotalCount());
      raffleActivityAccount.setDayCount(raffleActivityOrderRes.getDayCount());
      raffleActivityAccount.setDayCountSurplus(raffleActivityOrderRes.getDayCount());
      raffleActivityAccount.setMonthCount(raffleActivityOrderRes.getMonthCount());
      raffleActivityAccount.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

      // 账户对象 - 月
      RaffleActivityAccountMonth raffleActivityAccountMonth = new RaffleActivityAccountMonth();
      raffleActivityAccountMonth.setUserId(raffleActivityOrderRes.getUserId());
      raffleActivityAccountMonth.setActivityId(raffleActivityOrderRes.getActivityId());
      raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonth.currentMonth());
      raffleActivityAccountMonth.setMonthCount(raffleActivityOrderRes.getMonthCount());
      raffleActivityAccountMonth.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

      // 账户对象 - 日
      RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
      raffleActivityAccountDay.setUserId(raffleActivityOrderRes.getUserId());
      raffleActivityAccountDay.setActivityId(raffleActivityOrderRes.getActivityId());
      raffleActivityAccountDay.setDay(RaffleActivityAccountDay.currentDay());
      raffleActivityAccountDay.setDayCount(raffleActivityOrderRes.getDayCount());
      raffleActivityAccountDay.setDayCountSurplus(raffleActivityOrderRes.getDayCount());

      dbRouter.doRouter(deliveryOrderEntity.getUserId());

      transactionTemplate.execute(status ->{
        try{
          int updateCount = raffleActivityOrderDao.updateOrderCompleted(raffleActivityOrderReq);;
          if (updateCount != 1) { // 防止重复增加额度
            status.setRollbackOnly();
            return 1;
          }
          // 2. 更新账户 - 总
          RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount);
          if (null == raffleActivityAccountRes) {
            raffleActivityAccountDao.insert(raffleActivityAccount);
          } else {
            raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
          }
          // 4. 更新账户 - 月
          raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);
          // 5. 更新账户 - 日
          raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);
          return 1;
        } catch (DuplicateKeyException e) {
          status.setRollbackOnly();
          log.error("更新订单记录，完成态，唯一索引冲突 userId: {} outBusinessNo: {}", deliveryOrderEntity.getUserId(), deliveryOrderEntity.getOutBusinessNo(), e);
          throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
        }
      });
    } finally {
      dbRouter.clear();
      lock.unlock();
    }

  }

  @Override
  public UnpaidActivityOrderEntity queryUnpaidActivityOrder(SkuRechargeEntity skuRechargeEntity) {
    RaffleActivityOrder raffleActivityOrderReq = new RaffleActivityOrder();
    raffleActivityOrderReq.setUserId(skuRechargeEntity.getUserId());
    raffleActivityOrderReq.setSku(skuRechargeEntity.getSku());
    RaffleActivityOrder raffleActivityOrderRes = raffleActivityOrderDao.queryUnpaidActivityOrder(raffleActivityOrderReq);
    if (null == raffleActivityOrderRes) return null;
    return UnpaidActivityOrderEntity.builder()
        .userId(raffleActivityOrderRes.getUserId())
        .orderId(raffleActivityOrderRes.getOrderId())
        .outBusinessNo(raffleActivityOrderRes.getOutBusinessNo())
        .payAmount(raffleActivityOrderRes.getPayAmount())
        .build();
  }

  @Override
  public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
    List<RaffleActivitySku> raffleActivitySkus = raffleActivitySkuDao.queryActivitySkuListByActivityId(activityId);
    List<SkuProductEntity> skuProductEntities = new ArrayList<>(raffleActivitySkus.size());
    for (RaffleActivitySku raffleActivitySku : raffleActivitySkus){
      RaffleActivityCount raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(raffleActivitySku.getActivityCountId());
      SkuProductEntity.ActivityCount activityCount = new SkuProductEntity.ActivityCount();
      activityCount.setTotalCount(raffleActivityCount.getTotalCount());
      activityCount.setDayCount(raffleActivityCount.getDayCount());
      activityCount.setMonthCount(raffleActivityCount.getMonthCount());

      skuProductEntities.add(SkuProductEntity.builder()
          .sku(raffleActivitySku.getSku())
          .activityId(raffleActivitySku.getActivityId())
          .activityCountId(raffleActivitySku.getActivityCountId())
          .stockCount(raffleActivitySku.getStockCount())
          .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
          .productAmount(raffleActivitySku.getProductAmount())
          .activityCount(activityCount)
          .build());
    }
    return skuProductEntities;
  }

  @Override
  public BigDecimal queryUserCreditAccountAmount(String userId) {
    try {
      dbRouter.doRouter(userId);
      UserCreditAccount userCreditAccountReq = new UserCreditAccount();
      userCreditAccountReq.setUserId(userId);
      UserCreditAccount userCreditAccount = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
      if (null == userCreditAccount) return BigDecimal.ZERO;
      return userCreditAccount.getAvailableAmount();
    } finally {
      dbRouter.clear();
    }
  }


}
