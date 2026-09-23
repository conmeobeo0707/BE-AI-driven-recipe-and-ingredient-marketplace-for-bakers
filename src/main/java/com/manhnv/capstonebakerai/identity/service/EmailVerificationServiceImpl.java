package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.MessageResponse;
import com.manhnv.capstonebakerai.identity.entity.VerificationCode;
import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.exception.InvalidVerificationCodeException;
import com.manhnv.capstonebakerai.identity.exception.VerificationEmailDeliveryException;
import com.manhnv.capstonebakerai.identity.repository.VerificationCodeRepository;
import com.manhnv.capstonebakerai.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String sender;
    private final Duration codeExpiration;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String sender,
            @Value("${app.verification-code-expiration}") Duration codeExpiration
    ) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.sender = sender;
        this.codeExpiration = codeExpiration;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendRegistrationCode(UUID userId) {
        userRepository.findById(userId)
                .filter(user -> !Boolean.TRUE.equals(user.getIsConfirmed()))
                .ifPresent(this::sendVerificationCode);
    }

    private void sendVerificationCode(User user) {
        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        VerificationCode verification = verificationCodeRepository
                .findByUserUserId(user.getUserId())
                .orElseGet(VerificationCode::new);
        verification.setUser(user);
        verification.setEmailVerificationToken(passwordEncoder.encode(code));
        verification.setCreatedAt(now);
        verification.setEmailVerificationTokenExpiryTime(now.plus(codeExpiration));
        verificationCodeRepository.saveAndFlush(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(user.getEmail());
        message.setSubject("Your email verification code");
        message.setText("Your verification code is %s. It expires in %d minutes."
                .formatted(code, codeExpiration.toMinutes()));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new VerificationEmailDeliveryException(exception);
        }
    }

    @Override
    @Transactional
    public MessageResponse verify(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(InvalidVerificationCodeException::new);

        if (Boolean.TRUE.equals(user.getIsConfirmed())) {
            return new MessageResponse("Email is already verified");
        }

        VerificationCode verification = verificationCodeRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(InvalidVerificationCodeException::new);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (verification.getEmailVerificationTokenExpiryTime().isBefore(now)
                || !passwordEncoder.matches(code, verification.getEmailVerificationToken())) {
            throw new InvalidVerificationCodeException();
        }

        user.setIsConfirmed(true);
        user.setUpdatedAt(now);
        userRepository.save(user);
        verificationCodeRepository.delete(verification);
        return new MessageResponse("Email verified successfully");
    }

    @Override
    @Transactional
    public MessageResponse resend(String email) {
        Optional<User> optionalUser =
                userRepository.findByEmailIgnoreCase(normalize(email));

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (!Boolean.TRUE.equals(user.getIsConfirmed())) {
                sendVerificationCode(user);
            }
        }

        return new MessageResponse(
                "If the account exists and is not verified, a new code has been sent"
        );
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
