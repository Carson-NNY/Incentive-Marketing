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


@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_BLACKLIST) // 对于这个类通过RULE_BLACKLIST的LogicModel 来注册到LogicStrategy, 见 annotation/LogicStrategy.java
public class RuleBlackListLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

  @Resource
  private IStrategyRepository repository;

  @Override
  public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
    log.info("规则过滤-黑名单 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
    String userId = ruleMatterEntity.getUserId();

    // 先查询rule_model对应的rule_value
    //14	100001(strategyId)	NULL(awardId)	1	rule_blacklist(rule model)	100:user001,user002,user003	黑名单抽奖，积分兜底	2023-12-09 12:59:45	2024-01-06 14:05:34
    String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(), ruleMatterEntity.getRuleModel());
    String[] splitRuleValue = ruleValue.split(Constants.COLON);
    Integer awardId = Integer.parseInt(splitRuleValue[0]); // 这里得到黑名单的awardId:  100

    // check userId是不是在黑名单里面
    String[] userBlackIds = splitRuleValue[1].split(Constants.SPLIT);
    for (String userBlackId : userBlackIds) {
      if (userId.equals(userBlackId)) {
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
            .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
            .data(RuleActionEntity.RaffleBeforeEntity.builder()
                .strategyId(ruleMatterEntity.getStrategyId())
                .awardId(awardId)
                .build())
            .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
            .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
            .build();
      }
    }
    // if not in the blacklist, return ALLOW
    return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
        .code(RuleLogicCheckTypeVO.ALLOW.getCode())
        .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
        .build();
  }
}
