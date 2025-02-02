package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.valobj.RuleWeightVO;

import java.util.List;
import java.util.Map;

// 抽奖规则接口
public interface IRaffleRule {

  Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);

  List<RuleWeightVO> queryAwardRuleWeight(Long strategyId);

  List<RuleWeightVO> queryAwardRuleWeightByActivityId(Long activityId);
}
