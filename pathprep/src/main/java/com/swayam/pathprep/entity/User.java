package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a registered platform user (student or admin), holding profile details, institutional metadata, and credentials.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    @Column(name = "branch", length = 50)
    private String branch;

    @Column(name = "grad_year")
    private Integer gradYear;

    @Column(name = "target_track_id")
    private Long targetTrackId;

    @Column(name = "google_sub", length = 64, unique = true)
    private String googleSub;

    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User(Long id, String email, String name, UserRole role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
