package cn.bugstack.domain.activity.service.quota.rule.factory;

import cn.bugstack.domain.activity.service.quota.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultActivityChainFactory {

  private final IActionChain actionChain;

  // 因为我们只有已知的两个责任链, 直接在这里就连接好
  public DefaultActivityChainFactory(Map<String, IActionChain> actionChainGroup) {
    this.actionChain = actionChainGroup.get(ActionModel.activity_base_action.getCode());
    actionChain.appendNext(actionChainGroup.get(ActionModel.activity_sku_stock_action.getCode()));
  }

  public IActionChain openActionChain() {
    return this.actionChain;
  }


  @Getter
  @AllArgsConstructor
  public enum ActionModel {

    activity_base_action("activity_base_action", "活动的时间, 状态校验"),
    activity_sku_stock_action("activity_sku_stock_action", "活动sku库存"),
    ;

    private final String code;
    private final String info;

  }

}
