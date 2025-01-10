package cn.bugstack.domain.strategy.service.raffle;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

  // 策略仓储服务 -> domain层像一个大厨，仓储层提供米面粮油
  protected IStrategyRepository repository;
  // 策略调度服务 -> 只负责抽奖处理，通过新增接口的方式，隔离职责，不需要使用方关心或者调用抽奖的初始化
  protected IStrategyDispatch strategyDispatch;

  public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch) {
    this.repository = repository;
    this.strategyDispatch = strategyDispatch;
  }

  @Override
  public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
    // 1. 参数校验
    String userId = raffleFactorEntity.getUserId();
    Long strategyId = raffleFactorEntity.getStrategyId();
    if (null == strategyId || StringUtils.isBlank(userId)) {
      throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    // 2. 策略查询
    StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);

    // 3. 抽奖前 - 规则过滤
    RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity =
        this.doCheckRaffleBeforeLogic(RaffleFactorEntity.builder()
            .userId(userId)
            .strategyId(strategyId)
            .build(),
            strategy.ruleModels());

    // 4. handle the case if we need to take over the raffle for specific rule
    if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionEntity.getCode())) {
      if (DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(ruleActionEntity.getRuleModel())) {
        // 黑名单返回固定的奖品ID
        return RaffleAwardEntity.builder()
            .awardId(ruleActionEntity.getData().getAwardId())
            .build();
      } else if (DefaultLogicFactory.LogicModel.RULE_WIGHT.getCode().equals(ruleActionEntity.getRuleModel())) {
        // 权重根据返回的信息进行抽奖
        //13	100001	NULL	1	rule_weight	4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109	消耗6000分，必中奖范围	2023-12-09 10:30:43	2023-12-31 14:51:50
        RuleActionEntity.RaffleBeforeEntity raffleBeforeEntity = ruleActionEntity.getData();
        String ruleWeightValueKey = raffleBeforeEntity.getRuleWeightValueKey();
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey);
        return RaffleAwardEntity.builder()
            .awardId(awardId)
            .build();
      }
    }

    // 4. default 抽奖流程
    Integer awardId = strategyDispatch.getRandomAwardId(strategyId);

    return RaffleAwardEntity.builder()
        .awardId(awardId)
        .build();

  }

  protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity, String ...logics);


}
