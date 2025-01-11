package cn.bugstack.domain.strategy.service.rule.chain;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 责任链装配. 把 appendNext 和 next 方法抽取到这个interface, 所以未来我们在程序中使用
 * ILogicChain的appendNext 和 next 方法时, 我们可以知道这些方法是在更高的接口中定义的,帮助我们了解整体的层次框架
 * @create 2024-01-20 11:53
 */
public interface ILogicChainArmory {


  ILogicChain appendNext(ILogicChain next);

  ILogicChain next();
}
