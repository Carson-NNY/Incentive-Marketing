package cn.bugstack.domain.strategy.service.raffle;

import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.AbstractRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy
{


  public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory) {
    super(repository, strategyDispatch, defaultChainFactory, defaultTreeFactory);
  }

  @Override
  public DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
    ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
    return logicChain.logic(userId, strategyId);
  }

  @Override
  public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId) {

    // if no rule model for the award, just return the award since we don't need to check anything
//    23	100006	102	随机积分	NULL	97	97	0.9700	tree_lock(这里是这个奖品对应的rule models)	1	2023-12-09 09:38:31	2024-02-03 11:17:10
    // 我们的逻辑就是根据这个抽到的奖品 会有哪些rule model来进行树的构建 进一步进行检查库存等关于这个奖品的check
    StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModelVO(strategyId, awardId);
    if (null == strategyAwardRuleModelVO) {
      return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
    }

    // 得到rule model对应的tree(已经在数据库定好的树的架构), 这里的queryRuleTreeVOByTreeId会把从数据库中查到的树的架构进行RuleTreeVO的装配
    RuleTreeVO ruleTreeVO = repository.queryRuleTreeVOByTreeId(strategyAwardRuleModelVO.getRuleModels());
    if (null == ruleTreeVO) {
      throw new RuntimeException("存在抽奖策略配置的规则模型 Key，未在库表 rule_tree、rule_tree_node、rule_tree_line 配置对应的规则树信息 " + strategyAwardRuleModelVO.getRuleModels());
    }

    // 工厂进行装配tree工厂的 engine装配
    IDecisionTreeEngine decisionTreeEngine = defaultTreeFactory.openLogicTree(ruleTreeVO);
    // 开始check
    return decisionTreeEngine.process(userId, strategyId, awardId);
  }

  @Override
  public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
    return repository.takeQueueValue();
  }

  @Override
  public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
    repository.updateStrategyAwardStock(strategyId, awardId);
  }
}
