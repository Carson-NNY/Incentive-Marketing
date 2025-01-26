package cn.bugstack.domain.award.repository;

import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;

public interface IAwardRepository {

  void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);
}
