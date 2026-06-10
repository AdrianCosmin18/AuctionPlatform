package org.nedelcu.cosmin.auction.worker.notification;

import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nedelcu.cosmin.auction.shared.notification.NotificationType;
import org.nedelcu.cosmin.auction.worker.user.UserEntity;
import org.nedelcu.cosmin.auction.worker.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final Set<NotificationType> EMAIL_ELIGIBLE_TYPES = EnumSet.of(
            NotificationType.AUCTION_WON,
            NotificationType.OUTBID,
            NotificationType.AUCTION_CLOSED,
            NotificationType.AUCTION_SUSPENDED
    );

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@archivebid.test}")
    private String fromAddress;

    @Transactional
    public void deliver(NotificationEntity notification) {
        notification.setEmailLastAttemptAt(java.time.OffsetDateTime.now());

        if (!mailEnabled || !EMAIL_ELIGIBLE_TYPES.contains(notification.getType())) {
            notification.setEmailDeliveryStatus(EmailDeliveryStatus.SKIPPED);
            notification.setEmailLastError(null);
            notificationRepository.save(notification);
            return;
        }

        UserEntity user = userRepository.findById(notification.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            notification.setEmailDeliveryStatus(EmailDeliveryStatus.FAILED);
            notification.setEmailLastError("User email not found.");
            notificationRepository.save(notification);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject(notification.getTitle());
            message.setText(buildMailBody(notification));
            mailSender.send(message);

            notification.setEmailDeliveryStatus(EmailDeliveryStatus.SENT);
            notification.setEmailSentAt(java.time.OffsetDateTime.now());
            notification.setEmailLastError(null);
            notificationRepository.save(notification);
        } catch (Exception exception) {
            log.error("Failed to send email notification id={} to userId={}", notification.getId(), notification.getUserId(), exception);
            notification.setEmailDeliveryStatus(EmailDeliveryStatus.FAILED);
            notification.setEmailLastError(exception.getMessage());
            notificationRepository.save(notification);
        }
    }

    private String buildMailBody(NotificationEntity notification) {
        String auctionLine = notification.getAuctionId() != null
                ? "Auction ID: #" + notification.getAuctionId() + System.lineSeparator() + System.lineSeparator()
                : "";

        return notification.getTitle() + System.lineSeparator()
                + System.lineSeparator()
                + auctionLine
                + notification.getMessage() + System.lineSeparator()
                + System.lineSeparator()
                + "ArchiveBid automated notification";
    }
}
