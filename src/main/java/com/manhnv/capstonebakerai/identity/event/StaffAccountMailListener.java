package com.manhnv.capstonebakerai.identity.event;

import com.manhnv.capstonebakerai.user.event.StaffAccountCreatedEvent;
import com.manhnv.capstonebakerai.identity.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StaffAccountMailListener {

    private static final Logger log = LoggerFactory.getLogger(StaffAccountMailListener.class);

    private final EmailTemplateService emailTemplateService;

    @Value("${app.password-reset.code-expiration}")
    private Duration codeExpiration;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStaffAccountCreated(StaffAccountCreatedEvent event) {
        String plainText = """
                Xin chào %s,

                Quản trị viên đã tạo tài khoản cho bạn trên hệ thống Baker AI.

                Email đăng nhập: %s
                Vai trò: %s

                Bạn cần tự thiết lập mật khẩu trước khi đăng nhập. Quản trị viên không được cung cấp mật khẩu của bạn.

                Hướng dẫn thiết lập mật khẩu:

                1. Mở màn hình đăng nhập của hệ thống và chọn chức năng Quên mật khẩu.
                2. Nhập email đăng nhập ở trên và yêu cầu gửi mã xác nhận.
                3. Kiểm tra email để nhận mã xác nhận 6 chữ số. Mã có hiệu lực trong %d phút kể từ khi được tạo.
                4. Nhập mã xác nhận và đặt mật khẩu mới theo hướng dẫn trên màn hình.
                5. Sau khi thiết lập thành công, đăng nhập bằng email ở trên và mật khẩu bạn vừa đặt.

                Nếu chưa nhận được mã, kiểm tra thư mục Spam hoặc yêu cầu gửi lại mã sau 1 phút.
                """.formatted(
                event.fullName(),
                event.email(),
                event.role(),
                codeExpiration.toMinutes()
        );

        try {
            emailTemplateService.send(
                    event.email(),
                    "Tài khoản Baker AI của bạn đã được tạo",
                    "staff-account-created.html",
                    plainText,
                    Map.of(
                            "fullName", event.fullName(),
                            "email", event.email(),
                            "role", event.role().name(),
                            "expirationMinutes", String.valueOf(codeExpiration.toMinutes())
                    )
            );
        } catch (RuntimeException exception) {
            log.error("Staff account email failed for user {}; the account can still use password reset. Failure type: {}",
                    event.userId(), exception.getClass().getSimpleName());
        }
    }
}
