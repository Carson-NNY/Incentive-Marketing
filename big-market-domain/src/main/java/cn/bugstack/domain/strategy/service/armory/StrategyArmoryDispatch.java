package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import cn.bugstack.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 兵工厂: 负责初始化策略计算 */

@Slf4j
@Service
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

  @Resource
  private IStrategyRepository repository;

  @Override
  public boolean assembleLotteryStrategy(Long strategyId) {
    // 1. 查询策略配置
    List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);
    assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntities);

    // 2. 权重策略配置 - 适用于 rule_weight 权重规则配置
    // 当有rule_models: rule_weight时来判断是否消耗了一定积分来对抽奖来进行返利的升级操作: 4000:102, 103,104,105 5000: 102, 103, 104, 105, 106, 107 6000: 102,103, 104,105,106,107,108,109
    String ruleWeight = repository.queryStrategyEntityByStrategyId(strategyId).getRuleWeight();
    if (null == ruleWeight) return true;

    StrategyRuleEntity strategyRuleEntity = repository.queryStrategyRule(strategyId, ruleWeight);
    if (null == strategyRuleEntity) {
      throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(), ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
    }

    Map<String, List<Integer>> ruleWeightValueMap = strategyRuleEntity.getRuleWeightValues();
    Set<String> keys = ruleWeightValueMap.keySet();
    for (String key : keys) {
      List<Integer> ruleWeightValues = ruleWeightValueMap.get(key);
      ArrayList<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardEntities);
      strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
      assembleLotteryStrategy(String.valueOf(strategyId).concat(Constants.UNDERLINE).concat(key), strategyAwardEntitiesClone);
    }

    return true;
  }

  // 用来进行概率抽奖的算法实现, 使用空间换时间的方式
  /**
   * 计算公式；
   * 1. 找到范围内最小的概率值，比如 0.1、0.02、0.003，需要找到的值是 0.003
   * 2. 基于1找到的最小值，0.003 就可以计算出百分比、千分比的整数值。这里就是1000
   * 3. 那么「概率 * 1000」分别占比100个、20个、3个，总计是123个
   * 4. 后续的抽奖就用123作为随机数的范围值，生成的值100个都是0.1概率的奖品、20个是概率0.02的奖品、最后是3个是0.003的奖品。
   */
  private void assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardEntities) {
    // 1. 获取最小概率值
    BigDecimal minAwardRate = strategyAwardEntities.stream()
        .map(StrategyAwardEntity::getAwardRate)
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    // 2. 用 1 * 1000 获得概率范围，百分位、千分位、万分位
    BigDecimal rateRange = BigDecimal.valueOf(convert(minAwardRate.doubleValue()));

    // 3. 生成策略奖品概率查找表「这里指需要在list集合中，存放上对应的奖品占位即可，占位越多等于概率越高」
    List<Integer> strategyAwardSearchRateTables = new ArrayList<>(rateRange.intValue());
    for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
      Integer awardId = strategyAward.getAwardId();
      BigDecimal awardRate = strategyAward.getAwardRate();
      // Calculates how many times the awardId should appear in the search table by multiplying the scaled rateRange by the award's probability and rounding up to the nearest integer.
      // 「概率 * 1000」分别占比100个、20个、3个，总计是123个
      for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
        strategyAwardSearchRateTables.add(awardId);
      }
    }

    // 4. 对存储的奖品进行乱序操作
    Collections.shuffle(strategyAwardSearchRateTables);

    // 5. 把map倒转过来,  Converts the shuffled list into a LinkedHashMap where:
    //    Key: An integer index representing a position in the search table.
    //    Value: The awardId corresponding to that position.
    Map<Integer, Integer> shuffleStrategyAwardSearchRateTable = new LinkedHashMap<>();
    for (int i = 0; i < strategyAwardSearchRateTables.size(); i++) {
      shuffleStrategyAwardSearchRateTable.put(i, strategyAwardSearchRateTables.get(i));
    }

    // 6. 存放到 Redis
    repository.storeStrategyAwardSearchRateTable(key, shuffleStrategyAwardSearchRateTable.size(), shuffleStrategyAwardSearchRateTable);
  }


  /**
   * 转换计算，只根据小数位来计算。如【0.01返回100】、【0.009返回1000】、【0.0018返回10000】
   */
  private double convert(double min){
    double current = min;
    double max = 1;
    while (current < 1){
      current = current * 10;
      max = max * 10;
    }
    return max;
  }


  @Override
  public Integer getRandomAwardId(Long strategyId) {
    int rateRange = repository.getRateRange(strategyId);
    return repository.getStrategyAwardAssemble(String.valueOf(strategyId), new SecureRandom().nextInt(rateRange));
  }


  @Override
  public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
    String key = String.valueOf(strategyId).concat(Constants.UNDERLINE).concat(ruleWeightValue);
    int rateRange = repository.getRateRange(key);
    return repository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
  }


}
