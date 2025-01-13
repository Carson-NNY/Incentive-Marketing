package cn.bugstack.domain.strategy.service.rule.tree.factory.engine.impl;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

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
  public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId) {
    DefaultTreeFactory.StrategyAwardVO strategyAwardData = null;
    String nextNode = ruleTreeVO.getTreeRootRuleNode();
    Map<String, RuleTreeNodeVO> treeNodeMap = ruleTreeVO.getTreeNodeMap();

    // 用于构建decision tree的Node数据结构
    RuleTreeNodeVO ruleTreeNode = treeNodeMap.get(nextNode);
    while (null != ruleTreeNode) {
      // 得到当前Node的对应处理逻辑(对于当前的用户信息进行check来决定下一个Node)
      ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
      DefaultTreeFactory.TreeActionEntity logicEntity = logicTreeNode.logic(userId, strategyId, awardId);

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
    if (null == ruleTreeNodeLineVOList || ruleTreeNodeLineVOList.isEmpty()) {
      return null;
    }

    for (RuleTreeNodeLineVO nodeLine : ruleTreeNodeLineVOList) {
      if (decisionLogic(matterValue, nodeLine)) { // 当返回true时，说明我们找到了下一个节点
        return nodeLine.getRuleNodeTo();
      }
    }
    throw new RuntimeException("决策树引擎处理异常，nextNode 计算失败");

  }

  public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine) {
    switch (nodeLine.getRuleLimitType()) {
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
