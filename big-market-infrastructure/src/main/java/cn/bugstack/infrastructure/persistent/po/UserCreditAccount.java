package cn.bugstack.infrastructure.persistent.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserCreditAccount {
  private Long id;
  private String userId;
  // 总积分
  private BigDecimal totalAmount;
  // 可用积分
  private BigDecimal availableAmount;
  /** 账户状态【open - 可用，close - 冻结】 */
  private String accountStatus;
  private Date createTime;
  private Date updateTime;

}
