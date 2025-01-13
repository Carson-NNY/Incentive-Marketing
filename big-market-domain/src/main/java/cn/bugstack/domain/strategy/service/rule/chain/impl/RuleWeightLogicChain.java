package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 权重抽奖责任链
 * @create 2024-01-20 10:38
 */
@Slf4j
@Component("rule_weight")
public class RuleWeightLogicChain extends AbstractLogicChain {

  @Resource
  private IStrategyRepository repository;

  @Resource
  protected IStrategyDispatch strategyDispatch;

  public Long userScore = 0L;

  /**
   * 权重责任链过滤；
   * 1. 权重规则格式；4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
   * 2. 解析数据格式；判断哪个范围符合用户的特定抽奖范围
   */
  @Override
  public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
    log.info("抽奖责任链-权重开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());

    String ruleValue = repository.queryStrategyRuleValue(strategyId, ruleModel());

    // 1. 根据用户ID查询用户抽奖消耗的积分值，本章节我们先写死为固定的值。后续需要从数据库中查询。
    Map<Long, String> analyticalValueGroup = getAnalyticalValue(ruleValue);
    if (null == analyticalValueGroup || analyticalValueGroup.isEmpty()) {
      return null;
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
      Integer awardId = strategyDispatch.getRandomAwardId(strategyId, analyticalValueGroup.get(nextValue));
      log.info("抽奖责任链-权重接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
      return DefaultChainFactory.StrategyAwardVO.builder()
          .awardId(awardId)
          .logicModel(ruleModel())
          .build();
    }

    // 5. 过滤其他责任链
    log.info("抽奖责任链-权重放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
    return next().logic(userId, strategyId);
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

  @Override
  protected String ruleModel() {
    return DefaultChainFactory.LogicModel.RULE_WEIGHT.getCode();
  }
}
