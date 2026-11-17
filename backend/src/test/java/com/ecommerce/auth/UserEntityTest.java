package com.ecommerce.auth;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.RoleConverter;
import com.ecommerce.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEntityTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final RoleConverter roleConverter = new RoleConverter();

    @Test
    @DisplayName("Should block setting raw plaintext password and throw IllegalArgumentException")
    void shouldBlockRawPassword() {
        User user = new User();

        assertThatThrownBy(() -> user.setPasswordHash("PlainPassword123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Raw passwords are not allowed");

        assertThatThrownBy(() -> user.setPasswordHash("123456"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> user.setPasswordHash(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> user.setPasswordHash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should accept valid BCrypt hashed password with strength 12")
    void shouldAcceptValidBcryptHash() {
        User user = new User();
        String rawPassword = "SuperSecretPassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        user.setPasswordHash(encoded);

        assertThat(user.getPasswordHash()).isEqualTo(encoded);
        assertThat(passwordEncoder.matches(rawPassword, user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("RoleConverter should accurately map between Spring Security roles and Database column values")
    void shouldConvertRoleCorrectly() {
        assertThat(roleConverter.convertToDatabaseColumn(Role.ROLE_USER)).isEqualTo("USER");
        assertThat(roleConverter.convertToDatabaseColumn(Role.ROLE_ADMIN)).isEqualTo("ADMIN");
        assertThat(roleConverter.convertToDatabaseColumn(Role.ROLE_SUPER_ADMIN)).isEqualTo("SUPER_ADMIN");

        assertThat(roleConverter.convertToEntityAttribute("USER")).isEqualTo(Role.ROLE_USER);
        assertThat(roleConverter.convertToEntityAttribute("ADMIN")).isEqualTo(Role.ROLE_ADMIN);
        assertThat(roleConverter.convertToEntityAttribute("SUPER_ADMIN")).isEqualTo(Role.ROLE_SUPER_ADMIN);
    }

    @Test
    @DisplayName("UserDetails methods should reflect User entity state and role authorities")
    void shouldReflectUserDetailsProperly() {
        String hash = passwordEncoder.encode("SecureP@ssw0rd");
        User user = User.builder()
                .email("admin@ecommerce.com")
                .passwordHash(hash)
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();

        assertThat(user.getUsername()).isEqualTo("admin@ecommerce.com");
        assertThat(user.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();

        // When soft deleted
        user.softDelete();
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountNonLocked()).isFalse();
    }
}
