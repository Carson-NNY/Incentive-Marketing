package cn.bugstack.domain.strategy.service.rule.tree.impl;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 库存扣减节点
 * @create 2024-01-27 11:25
 */
@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {

  @Resource
  private IStrategyDispatch strategyDispatch;

  @Resource
  private IStrategyRepository strategyRepository;

  //Using Redis for inventory deduction prevents race conditions and ensures the database isn’t
  // overwhelmed by high-concurrency updates. Redis, being an in-memory database,
  // can handle a significantly higher throughput compared to traditional relational databases.
  @Override
  public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue, Date endDateTime) {

    // 1. 扣减库存. 不直接更新DB, 先进行Redis的更新, 这样保证不会出现超卖的情况 + 保证了高并发下的性能
    Boolean status = strategyDispatch.subtractionAwardStock(strategyId, awardId, endDateTime);

    // 2. Redis扣减成功, 证明没有超卖, 加入发奖队列
    if (status) {
      strategyRepository.awardStockConsumeSendQueue(
          StrategyAwardStockKeyVO.builder()
              .strategyId(strategyId)
              .awardId(awardId)
              .build());

      return DefaultTreeFactory.TreeActionEntity.builder()
          .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
          .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
              .awardId(awardId)
              .awardRuleValue("")
              .build())
          .build();
    }

    // 如果库存不足, 走兜底奖励
    log.warn("规则过滤-库存扣减-告警，库存不足。userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
    return DefaultTreeFactory.TreeActionEntity.builder()
        .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
        .build();
  }
}
