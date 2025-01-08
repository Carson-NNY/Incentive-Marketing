package cn.bugstack.domain.strategy.service.armory;


import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;

import java.util.List;

/** 兵工厂: 负责初始化策略计算 */
public interface IStrategyArmory {

  boolean assembleLotteryStrategy(Long strategyId);

  }
