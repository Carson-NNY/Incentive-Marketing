package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.SkuProductEntity;

import java.util.List;

public interface IRaffleActivitySkuProductService {

  List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);
}
