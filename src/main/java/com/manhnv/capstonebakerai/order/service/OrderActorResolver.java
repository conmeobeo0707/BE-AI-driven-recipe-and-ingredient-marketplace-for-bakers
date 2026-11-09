package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.enums.OrderActorType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderActorResolver {

    public OrderActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return OrderActor.system();
        }

        UUID userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return OrderActor.system();
        }

        boolean isCustomer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        return new OrderActor(isCustomer ? OrderActorType.CUSTOMER : OrderActorType.STAFF, userId);
    }
}
