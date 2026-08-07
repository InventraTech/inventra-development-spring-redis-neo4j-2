package com.inventra.api.core.domain.user;

import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.profile.Profile;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @Column(name = "id_user", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    @EqualsAndHashCode.Include
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @ToString.Exclude
    private String passwordHash;

    @Column(name = "role", length = 80)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kitchen")
    @ToString.Exclude
    private Kitchen kitchen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    @ToString.Exclude
    private Profile profile;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
