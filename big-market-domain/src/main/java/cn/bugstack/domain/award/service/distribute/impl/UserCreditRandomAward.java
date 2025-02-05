package cn.bugstack.domain.award.service.distribute.impl;

import cn.bugstack.domain.award.model.aggregate.GiveOutPrizesAggregate;
import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;
import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.entity.UserCreditAwardEntity;
import cn.bugstack.domain.award.model.valobj.AwardStateVO;
import cn.bugstack.domain.award.repository.IAwardRepository;
import cn.bugstack.domain.award.service.distribute.IDistributeAward;
import cn.bugstack.types.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.MathContext;


@Component("user_credit_random")
// 这里使用@Component("user_credit_random")是因为设计模式:
// map<String, Bean> key
// 在我们一个distributeAward()的方法里, 我们利用: 奖品id -> awardKey -> 得到Bean对象(我们对应的service)!
public class UserCreditRandomAward implements IDistributeAward {

  @Resource
  private IAwardRepository repository;

  @Override
  public void giveOutPrizes(DistributeAwardEntity distributeAwardEntity) {
    Integer awardId = distributeAwardEntity.getAwardId();
    String awardConfig = distributeAwardEntity.getAwardConfig();

    if (StringUtils.isBlank(awardConfig)) {
      // awardConfig例子: 0.01,1  1,100
      awardConfig = repository.queryAwardConfig(awardId);
    }

    String[] creditRange = awardConfig.split(Constants.SPLIT);
    if (creditRange.length != 2) {
      throw new RuntimeException("award_config" + awardConfig + "配置不是一个范围值, 如 1,100");
    }

    BigDecimal creditAmount = generateRandom(new BigDecimal(creditRange[0]), new BigDecimal(creditRange[1]));

    // 构建聚合对象
    UserAwardRecordEntity userAwardRecordEntity = GiveOutPrizesAggregate.buildDistributeUserAwardRecordEntity(
        distributeAwardEntity.getUserId(),
        distributeAwardEntity.getOrderId(),
        distributeAwardEntity.getAwardId(),
        AwardStateVO.complete
    );

    UserCreditAwardEntity userCreditAwardEntity = GiveOutPrizesAggregate.buildUserCreditAwardEntity(distributeAwardEntity.getUserId(), creditAmount);

    GiveOutPrizesAggregate giveOutPrizesAggregate = new GiveOutPrizesAggregate();
    giveOutPrizesAggregate.setUserId(distributeAwardEntity.getUserId());
    giveOutPrizesAggregate.setUserAwardRecordEntity(userAwardRecordEntity);
    giveOutPrizesAggregate.setUserCreditAwardEntity(userCreditAwardEntity);

    // 存储发奖对象
    repository.saveGiveOutPrizesAggregate(giveOutPrizesAggregate);

  }

  private BigDecimal generateRandom(BigDecimal min, BigDecimal max) {
    if (min.equals(max)) return min;
    BigDecimal randomBigDecimal = min.add(BigDecimal.valueOf(Math.random()).multiply(max.subtract(min)));
    return randomBigDecimal.round(new MathContext(3));
  }
}
