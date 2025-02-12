package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.UserCreditAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserCreditAccountDao {


  int updateAddAmount(UserCreditAccount userCreditAccountReq);

  void insert(UserCreditAccount userCreditAccountReq);

  UserCreditAccount queryUserCreditAccount(UserCreditAccount userCreditAccountReq);

  int updateSubtractionAmount(UserCreditAccount userCreditAccountReq);
}
