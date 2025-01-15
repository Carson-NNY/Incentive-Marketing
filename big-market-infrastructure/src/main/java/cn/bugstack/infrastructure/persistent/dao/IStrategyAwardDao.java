package cn.bugstack.infrastructure.persistent.dao;

import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyAwardDao {

  List<StrategyAward> queryStrategyAwardList();

  List<StrategyAward> queryStrategyAwardListByStrategyId(Long strategyId);

  String queryStrategyAwardRuleModels(StrategyAward strategyAward);

  void updateStrategyAwardStock(StrategyAward strategyAward);


}
