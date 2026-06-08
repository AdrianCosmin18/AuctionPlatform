package org.nedelcu.cosmin.auction.worker.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.shared.notification.NotificationType;
import org.nedelcu.cosmin.auction.worker.user.UserEntity;
import org.nedelcu.cosmin.auction.worker.user.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailNotificationService, "fromAddress", "no-reply@archivebid.test");
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deliverSendsEligibleNotificationEmail() {
        NotificationEntity notification = notification(10L, 2L, NotificationType.AUCTION_WON);
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail("winner@archivebid.test");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        emailNotificationService.deliver(notification);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        verify(notificationRepository).save(notification);
        assertEquals(EmailDeliveryStatus.SENT, notification.getEmailDeliveryStatus());
        assertNotNull(notification.getEmailSentAt());
        assertNotNull(notification.getEmailLastAttemptAt());
        assertEquals("winner@archivebid.test", mailCaptor.getValue().getTo()[0]);
        assertEquals("You won the auction", mailCaptor.getValue().getSubject());
    }

    @Test
    void deliverSkipsIneligibleNotificationTypes() {
        NotificationEntity notification = notification(11L, 3L, NotificationType.AUCTION_EXTENDED);

        emailNotificationService.deliver(notification);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertEquals(EmailDeliveryStatus.SKIPPED, notification.getEmailDeliveryStatus());
        assertNotNull(notification.getEmailLastAttemptAt());
    }

    @Test
    void deliverMarksNotificationFailedWhenUserEmailIsMissing() {
        NotificationEntity notification = notification(12L, 4L, NotificationType.OUTBID);
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        emailNotificationService.deliver(notification);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertEquals(EmailDeliveryStatus.FAILED, notification.getEmailDeliveryStatus());
        assertEquals("User email not found.", notification.getEmailLastError());
        assertNotNull(notification.getEmailLastAttemptAt());
    }

    private NotificationEntity notification(Long auctionId, Long userId, NotificationType type) {
        return NotificationEntity.builder()
                .id(100L)
                .auctionId(auctionId)
                .userId(userId)
                .type(type)
                .title(type == NotificationType.AUCTION_WON ? "You won the auction" : "Auction update")
                .message("Auction #" + auctionId + " notification body.")
                .read(false)
                .createdAt(OffsetDateTime.now())
                .readAt(null)
                .emailDeliveryStatus(EmailDeliveryStatus.PENDING)
                .emailSentAt(null)
                .emailLastAttemptAt(null)
                .emailLastError(null)
                .build();
    }
}
