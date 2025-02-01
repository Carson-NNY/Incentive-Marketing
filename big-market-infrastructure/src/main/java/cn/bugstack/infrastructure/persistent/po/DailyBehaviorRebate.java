package cn.bugstack.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

@Data
public class DailyBehaviorRebate {

  private Long id;
  /** 类型: sign 签到, openai_pay 支付 */
  private String behaviorType;
  private String rebateDesc;
  /** 返利类型（sku 活动库存充值商品、integral 用户活动积分） */
  private String rebateType;
  /** 返利配置 */
  private String rebateConfig;
  /** 状态（open 开启、close 关闭） */
  private String state;
  private Date createTime;
  private Date updateTime;

}
