package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;

public interface IRaffleStrategy {

  RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity);
}
