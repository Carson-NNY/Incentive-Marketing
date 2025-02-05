package cn.bugstack.domain.award.service;

import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.domain.award.model.aggregate.UserAwardRecordAggregate;
import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;
import cn.bugstack.domain.award.model.entity.TaskEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.valobj.TaskStateVO;
import cn.bugstack.domain.award.repository.IAwardRepository;
import cn.bugstack.domain.award.service.distribute.IDistributeAward;
import cn.bugstack.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class AwardService implements IAwardService{

  private final IAwardRepository awardRepository;
  private final SendAwardMessageEvent sendAwardMessageEvent;

  // 避免使用 if/else 语句的方式，使用策略模式(代码更好的扩展性)
  private final Map<String, IDistributeAward> distributeAwardMap;

  public AwardService(Map<String, IDistributeAward> distributeAwardMap, SendAwardMessageEvent sendAwardMessageEvent, IAwardRepository awardRepository) {
    this.distributeAwardMap = distributeAwardMap;
    this.sendAwardMessageEvent = sendAwardMessageEvent;
    this.awardRepository = awardRepository;
  }

  @Override
  public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
    SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
    sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
    sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
    sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
    sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());
    sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());

    BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

    // 构建任务对象
    TaskEntity taskEntity = new TaskEntity();
    taskEntity.setUserId(userAwardRecordEntity.getUserId());
    taskEntity.setTopic(sendAwardMessageEvent.topic());
    taskEntity.setMessageId(sendAwardMessageEventMessage.getId());
    taskEntity.setMessage(sendAwardMessageEventMessage);
    taskEntity.setState(TaskStateVO.create);

    UserAwardRecordAggregate userAwardRecordAggregate = UserAwardRecordAggregate.builder()
      .taskEntity(taskEntity)
      .userAwardRecordEntity(userAwardRecordEntity)
      .build();

    // 存储聚合对象 - 一个事务下，用户的中奖记录
    awardRepository.saveUserAwardRecord(userAwardRecordAggregate);

  }

  @Override
  public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
    String awardKey = awardRepository.queryAwardKey(distributeAwardEntity.getAwardId());
    if (null == awardKey) {
      log.error("分发奖品，奖品ID不存在。awardKey:{}", awardKey);
      return;
    }

    // 这里就没有if/else的方式了，直接从map中取出对应的Bean对象(奖品对应的service会有不同的制度: 有的是随机积分, 有的是
    // 固定积分,或者单纯增加次数等). 代码的可读/扩展性都更好
    IDistributeAward distributeAward = distributeAwardMap.get(awardKey);
    if (null == distributeAward) {
      log.error("分发奖品，对应的服务不存在。awardKey:{}", awardKey);
      // todo: 后续完成所有service后再去掉抛异常的注释
//      throw new RuntimeException("分发奖品，奖品" + awardKey + "对应的服务不存在");
      return;
    }
    distributeAward.giveOutPrizes(distributeAwardEntity);


  }
}
