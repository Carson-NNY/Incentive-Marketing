package cn.bugstack.domain.strategy.repository;


import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

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

  /**
   * 根据规则树ID，查询树结构信息
   *
   * @param treeId 规则树ID
   * @return 树结构信息
   */
  RuleTreeVO queryRuleTreeVOByTreeId(String treeId);

  /**
   * 缓存奖品库存
   *
   * @param cacheKey   key
   * @param awardCount 库存值
   */
  void cacheStrategyAwardCount(String cacheKey, Integer awardCount);

  Boolean subtractionAwardStock(String cacheKey);

  void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO);

  StrategyAwardStockKeyVO takeQueueValue();

  void updateStrategyAwardStock(Long strategyId, Integer awardId);

  StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId);

  Long queryStrategyIdByActivityId(Long activityId);

  Integer queryTodayUserRaffleCount(String userId, Long strategyId);

  Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);
}
