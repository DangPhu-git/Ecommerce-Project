package com.ecommerce.auth;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        String encodedPassword = passwordEncoder.encode("SecurePassword123!");
        sampleUser = User.builder()
                .email("john.doe@example.com")
                .passwordHash(encodedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully persist User with encrypted password and retrieve by ID")
    void shouldPersistAndRetrieveUser() {
        User saved = userRepository.save(sampleUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(passwordEncoder.matches("SecurePassword123!", saved.getPasswordHash())).isTrue();

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should find user by email with case-insensitivity (UPPERCASE, lowercase, MixedCase)")
    void shouldFindUserByEmailCaseInsensitive() {
        userRepository.save(sampleUser);

        Optional<User> foundLower = userRepository.findByEmailIgnoreCase("john.doe@example.com");
        assertThat(foundLower).isPresent();

        Optional<User> foundUpper = userRepository.findByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM");
        assertThat(foundUpper).isPresent();
        assertThat(foundUpper.get().getId()).isEqualTo(sampleUser.getId());

        Optional<User> foundMixed = userRepository.findByEmailIgnoreCase("John.Doe@Example.Com");
        assertThat(foundMixed).isPresent();
        assertThat(foundMixed.get().getId()).isEqualTo(sampleUser.getId());

        assertThat(userRepository.existsByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should filter out soft-deleted users in active query")
    void shouldFilterSoftDeletedUser() {
        User saved = userRepository.save(sampleUser);
        saved.softDelete();
        userRepository.saveAndFlush(saved);

        // findByEmailIgnoreCase returns it
        assertThat(userRepository.findByEmailIgnoreCase("john.doe@example.com")).isPresent();

        // findActiveByEmailIgnoreCase excludes it
        assertThat(userRepository.findActiveByEmailIgnoreCase("john.doe@example.com")).isEmpty();
        assertThat(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("JOHN.DOE@EXAMPLE.COM")).isEmpty();
        assertThat(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("john.doe@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should reject duplicate email registration via unique constraint")
    void shouldRejectDuplicateEmail() {
        userRepository.saveAndFlush(sampleUser);

        User duplicate = User.builder()
                .email("john.doe@example.com")
                .passwordHash(passwordEncoder.encode("AnotherSecret456!"))
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
