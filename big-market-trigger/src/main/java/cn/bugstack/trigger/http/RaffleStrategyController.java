package cn.bugstack.trigger.http;

import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleWeightVO;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleRule;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.trigger.api.IRaffleStrategyService;
import cn.bugstack.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRuleWeightRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleStrategyRuleWeightResponseDTO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import cn.bugstack.types.model.Response;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销抽奖服务
 * @create 2024-02-14 09:21
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}") // This annotation enables Cross-Origin Resource Sharing (CORS) for the controller. The value ${app.config.cross-origin} is a placeholder that refers to a property in the application.dev.yml file. In development, it is set to *, which allows requests from any origin.
@RequestMapping("/api/${app.config.api-version}/raffle/strategy") // 这个是在big-market-app里面的application.dev.yml里面的配置
public class RaffleStrategyController implements IRaffleStrategyService {

  @Resource
  private IStrategyArmory strategyArmory;

  @Resource
  private IRaffleAward raffleAward;

  @Resource
  private IRaffleStrategy raffleStrategy;

  @Resource
  private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

  @Resource
  private IRaffleRule raffleRule;

  /**
   * 策略装配，将策略信息装配到缓存中
   * <a href="http://localhost:8091/api/v1/raffle/strategy_armory">/api/v1/raffle/strategy_armory</a>
   *
   * @param strategyId 策略ID
   * @return 装配结果
   */
  @RequestMapping(value = "strategy_armory", method = RequestMethod.GET)
  @Override
  public Response<Boolean> strategyArmory(Long strategyId) {
    try{
      log.info("抽奖策略装配开始 strategyId:{}", strategyId);
      boolean armoryStatus = strategyArmory.assembleLotteryStrategy(strategyId);
      Response<Boolean> response = Response.<Boolean>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(armoryStatus)
          .build();
      log.info("抽奖策略装配完成 strategyId：{} response: {}", strategyId, JSON.toJSONString(response));
      return response;
    }catch (Exception e){
      log.error("抽奖策略装配异常 strategyId:{}", strategyId);
      return Response.<Boolean>builder()
          .code(ResponseCode.UN_ERROR.getCode())
          .info(ResponseCode.UN_ERROR.getInfo())
          .build();
    }
  }


  /**
   * 查询奖品列表
   * <a href="http://localhost:8091/api/v1/raffle/query_raffle_award_list">/api/v1/raffle/query_raffle_award_list</a>
   * 请求参数 raw json
   *In this context, a POST method is used instead of a GET for several reasons, all related to the nature of the data being handled and the design requirements of the API:
   *  Request Contains a Body: The queryRaffleAwardList method requires a RaffleAwardListRequestDTO object to be sent in the request body.
   * @param request {"strategyId":1000001}
   * @return 奖品列表
   */
  @RequestMapping(value = "query_raffle_award_list", method = RequestMethod.POST)
  @Override
  public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(@RequestBody RaffleAwardListRequestDTO request) {
    try{
      log.info("查询抽奖奖品列表配开始 userId:{} activityId：{}", request.getUserId(), request.getActivityId());
      // 1. 参数校验
      if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
        throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
      }

      // 2. 查询奖品配置
      List<StrategyAwardEntity> strategyAwardEntities = raffleAward.queryRaffleStrategyAwardListByActivityId(request.getActivityId());
      // 3. 获取规则配置 (rule_models就是 tree的id)
      String[] treeIds = strategyAwardEntities.stream()
          .map(StrategyAwardEntity::getRuleModels)
          .filter(ruleModel -> ruleModel != null && !ruleModel.isEmpty())
          .toArray(String[]::new);

      // 4. 查询规则配置 - 获取奖品的解锁限制, 抽奖N次后解锁
      Map<String, Integer> ruleLockCountMap = raffleRule.queryAwardRuleLockCount(treeIds);

      // 5. 查询抽奖次数: 用户已经参与抽奖的次数
      Integer dayPartakeCount = raffleActivityAccountQuotaService.queryRaffleActivityAccountDayPartakeCount(request.getActivityId(), request.getUserId());
      // 6. 遍历填充数据
      // 这里是把查询到的奖品列表转换成返回的DTO
      List<RaffleAwardListResponseDTO> raffleAwardListResponseDTOS = new ArrayList<>(strategyAwardEntities.size());
      for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
        Integer awardRuleLockCount = ruleLockCountMap.get(strategyAward.getRuleModels());
        raffleAwardListResponseDTOS.add(RaffleAwardListResponseDTO.builder()
            .awardId(strategyAward.getAwardId())
            .awardTitle(strategyAward.getAwardTitle())
            .awardSubtitle(strategyAward.getAwardSubtitle())
            .sort(strategyAward.getSort())
            .awardRuleLockCount(awardRuleLockCount)
            .isAwardUnlock(null == awardRuleLockCount || dayPartakeCount >= awardRuleLockCount)
            .waitUnlockCount(null == awardRuleLockCount || awardRuleLockCount <= dayPartakeCount ? 0 : awardRuleLockCount - dayPartakeCount)
            .build());
      }
      Response<List<RaffleAwardListResponseDTO>> response = Response.<List<RaffleAwardListResponseDTO>>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(raffleAwardListResponseDTOS)
          .build();
      log.info("查询抽奖奖品列表配置完成 userId:{} activityId：{} response:{}", request.getUserId(), request.getActivityId(), JSON.toJSONString(response));
      // 返回结果
      return response;
    } catch (Exception e) {
      log.error("查询抽奖奖品列表配置失败 userId:{} activityId：{}", request.getUserId(), request.getActivityId(), e);
      return Response.<List<RaffleAwardListResponseDTO>>builder()
          .code(ResponseCode.UN_ERROR.getCode())
          .info(ResponseCode.UN_ERROR.getInfo())
          .build();
    }
  }

  /**
   * &#x67E5;&#x8BE2;&#x62BD;&#x5956;&#x7B56;&#x7565;&#x6743;&#x91CD;&#x89C4;&#x5219;&#x914D;&#x7F6E;
   * curl --request POST \
   * --url http://localhost:8091/api/v1/raffle/strategy/query_raffle_strategy_rule_weight \
   * --header 'content-type: application/json' \
   * --data '{
   * "userId":"xiaofuge",
   * "activityId": 100301
   * }'
   */
  @RequestMapping(value = "query_raffle_strategy_rule_weight", method = RequestMethod.POST)
  @Override
  public Response<List<RaffleStrategyRuleWeightResponseDTO>> queryRaffleStrategyRuleWeight(RaffleStrategyRuleWeightRequestDTO request) {

    try {
      log.info("查询抽奖策略权重规则配置开始 userId:{} activityId：{}", request.getUserId(), request.getActivityId());
      // 1. 参数校验
      if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
        throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
      }
      // 2. 查询用户抽奖总次数
      Integer userActivityAccountTotalUseCount = raffleActivityAccountQuotaService.queryRaffleActivityAccountPartakeCount(request.getActivityId(), request.getUserId());

      List<RaffleStrategyRuleWeightResponseDTO> raffleStrategyRuleWeightList = new ArrayList<>();
      List<RuleWeightVO> ruleWeightVOList = raffleRule.queryAwardRuleWeightByActivityId(request.getActivityId());
      // 转换对象
      for (RuleWeightVO ruleWeightVO : ruleWeightVOList) {
       List<RaffleStrategyRuleWeightResponseDTO.StrategyAward> strategyAwards = new ArrayList<>();
       List<RuleWeightVO.Award> awardList = ruleWeightVO.getAwardList();
       for (RuleWeightVO.Award award : awardList) {
         RaffleStrategyRuleWeightResponseDTO.StrategyAward strategyAward = new RaffleStrategyRuleWeightResponseDTO.StrategyAward();
         strategyAward.setAwardId(award.getAwardId());
         strategyAward.setAwardTitle(award.getAwardTitle());
         strategyAwards.add(strategyAward);
       }

       // 封装对象
       RaffleStrategyRuleWeightResponseDTO raffleStrategyRuleWeightResponseDTO = new RaffleStrategyRuleWeightResponseDTO();
       raffleStrategyRuleWeightResponseDTO.setRuleWeightCount(ruleWeightVO.getWeight());
       raffleStrategyRuleWeightResponseDTO.setStrategyAwards(strategyAwards);
       raffleStrategyRuleWeightResponseDTO.setUserActivityAccountTotalUseCount(userActivityAccountTotalUseCount);

       raffleStrategyRuleWeightList.add(raffleStrategyRuleWeightResponseDTO);
      }
      Response<List<RaffleStrategyRuleWeightResponseDTO>> response = Response.<List<RaffleStrategyRuleWeightResponseDTO>>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(raffleStrategyRuleWeightList)
          .build();
      log.info("查询抽奖策略权重规则配置完成 userId:{} activityId：{} response: {}", request.getUserId(), request.getActivityId(), JSON.toJSONString(response));
      return response;
    } catch (Exception e) {
      log.error("查询抽奖策略权重规则配置失败 userId:{} activityId：{}", request.getUserId(), request.getActivityId(), e);
      return Response.<List<RaffleStrategyRuleWeightResponseDTO>>builder()
          .code(ResponseCode.UN_ERROR.getCode())
          .info(ResponseCode.UN_ERROR.getInfo())
          .build();
    }
  }

  /**
   * 随机抽奖接口
   * <a href="http://localhost:8091/api/v1/raffle/random_raffle">/api/v1/raffle/random_raffle</a>
   *
   * @param requestDTO 请求参数 {"strategyId":1000001}
   * @return 抽奖结果
   */
  @RequestMapping(value = "random_raffle", method = RequestMethod.POST)
  @Override
  public Response<RaffleStrategyResponseDTO> randomRaffle(@RequestBody RaffleStrategyRequestDTO requestDTO) {
    try {
      log.info("随机抽奖开始 strategyId: {}", requestDTO.getStrategyId());
      // 调用抽奖接口
      RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
          .userId("system")
          .strategyId(requestDTO.getStrategyId())
          .build());
      // 封装返回结果
      Response<RaffleStrategyResponseDTO> response = Response.<RaffleStrategyResponseDTO>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(RaffleStrategyResponseDTO.builder()
              .awardId(raffleAwardEntity.getAwardId())
              .awardIndex(raffleAwardEntity.getSort())
              .build())
          .build();
      log.info("随机抽奖完成 strategyId: {} response: {}", requestDTO.getStrategyId(), JSON.toJSONString(response));
      return response;
    } catch (AppException e) {
      log.error("随机抽奖失败 strategyId：{} {}", requestDTO.getStrategyId(), e.getInfo());
      return Response.<RaffleStrategyResponseDTO>builder()
          .code(e.getCode())
          .info(e.getInfo())
          .build();
    } catch (Exception e) {
      log.error("随机抽奖失败 strategyId：{}", requestDTO.getStrategyId(), e);
      return Response.<RaffleStrategyResponseDTO>builder()
          .code(ResponseCode.UN_ERROR.getCode())
          .info(ResponseCode.UN_ERROR.getInfo())
          .build();
    }

  }
}
