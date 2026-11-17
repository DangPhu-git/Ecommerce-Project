package com.ecommerce.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * User Entity mapped to 'users' table.
 * Implements Spring Security UserDetails for authentication and authorization.
 * Rejects raw/plaintext password hashes via domain enforcement.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotNull
    @Column(name = "role", nullable = false, length = 20)
    @Convert(converter = RoleConverter.class)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Enforces that raw passwords cannot be stored directly.
     * Only valid BCrypt hash format ($2a$, $2b$, $2y$) is permitted.
     *
     * @param passwordHash BCrypt hashed password
     */
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or empty");
        }
        if (!BCRYPT_PATTERN.matcher(passwordHash).matches()) {
            throw new IllegalArgumentException("Raw passwords are not allowed. Passwords must be hashed using BCrypt.");
        }
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.isActive = false;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ==========================================
    // UserDetails Implementation
    // ==========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role.name() : Role.ROLE_USER.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isDeleted();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && !isDeleted();
    }
}
