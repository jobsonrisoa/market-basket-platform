package com.jobson.market.order_service.persistence;

import com.jobson.market.order_service.domain.OrderEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface OrderRepository extends Repository<OrderEntity, UUID> {

  <S extends OrderEntity> S save(S entity);

  Optional<OrderEntity> findById(UUID id);

  List<OrderEntity> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

  void deleteAll();
}
