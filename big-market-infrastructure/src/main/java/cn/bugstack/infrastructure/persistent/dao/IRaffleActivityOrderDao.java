package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.RaffleActivityOrder;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动单Dao
 * @create 2024-03-09 10:08
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IRaffleActivityOrderDao {

  // 这个DBRouter就是用来分库分表的，key是分库分表的依据，这里是userId. whenever you want to insert a new record,
  // 我们都会根据userId来决定这个记录应该插入到哪个库的哪个表里面
  @DBRouter(key = "userId")
  void insert(RaffleActivityOrder raffleActivityOrder);

  @DBRouter
  List<RaffleActivityOrder> queryRaffleActivityOrderByUserId(String userId);

}
