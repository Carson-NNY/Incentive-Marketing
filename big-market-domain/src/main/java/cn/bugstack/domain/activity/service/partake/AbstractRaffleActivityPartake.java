package cn.bugstack.domain.activity.service.partake;

import cn.bugstack.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.PartakeRaffleActivityEntity;
import cn.bugstack.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.IRaffleActivityPartakeService;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动参与抽奖类 (abstract类就是描述一系列的business logic步骤的high level)
 * @create 2024-04-05 07:53
 */
@Slf4j
public abstract class AbstractRaffleActivityPartake implements IRaffleActivityPartakeService {

  protected final IActivityRepository activityRepository;

  protected AbstractRaffleActivityPartake(IActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Override
  public UserRaffleOrderEntity createOrder(String userId, Long activityId) {
    return createOrder(PartakeRaffleActivityEntity.builder()
        .userId(userId)
        .activityId(activityId)
        .build());

  }

  @Override
  public UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
    // 0. 基础信息
    String userId = partakeRaffleActivityEntity.getUserId();
    Long activityId = partakeRaffleActivityEntity.getActivityId();
    Date currentDate = new Date();

    // 1. 活动查询
    ActivityEntity activityEntity = activityRepository.queryRaffleActivityByActivityId(activityId);

    // 这里考虑到需要拓展的可能不大, 就不做责任链等设计来简化代码开发
    // 校验；活动状态
    if (!ActivityStateVO.open.equals(activityEntity.getState())) {
      throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(), ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
    }
    // 校验；活动日期「开始时间 <- 当前时间 -> 结束时间」
    if (activityEntity.getBeginDateTime().after(currentDate) || activityEntity.getEndDateTime().before(currentDate)) {
      throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
    }

    // 2. 查询未被使用的活动参与订单记录(之前下了订单, 但是由于某些网络原因/数据库原因未被使用)
    UserRaffleOrderEntity userRaffleOrderEntity = activityRepository.queryNoUseRaffleOrder(partakeRaffleActivityEntity);
    if (null != userRaffleOrderEntity) {
      log.info("创建参与活动订单[已存在 未消费] userId:{} activityId:{} userRaffleOrderEntity:{}", userId, activityId, userRaffleOrderEntity);
      return userRaffleOrderEntity;
    }

    // 3. 额度账户过滤(check 总/月/日额度是否已经用完) & 返回账户构建对象
    CreatePartakeOrderAggregate createPartakeOrderAggregate = this.doFilterAccount(userId, activityId, currentDate);

    // 4. 创建订单
    UserRaffleOrderEntity userRaffleOrder = this.buildUserRaffleOrder(userId, activityId, currentDate);

    // 5. 填充抽奖order实体对象
    createPartakeOrderAggregate.setUserRaffleOrderEntity(userRaffleOrder);

    // 6. 根据聚合对象来进行最后一系列操作(数据库的总账户/月账户/日账户的订单surplus变化 + 数据库写入订单).  一个领域内的一个聚合是一个事务操作
    activityRepository.saveCreatePartakeOrderAggregate(createPartakeOrderAggregate);

    return userRaffleOrder;
  }

  protected abstract UserRaffleOrderEntity buildUserRaffleOrder(String userId, Long activityId, Date currentDate);

  //(里面会进行各种...)
  protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date currentDate);
}
