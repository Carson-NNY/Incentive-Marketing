package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.DailyBehaviorRebate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IDailyBehaviorRebateDao {
  List<DailyBehaviorRebate> queryDailyBehaviorRebateByBehaviorType(String code);

}
