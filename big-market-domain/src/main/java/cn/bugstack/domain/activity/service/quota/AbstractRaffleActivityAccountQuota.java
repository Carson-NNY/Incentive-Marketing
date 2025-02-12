package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import cn.bugstack.domain.activity.service.quota.rule.IActionChain;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖活动抽象类，定义标准的抽奖下单的流程。
 * @create 2024-03-16 08:42
 */
@Slf4j
public abstract class AbstractRaffleActivityAccountQuota extends RaffleActivityAccountQuotaSupport implements IRaffleActivityAccountQuotaService {

    // spring boot 会根据这个map来注入不同的策略impl:
    // 1. @Service("credit_pay_trade")
    // 2. @Service("rebate_no_pay_trade")
    private final Map<String, ITradePolicy> tradePolicyGroup;

    public AbstractRaffleActivityAccountQuota(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityRepository, defaultActivityChainFactory);
      this.tradePolicyGroup = tradePolicyGroup;
    }


    // note: 这个主要是用户来领取抽奖机会的地方(通过充值/每日签到 来增加总账户的额度)
    @Override
    public UnpaidActivityOrderEntity createOrder(SkuRechargeEntity skuRechargeEntity) {

        // 1. 参数校验
        String userId = skuRechargeEntity.getUserId();
        Long sku = skuRechargeEntity.getSku();
        String outBusinessNo = skuRechargeEntity.getOutBusinessNo();
        if (null == sku || StringUtils.isBlank(userId) || StringUtils.isBlank(outBusinessNo)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 由于存在并发带来的之前用户下了多个订单同时支付,有的订单支付成功,有的订单支付失败因为积分不足,这样导致了有的订单下单了但是还是未支付
        // 状态, 所以我们这里需要先查询是否有未支付订单
        UnpaidActivityOrderEntity unpaidActivityOrderEntity = activityRepository.queryUnpaidActivityOrder(skuRechargeEntity);
        if (null != unpaidActivityOrderEntity) return unpaidActivityOrderEntity;

        // 2. 查询基础信息
        // 2.1 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(sku);
        // 2.2 查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        // 2.3 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        // 2.4. 账户额度 【交易属性的兑换，需要校验额度账户】
        if (OrderTradeTypeVO.credit_pay_trade.equals(skuRechargeEntity.getOrderTradeType())){
            BigDecimal availableAmount = activityRepository.queryUserCreditAccountAmount(userId);
            if (availableAmount.compareTo(activitySkuEntity.getProductAmount()) < 0) {
                throw new AppException(ResponseCode.USER_CREDIT_ACCOUNT_NO_AVAILABLE_AMOUNT.getCode(), ResponseCode.USER_CREDIT_ACCOUNT_NO_AVAILABLE_AMOUNT.getInfo());
            }
        }

        // 3. 活动动作规则校验, 然后会执行一系列的动作(更新cache中的库存(减少), 更新减少数据库中sku的总库存(这个是我们公司给这个活动的库存预算))
        IActionChain actionChain = defaultActivityChainFactory.openActionChain();
        actionChain.action(activitySkuEntity, activityEntity, activityCountEntity);

        // 4. 构建订单聚合对象
        CreateQuotaOrderAggregate createOrderAggregate = buildOrderAggregate(skuRechargeEntity, activitySkuEntity, activityEntity, activityCountEntity);

        // 5. 保存订单流水
        // 这里通过了策略模式来实现不同的交易策略, 因为我们避免了大量的if else, 也方便了未来的扩展
        ITradePolicy tradePolicy = tradePolicyGroup.get(skuRechargeEntity.getOrderTradeType().getCode());
        //(这里面就是通过聚合对象对一个事务进行处理: 保存这个用户购买/抽取的抽奖订单,并且更新数据库用户account的quota(增加qupta))
        tradePolicy.trade(createOrderAggregate);

        // 6
        ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
        return UnpaidActivityOrderEntity.builder()
                .userId(userId)
                .orderId(activityOrderEntity.getOrderId())
                .outBusinessNo(outBusinessNo)
                .payAmount(activityOrderEntity.getPayAmount())
                .build();
    }

    protected abstract CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);



}
