package cn.bugstack.domain.activity.service.quota.rule;

public interface IActionArmory {
  IActionChain next();

  IActionChain appendNext(IActionChain next);
}
