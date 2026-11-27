package com.ecommerce.product;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Product testProduct;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .name("Audio")
                .slug("audio-" + UUID.randomUUID())
                .build();
        categoryRepository.save(category);

        testProduct = Product.builder()
                .name("Bluetooth Speaker")
                .slug("bluetooth-speaker-" + UUID.randomUUID())
                .price(BigDecimal.valueOf(79.99))
                .category(category)
                .build();
        productRepository.save(testProduct);

        testUser1 = User.builder()
                .email("reviewer1-" + UUID.randomUUID() + "@example.com")
                .firstName("Alice")
                .lastName("Walker")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        testUser1.setPasswordHash("$2a$12$e8YkZ9N0O0b6Q5R8S7T6U4V3W2X1Y0Z9A8B7C6D5E4F3G2H1I0J1K");
        userRepository.save(testUser1);

        testUser2 = User.builder()
                .email("reviewer2-" + UUID.randomUUID() + "@example.com")
                .firstName("Bob")
                .lastName("Brown")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        testUser2.setPasswordHash("$2a$12$e8YkZ9N0O0b6Q5R8S7T6U4V3W2X1Y0Z9A8B7C6D5E4F3G2H1I0J1K");
        userRepository.save(testUser2);
    }

    @Test
    @DisplayName("Should calculate average rating and total count correctly")
    void shouldCalculateAverageRating() {
        Review review1 = Review.builder()
                .product(testProduct)
                .user(testUser1)
                .rating((short) 5)
                .title("Excellent speaker!")
                .body("Great sound quality and long battery life.")
                .isVerified(true)
                .build();

        Review review2 = Review.builder()
                .product(testProduct)
                .user(testUser2)
                .rating((short) 4)
                .title("Good value")
                .body("Satisfied with the purchase.")
                .isVerified(false)
                .build();

        reviewRepository.save(review1);
        reviewRepository.save(review2);

        entityManager.flush();
        entityManager.clear();

        Double avgRating = reviewRepository.getAverageRatingByProductId(testProduct.getId());
        long count = reviewRepository.countByProductId(testProduct.getId());

        assertThat(avgRating).isEqualTo(4.5);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should enforce unique constraint: one review per product per user")
    void shouldEnforceUniqueReviewPerProductAndUser() {
        Review review1 = Review.builder()
                .product(testProduct)
                .user(testUser1)
                .rating((short) 5)
                .build();
        reviewRepository.save(review1);
        entityManager.flush();

        Review duplicateReview = Review.builder()
                .product(testProduct)
                .user(testUser1)
                .rating((short) 3)
                .build();

        assertThatThrownBy(() -> {
            reviewRepository.save(duplicateReview);
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
    }
}
