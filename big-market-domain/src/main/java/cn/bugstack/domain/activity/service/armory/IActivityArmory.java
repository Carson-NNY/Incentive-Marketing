package cn.bugstack.domain.activity.service.armory;

public interface IActivityArmory {

  // 一个activityId可能对应多个sku
  boolean assembleActivitySkuByActivityId(Long activityId);

  boolean assembleActivitySku(Long sku);
}
