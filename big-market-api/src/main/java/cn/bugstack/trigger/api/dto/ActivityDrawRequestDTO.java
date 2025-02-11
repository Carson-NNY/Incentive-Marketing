package cn.bugstack.trigger.api.dto;

import lombok.Data;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动抽奖请求对象
 * @create 2024-04-13 09:29
 */

// 我们把api层的对象和domain中的各种对象分开，这样可以更好的控制各层的依赖关系,防止api接口更新
//然后导致domain中的对象也要更新，这样就会导致domain中的对象被污染

@Data
public class ActivityDrawRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
