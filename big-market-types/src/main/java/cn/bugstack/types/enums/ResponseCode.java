package cn.bugstack.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "success"),
    UN_ERROR("0001", "unknown failure"),
    ILLEGAL_PARAMETER("0002", "invalid params"),
    INDEX_DUP("0003", "唯一索引冲突"),
    STRATEGY_RULE_WEIGHT_IS_NULL("ERR_BIZ_001", "Business exception: The rule_weight weight rule in the strategy rule has been applied but not configured."),
    UN_ASSEMBLED_STRATEGY_ARMORY("ERR_BIZ_002", "抽奖策略配置未装配，请通过IStrategyArmory完成装配"),
    ACTIVITY_STATE_ERROR("ERR_BIZ_003", "活动未开启（非open状态）"),
    ACTIVITY_DATE_ERROR("ERR_BIZ_004", "非活动日期范围"),
    ACTIVITY_SKU_STOCK_ERROR("ERR_BIZ_005", "活动库存不足"),
    ;



    private String code;
    private String info;

}
