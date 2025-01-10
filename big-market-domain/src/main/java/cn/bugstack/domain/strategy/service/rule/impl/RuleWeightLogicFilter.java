package cn.bugstack.domain.strategy.service.rule.impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.RuleMatterEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.ILogicFilter;
import cn.bugstack.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_WIGHT)
public class RuleWeightLogicFilter  implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity>{

  @Resource
  private IStrategyRepository repository;

  // hardcode user score(用户积分)
  private Long userScore = 4500L;

  /**
   * 权重规则过滤；
   * 1. 权重规则格式；4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
   * 2. 解析数据格式；判断哪个范围符合用户的特定抽奖范围
   *
   * @param ruleMatterEntity 规则物料实体对象
   * @return 规则过滤结果
   */
  @Override
  public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
    log.info("规则过滤-权重范围 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());

    String userId = ruleMatterEntity.getUserId();
    Long strategyId = ruleMatterEntity.getStrategyId();
    String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(), ruleMatterEntity.getRuleModel());

    // 1. 根据用户ID查询用户抽奖消耗的积分值，本章节我们先写死为固定的值。后续需要从数据库中查询。
    Map<Long, String> analyticalValueGroup = getAnalyticalValue(ruleValue);
    if (null == analyticalValueGroup || analyticalValueGroup.isEmpty()) {
      return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
          .code(RuleLogicCheckTypeVO.ALLOW.getCode())
          .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
          .build();
    }

    // 2. 转换Keys值，并默认排序
    List<Long> analyticalSortedKeys = new ArrayList<>(analyticalValueGroup.keySet());
    Collections.sort(analyticalSortedKeys);

    // 3. 找出最小符合的值，也就是【4500 积分，能找到 4000:102,103,104,105】、【5000 积分，能找到 5000:102,103,104,105,106,107】
    // find The first key that satisfies userScore >= key, or null if no such key exists.
    Long nextValue = analyticalSortedKeys.stream()
        .filter(key -> userScore >= key)
        .findFirst()
        .orElse(null);

    if (null != nextValue) {
      return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
          .data(RuleActionEntity.RaffleBeforeEntity.builder()
              .strategyId(strategyId)
              .ruleWeightValueKey(analyticalValueGroup.get(nextValue))
              .build())
          .ruleModel(DefaultLogicFactory.LogicModel.RULE_WIGHT.getCode())
          .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
          .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
          .build();
    }

    // if did not find it, allow
    return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
        .code(RuleLogicCheckTypeVO.ALLOW.getCode())
        .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
        .build();
  }

  private Map<Long, String> getAnalyticalValue(String ruleValue) {
    String[] ruleValueGroups = ruleValue.split(Constants.SPACE);
    HashMap<Long, String> ruleValueMap = new HashMap<>();

    for(String ruleValueKey : ruleValueGroups) {
      if (ruleValueKey == null || ruleValueKey.isEmpty()) {
        return ruleValueMap;
      }
      String[] parts = ruleValueKey.split(Constants.COLON);
      if (parts.length != 2) {
        throw new IllegalArgumentException("rule_weight rule_rule invalid input format" + ruleValueKey);
      }
      ruleValueMap.put(Long.parseLong(parts[0]), ruleValueKey);
    }
    return ruleValueMap;
  }

  }
