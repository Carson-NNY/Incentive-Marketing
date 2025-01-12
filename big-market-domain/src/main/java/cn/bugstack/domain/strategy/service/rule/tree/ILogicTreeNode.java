package cn.bugstack.domain.strategy.service.rule.tree;

import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

public interface ILogicTreeNode {
  DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId);

}
