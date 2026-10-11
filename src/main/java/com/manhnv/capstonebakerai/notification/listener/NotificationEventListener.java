package com.manhnv.capstonebakerai.notification.listener;

import com.manhnv.capstonebakerai.notification.service.NotificationService;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void onOrderCreated(OrderCreatedEvent e) {                 // SY-3
//        List<UUID> staffIds = userRepository.findIdsByRoleIn(List.of("MANAGER", "STAFF"));
//        notificationService.createForUsers(staffIds,
//                "Đơn hàng mới", "Có đơn hàng mới cần xử lý",
//                NotificationRefType.ORDER, e.orderId());
//    }
//
//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void onOrderStatusChanged(OrderStatusChangedEvent e) {     // SY-2
//        notificationService.create(e.customerUserId(),
//                "Cập nhật đơn hàng", "Đơn hàng của bạn đã chuyển sang " + e.newStatus(),
//                NotificationRefType.ORDER, e.orderId());
//    }
//
//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void onCommentCreated(CommentCreatedEvent e) {             // SY-4 và SY-6
//        // SY-6: reply -> báo cho chủ comment cha
//        if (e.parentCommentOwnerId() != null) {
//            if (!e.parentCommentOwnerId().equals(e.commenterId())) {
//                notificationService.create(e.parentCommentOwnerId(),
//                        "Có phản hồi mới", "Công thức \"" + e.recipeTitle() + "\" có phản hồi cho bình luận của bạn",
//                        NotificationRefType.COMMENT, e.commentId());
//            }
//            return;
//        }
//        // SY-4: comment gốc -> báo cho chủ recipe (trừ khi tự comment)
//        if (!e.recipeOwnerId().equals(e.commenterId())) {
//            notificationService.create(e.recipeOwnerId(),
//                    "Bình luận mới", "Công thức \"" + e.recipeTitle() + "\" có bình luận mới",
//                    NotificationRefType.COMMENT, e.commentId());
//        }
//    }
//
//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void onRatingCreated(RatingCreatedEvent e) {               // SY-5
//        if (e.recipeOwnerId().equals(e.raterUserId())) return;
//        notificationService.create(e.recipeOwnerId(),
//                "Đánh giá mới", "Công thức \"" + e.recipeTitle() + "\" nhận " + e.score() + " sao",
//                NotificationRefType.RATING, e.ratingId());
//    }
}
