package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.CompanyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "role_tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 20)
    private CompanyType companyType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RoleTrack(Long id, String name, String description, CompanyType companyType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.companyType = companyType;
        this.createdAt = Instant.now();
    }
}
