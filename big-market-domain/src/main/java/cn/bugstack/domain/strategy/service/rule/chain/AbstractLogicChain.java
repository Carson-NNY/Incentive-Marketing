package cn.bugstack.domain.strategy.service.rule.chain;

//The AbstractLogicChain is designed to provide shared functionality or structure for its subclasses
// (e.g., appendNext, next, or ruleModel). It is not meant to be instantiated directly or to have a
// complete implementation of the chain logic.
public abstract class AbstractLogicChain implements ILogicChain{

  private ILogicChain next;

  @Override
  public ILogicChain appendNext(ILogicChain next) {
    this.next = next;
    return next;
  }

  @Override
  public ILogicChain next() {
    return next;
  }

  protected abstract String ruleModel();
}
