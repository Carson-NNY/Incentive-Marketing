package cn.bugstack.domain.strategy.service.rule.chain.factory;

import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 工厂
 * @create 2024-01-20 10:54
 */
@Service
public class DefaultChainFactory {

  private final Map<String, ILogicChain> logicChainGroup;

  private final IStrategyRepository repository;

  // The map logicChainGroup in your DefaultChainFactory is populated automatically by Spring when the beans
  // for the ILogicChain implementations (BlackListLogicChain, DefaultLogicChain,
  // RuleWeightLogicChain, etc.) are scanned and registered. This is because of the combination of
  // Spring's @Component scanning and the constructor-based dependency injection mechanism

  //Spring uses constructor-based dependency injection to provide values for these parameters. Here's how Spring resolves the logicChainGroup:
  //
  //    It looks for all beans of type ILogicChain in the application context.
  //    It maps the bean name (specified in the @Component annotation) to the corresponding bean instance.
  //    It creates a Map<String, ILogicChain> where:
  //        The keys are the bean names (rule_blacklist, etc.).
  //        The values are the corresponding ILogicChain implementations (BlackListLogicChain, etc.).
  private DefaultChainFactory(Map<String, ILogicChain> logicChainGroup, IStrategyRepository repository) {
    this.logicChainGroup = logicChainGroup;
    this.repository = repository;
  }

  // fill the logicChainGroup with the chains
  public ILogicChain openLogicChain(Long strategyId) {
    StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
    String[] ruleModels = strategy.ruleModels();

    // if no ruleModels, return default chain
    if (null == ruleModels || ruleModels.length == 0) {
      return logicChainGroup.get("default");
    }

    // get the first chain
    ILogicChain logicChain = logicChainGroup.get(ruleModels[0]);
    ILogicChain current = logicChain;

    // now we connect the chains
    for (int i = 1; i < ruleModels.length; i++) {
      ILogicChain nextChain = logicChainGroup.get(ruleModels[i]);
      current = current.appendNext(nextChain);
    }

    // add the default chain to the end
    current.appendNext(logicChainGroup.get("default"));
    return logicChain;
  }
}
