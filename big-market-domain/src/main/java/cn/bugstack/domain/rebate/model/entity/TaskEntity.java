package cn.bugstack.domain.rebate.model.entity;

import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.valobj.TaskStateVO;
import cn.bugstack.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {

  private String userId;

  private String topic;

  private String messageId;

  private BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> message;

  private TaskStateVO state;
}
