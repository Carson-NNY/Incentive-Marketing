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
    STRATEGY_RULE_WEIGHT_IS_NULL("ERR_BIZ_001", "Business exception: The rule_weight weight rule in the strategy rule has been applied but not configured."),
    ;



    private String code;
    private String info;

}
