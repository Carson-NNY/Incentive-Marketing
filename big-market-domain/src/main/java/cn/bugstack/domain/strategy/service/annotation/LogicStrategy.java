package cn.bugstack.domain.strategy.service.annotation;

import cn.bugstack.domain.strategy.service.rule.factory.DefaultLogicFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略自定义枚举, 方便我们的对象往里注入(通过自动扫描的方式就能注入对象)
 * Custom annotations are often used in Spring's custom scanning mechanisms to automatically discover and register classes.
 * Example: A factory (like DefaultLogicFactory) can scan for classes annotated with @LogicStrategy (例如 RuleBlackListLogicFilter) and process them based on their logicMode.
 * This reduces manual configuration, as you don’t need to explicitly register each strategy class.
 * @create 2023-12-31 11:29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicStrategy {
  //Why Use Annotations?:
  //    The @LogicStrategy annotation associates a class with its logic mode in a declarative way.
  //    Instead of manually registering filters in a configuration or factory, you just annotate them, and the factory handles registration automatically.
  DefaultLogicFactory.LogicModel logicMode();

}
