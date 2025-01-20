package cn.bugstack.domain.activity.service.rule;

public interface IActionArmory {
  IActionChain next();

  IActionChain appendNext(IActionChain next);
}
