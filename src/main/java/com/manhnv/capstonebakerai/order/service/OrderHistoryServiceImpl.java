package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.order.dto.response.OrderHistoryResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderHistoryStaffResponse;
import com.manhnv.capstonebakerai.order.entity.OrderHistory;
import com.manhnv.capstonebakerai.order.enums.OrderHistoryAction;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.mapper.OrderMapper;
import com.manhnv.capstonebakerai.order.repository.OrderHistoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderHistoryServiceImpl implements OrderHistoryService {

    OrderHistoryRepository repository;
    OrderActorResolver actorResolver;
    OrderMapper mapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID orderId, OrderHistoryAction action, OrderStatus from, OrderStatus to, String note) {
        OrderActor actor = actorResolver.current();
        repository.save(OrderHistory.builder()
                .orderId(orderId)
                .action(action)
                .fromStatus(from)
                .toStatus(to)
                .actorType(actor.type())
                .actorId(actor.userId())
                .note(note)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getForCustomer(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(mapper::toHistoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryStaffResponse> getForStaff(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(mapper::toHistoryStaffResponse).toList();
    }
}
