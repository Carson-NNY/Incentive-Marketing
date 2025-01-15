package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLimitTypeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.persistent.dao.IRuleTreeDao;
import cn.bugstack.infrastructure.persistent.dao.IRuleTreeNodeDao;
import cn.bugstack.infrastructure.persistent.dao.IRuleTreeNodeLineDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyRuleDao;
import cn.bugstack.infrastructure.persistent.po.RuleTree;
import cn.bugstack.infrastructure.persistent.po.RuleTreeNode;
import cn.bugstack.infrastructure.persistent.po.RuleTreeNodeLine;
import cn.bugstack.infrastructure.persistent.po.Strategy;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import cn.bugstack.infrastructure.persistent.po.StrategyRule;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 实现在big-market-domain里面定义的IStrategyRepository interface
 */
@Slf4j
@Repository
public class StrategyRepository implements IStrategyRepository {

  @Resource
  private IStrategyDao strategyDao;

  @Resource
  private IStrategyRuleDao strategyRuleDao;

  @Resource
  private IStrategyAwardDao strategyAwardDao;

  @Resource
  private IRedisService redisService;

  @Resource
  private IRuleTreeDao ruleTreeDao;

  @Resource
  private IRuleTreeNodeDao ruleTreeNodeDao;

  @Resource
  private IRuleTreeNodeLineDao ruleTreeNodeLineDao;

  @Override
  public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
    String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;
    List<StrategyAwardEntity> strategyAwardEntities = redisService.getValue(cacheKey);

    if(strategyAwardEntities != null && !strategyAwardEntities.isEmpty()) {
      return strategyAwardEntities;
    }

    // 直接从DB中读取数据
    List<StrategyAward> strategyAwards =  strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
    strategyAwardEntities = new ArrayList<>(strategyAwards.size());
    for (StrategyAward strategyAward : strategyAwards) {
      StrategyAwardEntity strategyAwardEntity = StrategyAwardEntity.builder()
        .strategyId(strategyAward.getStrategyId())
        .awardId(strategyAward.getAwardId())
        .awardCount(strategyAward.getAwardCount())
        .awardCountSurplus(strategyAward.getAwardCountSurplus())
        .awardRate(strategyAward.getAwardRate())
        .build();
      strategyAwardEntities.add(strategyAwardEntity);
    }

    //先存入redis 为了以后的缓存
    redisService.setValue(cacheKey, strategyAwardEntities);
    return strategyAwardEntities;
  }

  @Override
  public void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> strategyAwardSearchRateTable) {
    // 1. 存储抽奖策略范围值，如10000，用于生成1000以内的随机数
    redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key, rateRange);
    // 2. 存储概率查找表
    Map<Integer, Integer> cacheRateTable = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key);
    cacheRateTable.putAll(strategyAwardSearchRateTable);
  }

  @Override
  public int getRateRange(Long strategyId) {
    return getRateRange(String.valueOf(strategyId));
  }

  @Override
  public int getRateRange(String key) {
    return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key);
  }

  @Override
  public Integer getStrategyAwardAssemble(String key, Integer rateKey) {
    return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key, rateKey);
  }

  @Override
  public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
    // 优先从缓存获取
    String cacheKey = Constants.RedisKey.STRATEGY_KEY + strategyId;
    StrategyEntity strategyEntity = redisService.getValue(cacheKey);
    if (null != strategyEntity) return strategyEntity;
    Strategy strategy = strategyDao.queryStrategyByStrategyId(strategyId);
    if (null == strategy) return StrategyEntity.builder().build();
    strategyEntity = StrategyEntity.builder()
        .strategyId(strategy.getStrategyId())
        .strategyDesc(strategy.getStrategyDesc())
        .ruleModels(strategy.getRuleModels())
        .build();
    redisService.setValue(cacheKey, strategyEntity);
    return strategyEntity;
  }

  @Override
  public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
    StrategyRule strategyRuleReq = new StrategyRule();
    strategyRuleReq.setStrategyId(strategyId);
    strategyRuleReq.setRuleModel(ruleModel);
    StrategyRule strategyRuleRes = strategyRuleDao.queryStrategyRule(strategyRuleReq);
    return StrategyRuleEntity.builder()
        .strategyId(strategyRuleRes.getStrategyId())
        .awardId(strategyRuleRes.getAwardId())
        .ruleType(strategyRuleRes.getRuleType())
        .ruleModel(strategyRuleRes.getRuleModel())
        .ruleValue(strategyRuleRes.getRuleValue())
        .ruleDesc(strategyRuleRes.getRuleDesc())
        .build();
  }

  @Override
  public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
    return queryStrategyRuleValue(strategyId, null, ruleModel);
  }

  @Override
  public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
    StrategyRule strategyRule = new StrategyRule();
    strategyRule.setStrategyId(strategyId);
    strategyRule.setAwardId(awardId);
    strategyRule.setRuleModel(ruleModel);
    return strategyRuleDao.queryStrategyRuleValue(strategyRule);
  }

  @Override
  public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId) {
    StrategyAward strategyAward = new StrategyAward();
    strategyAward.setStrategyId(strategyId);
    strategyAward.setAwardId(awardId);
    String ruleModels = strategyAwardDao.queryStrategyAwardRuleModels(strategyAward);
    return StrategyAwardRuleModelVO.builder()
        .ruleModels(ruleModels)
        .build();
  }

  @Override
  public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
    // 优先从缓存获取
    String cacheKey = Constants.RedisKey.RULE_TREE_VO_KEY + treeId;
    RuleTreeVO ruleTreeVOCache = redisService.getValue(cacheKey);
    if (null != ruleTreeVOCache) return ruleTreeVOCache;

    // 从数据库获取
    RuleTree ruleTree = ruleTreeDao.queryRuleTreeByTreeId(treeId);
    List<RuleTreeNode> ruleTreeNodes = ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(treeId);
    List<RuleTreeNodeLine> ruleTreeNodeLines = ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(treeId);

    // 1. tree node line 转换Map结构
    Map<String, List<RuleTreeNodeLineVO>> ruleTreeNodeLineMap = new HashMap<>();
    for (RuleTreeNodeLine ruleTreeNodeLine : ruleTreeNodeLines) {
      RuleTreeNodeLineVO ruleTreeNodeLineVO = RuleTreeNodeLineVO.builder()
          .treeId(ruleTreeNodeLine.getTreeId())
          .ruleNodeFrom(ruleTreeNodeLine.getRuleNodeFrom())
          .ruleNodeTo(ruleTreeNodeLine.getRuleNodeTo())
          .ruleLimitType(RuleLimitTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitType()))
          .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitValue()))
          .build();

      //The map ruleTreeNodeLineMap stores keys and associated lists of RuleTreeNodeLineVO objects.
      //For the given key (ruleTreeNodeLine.getRuleNodeFrom()):
      //    If it already exists in the map, retrieve its associated list.
      //    If it does not exist, create a new empty ArrayList, associate it with the key in the map, and return this new list.
      List<RuleTreeNodeLineVO> ruleTreeNodeLineVOList = ruleTreeNodeLineMap.computeIfAbsent(ruleTreeNodeLine.getRuleNodeFrom(), k -> new ArrayList<>());
      ruleTreeNodeLineVOList.add(ruleTreeNodeLineVO);
    }

    // 2. tree node 转换为Map结构
    Map<String, RuleTreeNodeVO> treeNodeMap = new HashMap<>();
    for (RuleTreeNode ruleTreeNode : ruleTreeNodes) {
      RuleTreeNodeVO ruleTreeNodeVO = RuleTreeNodeVO.builder()
          .treeId(ruleTreeNode.getTreeId())
          .ruleKey(ruleTreeNode.getRuleKey())
          .ruleDesc(ruleTreeNode.getRuleDesc())
          .ruleValue(ruleTreeNode.getRuleValue())
          .treeNodeLineVOList(ruleTreeNodeLineMap.get(ruleTreeNode.getRuleKey()))
          .build();
      treeNodeMap.put(ruleTreeNode.getRuleKey(), ruleTreeNodeVO);
    }

    // 3. 构建 Rule Tree
    RuleTreeVO ruleTreeVODB = RuleTreeVO.builder()
        .treeId(ruleTree.getTreeId())
        .treeName(ruleTree.getTreeName())
        .treeDesc(ruleTree.getTreeDesc())
        .treeRootRuleNode(ruleTree.getTreeRootRuleKey())
        .treeNodeMap(treeNodeMap)
        .build();

    redisService.setValue(cacheKey, ruleTreeVODB);
    return ruleTreeVODB;
  }

  @Override
  public void cacheStrategyAwardCount(String cacheKey, Integer awardCount) {
    if (null != redisService.getValue(cacheKey)) return;
    redisService.setAtomicLong(cacheKey, awardCount);

  }

  @Override
  public Boolean subtractionAwardStock(String cacheKey) {
    // decr 返回的是减少之后的值
    long surplus = redisService.decr(cacheKey);
    if (surplus < 0) {
      redisService.setValue(cacheKey, 0);
      return false;
    }

    // 通过Redis手动做一个锁
    // 1. 按照cacheKey decr 后的值，如 99、98、97 和 key 组成为库存锁的key进行使用。
    // 2. 加锁为了兜底，如果后续有恢复库存，手动处理等，也不会超卖。因为所有的可用库存key，都被加锁了!!!!
    String lockKey = cacheKey + Constants.UNDERLINE + surplus;
    Boolean lock = redisService.setNx(lockKey); // return true if there is no this key in redis
    if (!lock) {
      log.info("策略奖品库存加锁失败 {}", lockKey);
    }

    return true;
  }

  @Override
  public void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO) {
    // 1. 消费奖品库存
    String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;

    //A blocking queue is a thread-safe queue that can block the producer or consumer threads until the queue is ready for data production or consumption
    RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
    //Also provided by Redisson, it allows for scheduling tasks or messages for delayed execution or consumption.
    //It wraps around the blockingQueue, enabling tasks (represented as StrategyAwardStockKeyVO) to be delayed before being available in the queue for consumption.
    // 原因使用延迟队列，是为了防止瞬间高并发，导致数据库压力过大
    RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
    //The offer method in RDelayedQueue is used to add an object to the queue with a specified delay.
    //RDelayedQueue) allows stock updates to be processed in batches rather than in real-time, reducing the frequency of direct database writes.
    //This design ensures the database handles fewer transactions, minimizing contention and improving overall system scalability.
    delayedQueue.offer(strategyAwardStockKeyVO, 3, TimeUnit.SECONDS);

  }

  @Override
  public StrategyAwardStockKeyVO takeQueueValue() {
    String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
    RBlockingQueue<StrategyAwardStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
    return destinationQueue.poll();
  }

  @Override
  public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
    StrategyAward strategyAward = new StrategyAward();
    strategyAward.setStrategyId(strategyId);
    strategyAward.setAwardId(awardId);
    strategyAwardDao.updateStrategyAwardStock(strategyAward);
  }
}
