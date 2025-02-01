package cn.bugstack.domain.rebate.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 日常行为返利配置值对象
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyBehaviorRebateVO {

  /** 类型: sign 签到, openai_pay 支付 */
  private String behaviorType;
  private String rebateDesc;
  /** 返利类型（sku 活动库存充值商品、integral 用户活动积分） */
  private String rebateType;
  /** 返利配置 */
  private String rebateConfig;
}
