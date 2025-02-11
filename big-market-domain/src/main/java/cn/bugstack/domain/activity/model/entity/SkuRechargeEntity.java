package cn.bugstack.domain.activity.model.entity;

import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import lombok.Data;

@Data
public class SkuRechargeEntity {

  /** 用户ID */
  private String userId;
  /** 商品SKU - activity + activity count */
  private Long sku;

  /** 防重id */
  private String outBusinessNo;

  // 凡是sku, 我们都默认是返利不支付的交易(通过签到获得的商品)
  private OrderTradeTypeVO orderTradeType = OrderTradeTypeVO.rebate_no_pay_trade;
}
