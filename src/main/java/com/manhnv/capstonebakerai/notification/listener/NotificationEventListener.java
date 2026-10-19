package com.manhnv.capstonebakerai.notification.listener;

import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import com.manhnv.capstonebakerai.notification.service.NotificationService;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.event.OrderCreatedEvent;
import com.manhnv.capstonebakerai.order.event.OrderStatusChangedEvent;
import com.manhnv.capstonebakerai.recipe.event.CommentCreatedEvent;
import com.manhnv.capstonebakerai.recipe.event.RatingCreatedEvent;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedEvent e) {
        List<UUID> staffIds = findStaffIds();
        if (staffIds.isEmpty()) return;
        notificationService.createForUsers(staffIds,
                "Đơn hàng mới",
                "Có đơn hàng " + e.orderCode() + " cần xử lý",
                NotificationRefType.ORDER,
                e.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusChangedEvent e) {
        if (e.newStatus() == OrderStatus.CANCELLED && e.cancelledByCustomer()) {
            List<UUID> staffIds = findStaffIds();
            if (staffIds.isEmpty()) return;
            notificationService.createForUsers(staffIds,
                    "Khách hủy đơn",
                    "Đơn hàng " + e.orderCode() + " đã bị khách hàng hủy",
                    NotificationRefType.ORDER,
                    e.orderId());
            return;
        }
        notificationService.create(e.customerUserId(),
                "Cập nhật đơn hàng",
                "Đơn hàng " + e.orderCode() + " " + describe(e.newStatus()),
                NotificationRefType.ORDER, e.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent e) {
        String title = shorten(e.recipeTitle());

        // Comment gốc -> báo chủ công thức (trừ khi tự comment)
        UUID parentOwner = e.parentCommentOwnerId();
        if (parentOwner == null) {
            if (!e.recipeOwnerId().equals(e.commenterId())) {
                notificationService.create(e.recipeOwnerId(),
                        "Bình luận mới",
                        "Công thức \"" + title + "\" có bình luận mới",
                        NotificationRefType.COMMENT, e.commentId());
            }
            return;
        }

        // Reply -> báo chủ comment cha
        if (!parentOwner.equals(e.commenterId())) {
            notificationService.create(parentOwner,
                    "Có phản hồi mới",
                    "Công thức \"" + title + "\" có phản hồi cho bình luận của bạn",
                    NotificationRefType.COMMENT, e.commentId());
        }

        // Reply -> báo thêm chủ công thức, nếu:
        //  - không phải người đang reply
        //  - không trùng chủ comment cha (đã được báo ở trên)
        UUID recipeOwner = e.recipeOwnerId();
        if (!recipeOwner.equals(e.commenterId()) && !recipeOwner.equals(parentOwner)) {
            notificationService.create(recipeOwner,
                    "Bình luận mới",
                    "Công thức \"" + title + "\" có phản hồi mới trong phần bình luận",
                    NotificationRefType.COMMENT, e.commentId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRatingCreated(RatingCreatedEvent e) {
        if (e.recipeOwnerId() == null || e.recipeOwnerId().equals(e.raterUserId())) return;
        notificationService.create(e.recipeOwnerId(),
                "Đánh giá mới",
                "Công thức \"" + shorten(e.recipeTitle()) + "\" nhận " + e.score() + " sao",
                NotificationRefType.RATING, e.ratingId()
        );
    }


    private String shorten(String s) {
        if (s == null) return "";
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    private String describe(OrderStatus s) {
        return switch (s) {
            case PENDING -> "đã được ghi nhận và đang chờ xác nhận";
            case CONFIRMED -> "đã được xác nhận";
            case DELIVERED -> "đã giao thành công";
            case CANCELLED -> "đã bị hủy";
            default -> "đã chuyển sang trạng thái " + s.name();
        };
    }

    private List<UUID> findStaffIds() {
        return userRepository
                .findByRoleInOrderByCreatedAtDescUserIdAsc(List.of(Role.MANAGER, Role.STAFF))
                .stream()
                .map(User::getUserId)
                .toList();
    }

}