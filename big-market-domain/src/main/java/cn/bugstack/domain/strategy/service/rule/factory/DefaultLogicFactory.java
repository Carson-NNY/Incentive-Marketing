package cn.bugstack.domain.strategy.service.rule.factory;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 规则工厂 // 帮助我们实现策略模式的工厂
 * This DefaultLogicFactory class is an implementation of the Factory pattern to facilitate the strategy pattern for managing business logic filters.
 * @Purpose: The DefaultLogicFactory centralizes the creation, registration, and retrieval of logic filters (ILogicFilter<?> implementations).
 * Why It's Needed: It decouples the logic filter registration and lookup from the core application logic. This promotes modularity, extensibility, and maintainability.
 * @create 2023-12-31 11:23
 */
@Service //@Service Enables it to be autowired into other Spring beans and used as a singleton instance.
public class DefaultLogicFactory {

  //Why ConcurrentHashMap?:
  //    The map is accessed by multiple threads, especially if this factory is used in a multithreaded web application.
  public Map<String, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();

  // Dependency Injection with List
  //How it Works:
  //    Spring automatically injects all beans implementing ILogicFilter<?> (比如RuleBlackListLogicFilter) into the constructor as a List<ILogicFilter<?>>
  //Why It Matters:
  //    This dynamic registration removes the need for manual bean registration or hardcoding logic filters.
  //    It enables a plug-and-play architecture—new filters can be added just by implementing ILogicFilter<?> and annotating them with @LogicStrategy.
  public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
    logicFilters.forEach(logic -> {
      // LogicStrategy is a custom annotation that helps the factory identify and register logic filters (in our case: RuleBlackListLogicFilter...)
      // associates a logic filter with a logic mode.
      LogicStrategy strategy = AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
      if (null != strategy) {
        //Filters are registered in logicFilterMap with their associated logicMode code.
        logicFilterMap.put(strategy.logicMode().getCode(), logic);
      }
    });
  }

  //    Provides type-safe access to the registered logic filters.
  //    Allows retrieval of the logicFilterMap while preserving the flexibility of generic types.
  //Why Use Generics?:
  //    Filters may have different input types (e.g., RaffleEntity, RaffleBeforeEntity).
  public <T extends RuleActionEntity.RaffleEntity> Map<String, ILogicFilter<T>> openLogicFilter() {
    return (Map<String, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
  }

  @Getter
  @AllArgsConstructor
  public enum LogicModel {

    //Encapsulates the supported logic modes as constants.
    RULE_WIGHT("rule_weight","【抽奖前规则】根据抽奖权重返回可抽奖范围KEY"),
    RULE_BLACKLIST("rule_blacklist","【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回"),

    ;

    //Why Use an Enum?:
    //    Ensures type safety by limiting logic modes to predefined values.
    //    Improves code readability and reduces the chance of errors (e.g., typos in string keys).
    private final String code;
    private final String info;

  }

}
