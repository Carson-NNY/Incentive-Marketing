package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.rule.factory.DefaultActivityChainFactory;

//RaffleActivitySupport 是支撑类，类似 Spring 源码中也会有 XxxSupport 来提供数据支撑。
// 这样可以简化抽象类（AbstractRaffleActivity）里的代码量。
public class RaffleActivitySupport {

  protected DefaultActivityChainFactory defaultActivityChainFactory;
  protected IActivityRepository activityRepository;

  public RaffleActivitySupport(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
    this.activityRepository = activityRepository;
    this.defaultActivityChainFactory = defaultActivityChainFactory;
  }

  public ActivitySkuEntity queryActivitySku(Long sku) {
    return activityRepository.queryActivitySku(sku);
  }

  public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
    return activityRepository.queryRaffleActivityByActivityId(activityId);
  }

  public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
    return activityRepository.queryRaffleActivityCountByActivityCountId(activityCountId);
  }

}
