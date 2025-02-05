package cn.bugstack.domain.award.repository;

import cn.bugstack.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;

public interface IAwardRepository {

  void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);

  String queryAwardConfig(Integer awardId);

  void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate);

  String queryAwardKey(Integer awardId);

}
