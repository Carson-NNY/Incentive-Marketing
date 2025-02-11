package cn.bugstack.domain.activity.service.quota.policy.impl;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("rebate_no_pay_trade")
public class RebateNoPayTradePolicy implements ITradePolicy {

  private final IActivityRepository activityRepository;

  public RebateNoPayTradePolicy(IActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Override
  public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
    createQuotaOrderAggregate.setOrderState(OrderStateVO.completed);
    createQuotaOrderAggregate.getActivityOrderEntity().setPayAmount(BigDecimal.ZERO);
    activityRepository.doSaveNoPayOrder(createQuotaOrderAggregate);

  }
}
