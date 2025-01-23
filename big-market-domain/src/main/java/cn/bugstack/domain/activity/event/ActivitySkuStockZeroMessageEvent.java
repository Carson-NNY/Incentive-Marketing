package cn.bugstack.domain.activity.event;

import cn.bugstack.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动sku库存清空消息
 * @create 2024-03-30 12:43
 */
@Component
public class ActivitySkuStockZeroMessageEvent extends BaseEvent<Long> {


  // 这里的配置value是和 big-market-app里的application-dev.yml里的配置对应的
  @Value("${spring.rabbitmq.topic.activity_sku_stock_zero}")
  private String topic;

  @Override
  public EventMessage<Long> buildEventMessage(Long sku) {
    // rabbitmq消息发送的统一格式
    return EventMessage.<Long>builder()
        .id(RandomStringUtils.randomNumeric(11))
        .timestamp(new Date())
        .data(sku)
        .build();
  }

  @Override
  public String topic() {
    return topic;
  }
}
