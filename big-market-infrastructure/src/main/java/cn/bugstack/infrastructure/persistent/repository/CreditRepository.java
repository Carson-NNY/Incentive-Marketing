package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TaskEntity;
import cn.bugstack.domain.credit.repository.ICreditRepository;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.persistent.dao.ITaskDao;
import cn.bugstack.infrastructure.persistent.dao.IUserCreditAccountDao;
import cn.bugstack.infrastructure.persistent.dao.IUserCreditOrderDao;
import cn.bugstack.infrastructure.persistent.po.Task;
import cn.bugstack.infrastructure.persistent.po.UserCreditAccount;
import cn.bugstack.infrastructure.persistent.po.UserCreditOrder;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class CreditRepository implements ICreditRepository {

  @Resource
  private IRedisService redisService;
  @Resource
  private IUserCreditAccountDao userCreditAccountDao;
  @Resource
  private IUserCreditOrderDao userCreditOrderDao;
  @Resource
  private ITaskDao taskDao;
  @Resource
  private IDBRouterStrategy dbRouter;
  @Resource
  private TransactionTemplate transactionTemplate;
  @Autowired
  private EventPublisher eventPublisher;


  @Override
  public void saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
    String userId = tradeAggregate.getUserId();
    CreditAccountEntity creditAccountEntity = tradeAggregate.getCreditAccountEntity();
    CreditOrderEntity creditOrderEntity = tradeAggregate.getCreditOrderEntity();
    TaskEntity taskEntity = tradeAggregate.getTaskEntity();

    // 积分账户
    UserCreditAccount userCreditAccountReq = new UserCreditAccount();
    userCreditAccountReq.setUserId(userId);
    userCreditAccountReq.setTotalAmount(creditAccountEntity.getAdjustAmount());
    // 知识；仓储往上有业务语义，仓储往下到 dao 操作是没有业务语义的。所以不用在乎这块使用的字段名称，直接用持久化对象即可。
    userCreditAccountReq.setAvailableAmount(creditAccountEntity.getAdjustAmount());

    // 积分订单
    UserCreditOrder userCreditOrderReq = new UserCreditOrder();
    userCreditOrderReq.setUserId(creditOrderEntity.getUserId());
    userCreditOrderReq.setOrderId(creditOrderEntity.getOrderId());
    userCreditOrderReq.setTradeName(creditOrderEntity.getTradeName().getName());
    userCreditOrderReq.setTradeType(creditOrderEntity.getTradeType().getCode());
    userCreditOrderReq.setTradeAmount(creditOrderEntity.getTradeAmount());
    userCreditOrderReq.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());

    Task task = new Task();
    task.setUserId(taskEntity.getUserId());
    task.setTopic(taskEntity.getTopic());
    task.setMessageId(taskEntity.getMessageId());
    task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
    task.setState(taskEntity.getState().getCode());

    RLock lock = redisService.getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + userId + Constants.UNDERLINE + creditOrderEntity.getOutBusinessNo());

    try{
      lock.lock(3, TimeUnit.SECONDS);
      dbRouter.doRouter(userId);
      transactionTemplate.execute(status ->{
        try{
          UserCreditAccount userCreditAccount = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
          if(userCreditAccount == null){
            userCreditAccountDao.insert(userCreditAccountReq);
          } else {
            userCreditAccountDao.updateAddAmount(userCreditAccountReq);
          }

          userCreditOrderDao.insert(userCreditOrderReq);
          // 保存任务
          taskDao.insert(task);
        }catch(DuplicateKeyException e) {
          status.setRollbackOnly();
          log.error("调整账户积分额度异常，唯一索引冲突 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
        }catch (Exception e){
          status.setRollbackOnly();
          log.error("调整账户积分额度失败 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
        }
        return 1;
      });

    } finally {
      dbRouter.clear();
      lock.unlock();
    }

    try {
      eventPublisher.publish(task.getTopic(), task.getMessage());
      taskDao.updateTaskSendMessageCompleted(task);
      log.info("调整账户积分记录，发送MQ消息完成 userId: {} orderId:{} topic: {}", userId, creditOrderEntity.getOrderId(), task.getTopic());
    } catch (Exception e) {
      log.error("调整账户积分记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
      taskDao.updateTaskSendMessageFail(task);
    }

  }
}
