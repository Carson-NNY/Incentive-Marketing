package cn.bugstack.trigger.api.dto;

import lombok.Data;

@Data
public class SkuProductShopCartRequestDTO {

  private String userId;

  private Long sku;
}
