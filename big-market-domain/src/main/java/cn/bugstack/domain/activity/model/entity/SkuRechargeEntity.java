package cn.bugstack.domain.activity.model.entity;

import lombok.Data;

@Data
public class SkuRechargeEntity {

  /** 用户ID */
  private String userId;
  /** 商品SKU - activity + activity count */
  private Long sku;

  /** 防重id */
  private String outBusinessNo;
}
