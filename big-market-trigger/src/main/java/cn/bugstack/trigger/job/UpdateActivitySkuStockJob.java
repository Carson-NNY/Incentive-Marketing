package cn.bugstack.trigger.job;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.service.ISkuStock;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component()
public class UpdateActivitySkuStockJob {

  @Resource
  private ISkuStock skuStock;

  //The @Scheduled(cron = "0/5 * * * * ?") schedules the exec method to run every 5 seconds.
  //This method acts as a polling mechanism to check for new tasks in the queue (takeQueueValue) and processes them.
  @Scheduled(cron = "0/5 * * * * ?")
  public void exec() {
    try {
      log.info("定时任务，更新活动sku库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
      ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
      if (null == activitySkuStockKeyVO) return;
      log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
      skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
    } catch (Exception e) {
      log.error("定时任务，更新活动sku库存失败", e);
    }
  }
}
