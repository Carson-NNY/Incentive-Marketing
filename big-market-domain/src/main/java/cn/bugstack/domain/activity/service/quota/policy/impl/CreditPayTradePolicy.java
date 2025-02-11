package cn.bugstack.domain.activity.service.quota.policy.impl;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

// 我们有这种设计, 单独的把需要积分支付和签到不用积分支付的sku发放交易方法分开就是为了decouple这两个方法
// 为了未来扩展性
@Service("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {

  private final IActivityRepository activityRepository;

  public CreditPayTradePolicy(IActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Override
  public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
    createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
    activityRepository.doSaveCreditPayOrder(createQuotaOrderAggregate);
  }
}
