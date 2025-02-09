package cn.bugstack.domain.credit.model.aggregate;

import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeAggregate {

  private String userId;

  private CreditAccountEntity creditAccountEntity;

  private CreditOrderEntity creditOrderEntity;

  public  static CreditAccountEntity createCreditAccountEntity(String userId, BigDecimal adjustAmount) {
    return CreditAccountEntity.builder().userId(userId).adjustAmount(adjustAmount).build();
  }

  public static CreditOrderEntity createCreditOrderEntity(String userId,
                                                          TradeNameVO tradeName,
                                                          TradeTypeVO tradeType,
                                                          BigDecimal tradeAmount,
                                                          String outBusinessNo) {
    return CreditOrderEntity.builder()
        .userId(userId)
        .orderId(RandomStringUtils.randomNumeric(12))
        .tradeName(tradeName).tradeType(tradeType)
        .tradeAmount(tradeAmount)
        .outBusinessNo(outBusinessNo)
        .build();
  }
}
