package org.nedelcu.cosmin.auction.api.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("""
            update NotificationEntity n
            set n.read = true, n.readAt = CURRENT_TIMESTAMP
            where n.userId = :userId and n.read = false
            """)
    int markAllAsRead(@Param("userId") Long userId);
}
