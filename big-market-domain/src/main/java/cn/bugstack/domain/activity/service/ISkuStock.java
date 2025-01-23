package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import org.checkerframework.checker.units.qual.A;

// rabbitmq 消息发送的处理
public interface ISkuStock {

  /**
   * 获取活动sku库存消耗队列
   * @return 奖品库存Key信息
   * @throws InterruptedException 异常
   */
  ActivitySkuStockKeyVO takeQueueValue() throws InterruptedException;


  /**
   * 清空队列, 拿到rabbitmq的消息后
   */
  void clearQueueValue();


  /**
   * 延迟队列 + 任务趋势更新活动sku库存
   *
   * @param sku 活动商品
   */
  void updateActivitySkuStock(Long sku);

  /**
   * 缓存库存以消耗完毕，清空数据库库存
   *
   * @param sku 活动商品
   */
  void clearActivitySkuStock(Long sku);


}
