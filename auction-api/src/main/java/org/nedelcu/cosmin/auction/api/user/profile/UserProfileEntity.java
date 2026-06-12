package org.nedelcu.cosmin.auction.api.user.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_profiles_seq_gen")
    @SequenceGenerator(name = "user_profiles_seq_gen", sequenceName = "user_profiles_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    private String country;

    private String city;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
