package cn.bugstack.domain.award.model.aggregate;

import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.entity.UserCreditAwardEntity;
import cn.bugstack.domain.award.model.valobj.AwardStateVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 发送奖品聚合对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiveOutPrizesAggregate {

  private String userId;
  /** 用户发奖记录 */
  private UserAwardRecordEntity userAwardRecordEntity;
  /** 用户 积分奖品 */
  private UserCreditAwardEntity userCreditAwardEntity;

  public static UserAwardRecordEntity buildDistributeUserAwardRecordEntity(String userId, String orderId, Integer awardId, AwardStateVO awardState) {
    return UserAwardRecordEntity.builder()
        .userId(userId)
        .orderId(orderId)
        .awardId(awardId)
        .awardState(awardState)
        .build();
  }

  public static UserCreditAwardEntity buildUserCreditAwardEntity(String userId, BigDecimal creditAmount) {
    return UserCreditAwardEntity.builder()
        .userId(userId)
        .creditAmount(creditAmount)
        .build();
  }
}
