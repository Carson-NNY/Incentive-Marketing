package cn.bugstack.domain.credit.repository;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;

public interface ICreditRepository {
  void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);

  CreditAccountEntity queryUserCreditAccount(String userId);
}
