package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 黑名单责任链
 * @create 2024-01-20 10:23
 */
@Slf4j
@Component("rule_blacklist") // 利用@Component注解，将当前类注册到Spring容器中，bean名称为rule_blacklist
//This annotation registers the class as a Spring Bean in the application context, and the provided string (rule_blacklist) specifies the bean name.
//When Spring initializes the application context, it scans the package for all classes annotated with @Component and registers them as beans.
public class BlackListLogicChain  extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;

    @Override
    public Integer logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        // 黑名单校验. 先查询rule_model对应的rule_value
        //14	100001(strategyId)	NULL(awardId)	1	rule_blacklist(rule model)	100:user001,user002,user003	黑名单抽奖，积分兜底	2023-12-09 12:59:45	2024-01-06 14:05:34
        String ruleValue = repository.queryStrategyRuleValue(strategyId, ruleModel());
        String[] splitRuleValue = ruleValue.split(Constants.COLON);
        Integer awardId = Integer.parseInt(splitRuleValue[0]); // 这里得到黑名单的awardId:  100

        // check userId是不是在黑名单里面
        String[] userBlackIds = splitRuleValue[1].split(Constants.SPLIT);
        for (String userBlackId : userBlackIds) {
            if (userId.equals(userBlackId)) {
                log.info("抽奖责任链-黑名单接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
                return awardId;
            }
        }

        // if not in the blacklist, 我们就继续往下一个责任链传递
        return next().logic(userId, strategyId);
    }

    @Override
    protected String ruleModel() {
        return "rule_blacklist";
    }
}
