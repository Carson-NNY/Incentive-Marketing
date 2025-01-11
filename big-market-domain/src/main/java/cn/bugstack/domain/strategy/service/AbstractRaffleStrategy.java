package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

  // 策略仓储服务 -> domain层像一个大厨，仓储层提供米面粮油
  protected IStrategyRepository repository;
  // 策略调度服务 -> 只负责抽奖处理，通过新增接口的方式，隔离职责，不需要使用方关心或者调用抽奖的初始化
  protected IStrategyDispatch strategyDispatch;

  private DefaultChainFactory defaultChainFactory;

  public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory) {
    this.repository = repository;
    this.strategyDispatch = strategyDispatch;
    this.defaultChainFactory = defaultChainFactory;
  }

  @Override
  public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
    // 1. 参数校验
    String userId = raffleFactorEntity.getUserId();
    Long strategyId = raffleFactorEntity.getStrategyId();
    if (null == strategyId || StringUtils.isBlank(userId)) {
      throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    // 2. 抽奖前- 通过 chain of responsibility pattern 进行抽奖前规则过滤
    ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
    Integer awardId = logicChain.logic(userId, strategyId);

    // 3. 抽奖中- 规则过滤
    // 查询奖品规则「
    //    抽奖中（拿到奖品ID时，过滤规则. check 用户是否满足抽到的奖品的前提条件(累计抽奖xx次后解锁)
    //    抽奖后（扣减完奖品库存后过滤，抽奖中拦截和无库存则走兜底奖品）」
    StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModelVO(strategyId, awardId);
    RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleActionCenterEntity =
        this.doCheckRaffleCenterLogic(RaffleFactorEntity.builder()
            .userId(userId)
            .strategyId(strategyId)
            .awardId(awardId)
            .build(),
            strategyAwardRuleModelVO.raffleCenterRuleModelList());

    if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionCenterEntity.getCode())){
      log.info("【临时日志】抽到未符合条件的奖品,返回 rule_luck_award 走兜底奖励。");
      return RaffleAwardEntity.builder()
          .awardDesc("中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。")
          .build();
    }

    return RaffleAwardEntity.builder()
        .awardId(awardId)
        .build();

  }

  protected abstract RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity, String ...logics);


}
