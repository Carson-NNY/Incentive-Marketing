package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.RaffleActivityAccountDay;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IRaffleActivityAccountDayDao {

  @DBRouter
  RaffleActivityAccountDay queryActivityAccountDayByUserId(RaffleActivityAccountDay raffleActivityAccountDayReq);

  // 这里不加@DBRouter注解，因为call这两个方法的地方已经在事务中(用过@DBRouter注解)，所以不需要再加
  int updateActivityAccountDaySubtractionQuota(RaffleActivityAccountDay raffleActivityAccountDay);

  void insertActivityAccountDay(RaffleActivityAccountDay raffleActivityAccountDay);

  @DBRouter
  Integer queryRaffleActivityAccountDayPartakeCount(RaffleActivityAccountDay raffleActivityAccountDay);

  void addAccountQuota(RaffleActivityAccountDay raffleActivityAccountDay);
}
