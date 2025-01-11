package cn.bugstack.domain.strategy.repository;


import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;

import java.util.List;
import java.util.Map;

public interface IStrategyRepository {
  List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

  void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> strategyAwardSearchRateTable);

  int getRateRange(Long strategyId);

  int getRateRange(String strategyId);

  Integer getStrategyAwardAssemble(String key, Integer rateKey);

  StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

  StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);

  String queryStrategyRuleValue(Long strategyId, String ruleModel);

  String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel);

  StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId);
}
