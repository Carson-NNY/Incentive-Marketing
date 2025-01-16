package cn.bugstack.trigger.http;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.trigger.api.IRaffleService;
import cn.bugstack.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.trigger.api.dto.RaffleRequestDTO;
import cn.bugstack.trigger.api.dto.RaffleResponseDTO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import cn.bugstack.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销抽奖服务
 * @create 2024-02-14 09:21
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}") // This annotation enables Cross-Origin Resource Sharing (CORS) for the controller. The value ${app.config.cross-origin} is a placeholder that refers to a property in the application.dev.yml file. In development, it is set to *, which allows requests from any origin.
@RequestMapping("/api/${app.config.api-version}/raffle/") // 这个是在big-market-app里面的application.dev.yml里面的配置
public class RaffleController implements IRaffleService {

  @Resource
  private IStrategyArmory strategyArmory;

  @Resource
  private IRaffleAward raffleAward;

  @Resource
  private IRaffleStrategy raffleStrategy;

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
   * @param requestDTO {"strategyId":1000001}
   * @return 奖品列表
   */
  @RequestMapping(value = "query_raffle_award_list", method = RequestMethod.POST)
  @Override
  public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(@RequestBody RaffleAwardListRequestDTO requestDTO) {
    try{
      log.info("查询抽奖奖品列表配开始 strategyId：{}", requestDTO.getStrategyId());
      List<StrategyAwardEntity> strategyAwardEntities = raffleAward.queryRaffleStrategyAwardList(requestDTO.getStrategyId());
      // 这里是把查询到的奖品列表转换成返回的DTO
      List<RaffleAwardListResponseDTO> raffleAwardListResponseDTOS = new ArrayList<>(strategyAwardEntities.size());
      for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
        raffleAwardListResponseDTOS.add(RaffleAwardListResponseDTO.builder()
            .awardId(strategyAward.getAwardId())
            .awardTitle(strategyAward.getAwardTitle())
            .awardSubtitle(strategyAward.getAwardSubtitle())
            .sort(strategyAward.getSort())
            .build());
      }
      Response<List<RaffleAwardListResponseDTO>> response = Response.<List<RaffleAwardListResponseDTO>>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(raffleAwardListResponseDTOS)
          .build();
      log.info("查询抽奖奖品列表配置完成 strategyId：{} response: {}", requestDTO.getStrategyId(), JSON.toJSONString(response));
      // 返回结果
      return response;
    } catch (Exception e) {
      log.error("查询抽奖奖品列表配置失败 strategyId：{}", requestDTO.getStrategyId(), e);
      return Response.<List<RaffleAwardListResponseDTO>>builder()
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
  public Response<RaffleResponseDTO> randomRaffle(@RequestBody RaffleRequestDTO requestDTO) {
    try {
      log.info("随机抽奖开始 strategyId: {}", requestDTO.getStrategyId());
      // 调用抽奖接口
      RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
          .userId("system")
          .strategyId(requestDTO.getStrategyId())
          .build());
      // 封装返回结果
      Response<RaffleResponseDTO> response = Response.<RaffleResponseDTO>builder()
          .code(ResponseCode.SUCCESS.getCode())
          .info(ResponseCode.SUCCESS.getInfo())
          .data(RaffleResponseDTO.builder()
              .awardId(raffleAwardEntity.getAwardId())
              .awardIndex(raffleAwardEntity.getSort())
              .build())
          .build();
      log.info("随机抽奖完成 strategyId: {} response: {}", requestDTO.getStrategyId(), JSON.toJSONString(response));
      return response;
    } catch (AppException e) {
      log.error("随机抽奖失败 strategyId：{} {}", requestDTO.getStrategyId(), e.getInfo());
      return Response.<RaffleResponseDTO>builder()
          .code(e.getCode())
          .info(e.getInfo())
          .build();
    } catch (Exception e) {
      log.error("随机抽奖失败 strategyId：{}", requestDTO.getStrategyId(), e);
      return Response.<RaffleResponseDTO>builder()
          .code(ResponseCode.UN_ERROR.getCode())
          .info(ResponseCode.UN_ERROR.getInfo())
          .build();
    }

  }
}
