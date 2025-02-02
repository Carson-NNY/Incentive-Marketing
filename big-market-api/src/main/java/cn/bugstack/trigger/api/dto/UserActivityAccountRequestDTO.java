package cn.bugstack.trigger.api.dto;

import lombok.Data;

@Data
public class UserActivityAccountRequestDTO {

  private String userId;
  private Long activityId;
}
