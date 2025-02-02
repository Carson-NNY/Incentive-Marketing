package cn.bugstack.trigger.api;

import cn.bugstack.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRuleWeightRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRuleWeightResponseDTO;
import cn.bugstack.types.model.Response;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖服务接口
 * @create 2024-02-14 09:33
 */
public interface IRaffleStrategyService {

  /**
   * 策略装配接口
   * @param strategyId
   * @return 装备结果
   */
  Response<Boolean> strategyArmory(Long strategyId);

  /**
   * param我们定义为一个对象原因是因为为了scalibility and maintainability, 后期我们需要增加值或者改变某些值的时候，我们只需要改变这个对象的属性即可
   * @return
   */
  Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO requestDTO);

  Response<List<RaffleStrategyRuleWeightResponseDTO>> queryRaffleStrategyRuleWeight(RaffleStrategyRuleWeightRequestDTO requestDTO);

  /**
   * 随机抽奖接口
   *
   * @param requestDTO 请求参数
   * @return 抽奖结果
   */
  Response<RaffleStrategyResponseDTO> randomRaffle(RaffleStrategyRequestDTO requestDTO);


}
