package cn.bugstack.domain.strategy.service.rule.tree.factory.engine.impl;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 决策树引擎
 * @create 2024-01-27 11:34
 */
@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {

  private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

  private final RuleTreeVO ruleTreeVO;

  public DecisionTreeEngine(Map<String, ILogicTreeNode> logicTreeNodeGroup, RuleTreeVO ruleTreeVO) {
    this.logicTreeNodeGroup = logicTreeNodeGroup;
    this.ruleTreeVO = ruleTreeVO;
  }

  @Override
  public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId, Date endDateTime) {
    DefaultTreeFactory.StrategyAwardVO strategyAwardData = null;
    String nextNode = ruleTreeVO.getTreeRootRuleNode();
    Map<String, RuleTreeNodeVO> treeNodeMap = ruleTreeVO.getTreeNodeMap();

    // 用于构建decision tree的Node数据结构
    RuleTreeNodeVO ruleTreeNode = treeNodeMap.get(nextNode);
    while (null != ruleTreeNode) {
      // 得到当前Node的对应处理逻辑(对于当前的用户信息进行check来决定下一个Node)
      ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
      String ruleValue = ruleTreeNode.getRuleValue();

// 1	tree_lock	rule_lock	限定用户已完成N次抽奖后解锁	1(这个是针对于rule_lock Tree Node的rule value)	2024-01-27 10:03:09	2024-02-03 10:40:18
      DefaultTreeFactory.TreeActionEntity logicEntity = logicTreeNode.logic(userId, strategyId, awardId, ruleValue, endDateTime);

      RuleLogicCheckTypeVO ruleLogicCheckTypeVO = logicEntity.getRuleLogicCheckType();
      strategyAwardData = logicEntity.getStrategyAwardVO();

      log.info("决策树引擎【{}】treeId:{} node:{} code:{}", ruleTreeVO.getTreeName(), ruleTreeVO.getTreeId(), nextNode, ruleLogicCheckTypeVO.getCode());

      // 在这里根据已经得到的logicEntity和当前节点的子节点来决定我们下一个节点是什么(make decision here)
      nextNode = nextNode((ruleLogicCheckTypeVO.getCode()), ruleTreeNode.getTreeNodeLineVOList());
      ruleTreeNode = treeNodeMap.get(nextNode);

    }

    return strategyAwardData;
  }

  private String nextNode(String matterValue, List<RuleTreeNodeLineVO> ruleTreeNodeLineVOList) {
    if (null == ruleTreeNodeLineVOList || ruleTreeNodeLineVOList.isEmpty()) return null;
    for (RuleTreeNodeLineVO nodeLine : ruleTreeNodeLineVOList) {
      if (decisionLogic(matterValue, nodeLine)) { // 当返回true时，说明我们找到了下一个节点
        return nodeLine.getRuleNodeTo();
      }
    }
    return null;

  }

  public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine) {
    switch (nodeLine.getRuleLimitType()) {
      // decision 分叉路口的data
      //1	tree_lock	rule_lock	rule_stock	EQUAL	ALLOW	0000-00-00 00:00:00	2024-02-03 10:40:25
      //2	tree_lock	rule_lock	rule_luck_award	EQUAL	TAKE_OVER	0000-00-00 00:00:00	2024-02-03 10:40:26
      //3	tree_lock	rule_stock	rule_luck_award	EQUAL	ALLOW	0000-00-00 00:00:00	2025-01-15 01:50:25
      case EQUAL:
        return matterValue.equals(nodeLine.getRuleLimitValue().getCode());
      // 以下规则暂时不需要实现
      case GT:
      case LT:
      case GE:
      case LE:
      default:
        return false;
    }
  }
}
