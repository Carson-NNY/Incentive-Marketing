package cn.bugstack.domain.rebate.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RebateTypeVO {

  SKU("sku", "活动库存充值商品"),
  INTEGER("integer", "用户活动积分"),
  ;

  private final String code;
  private final String info;

}
