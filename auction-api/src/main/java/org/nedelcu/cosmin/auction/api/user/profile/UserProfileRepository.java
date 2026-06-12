package org.nedelcu.cosmin.auction.api.user.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    Optional<UserProfileEntity> findByUserId(Long userId);
}
