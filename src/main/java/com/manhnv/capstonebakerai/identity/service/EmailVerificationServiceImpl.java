package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.identity.dto.MessageResponse;
import com.manhnv.capstonebakerai.identity.entity.VerificationCode;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.identity.enums.VerificationPurpose;
import com.manhnv.capstonebakerai.identity.exception.InvalidVerificationCodeException;
import com.manhnv.capstonebakerai.identity.exception.VerificationEmailDeliveryException;
import com.manhnv.capstonebakerai.identity.repository.VerificationCodeRepository;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
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
    private final CustomerRepository customerRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String sender;
    private final Duration codeExpiration;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String sender,
            @Value("${app.verification-code-expiration}") Duration codeExpiration
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.sender = sender;
        this.codeExpiration = codeExpiration;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendRegistrationCode(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        customerRepository.findByUserId(userId)
                .filter(customer -> !customer.isConfirmed())
                .ifPresent(customer -> sendVerificationCode(user));
    }

    private void sendVerificationCode(User user) {
        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        VerificationCode verification = verificationCodeRepository
                .findByUserUserIdAndPurpose(user.getUserId(), VerificationPurpose.EMAIL_VERIFICATION)
                .orElseGet(VerificationCode::new);
        verification.setUser(user);
        verification.setPurpose(VerificationPurpose.EMAIL_VERIFICATION);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setCreatedAt(now);
        verification.setExpiresAt(now.plus(codeExpiration));
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

        Customer customer = customerRepository.findByUserId(user.getUserId())
                .orElseThrow(InvalidVerificationCodeException::new);

        if (customer.isConfirmed()) {
            return new MessageResponse("Email is already verified");
        }

        VerificationCode verification = verificationCodeRepository
                .findByUserUserIdAndPurpose(user.getUserId(), VerificationPurpose.EMAIL_VERIFICATION)
                .orElseThrow(InvalidVerificationCodeException::new);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!verification.getExpiresAt().isAfter(now)
                || !passwordEncoder.matches(code, verification.getCodeHash())) {
            throw new InvalidVerificationCodeException();
        }

        customer.setConfirmed(true);
        user.setUpdatedAt(now);
        customerRepository.save(customer);
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
            customerRepository.findByUserId(user.getUserId())
                    .filter(customer -> !customer.isConfirmed())
                    .ifPresent(customer -> sendVerificationCode(user));
        }

        return new MessageResponse(
                "If the account exists and is not verified, a new code has been sent"
        );
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
