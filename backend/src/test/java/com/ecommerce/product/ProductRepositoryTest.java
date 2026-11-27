package com.ecommerce.product;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private Category electronicsCategory;

    @BeforeEach
    void setUp() {
        electronicsCategory = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic gadgets and devices")
                .sortOrder(1)
                .build();
        categoryRepository.save(electronicsCategory);
    }

    @Test
    @DisplayName("N+1 Prevention Test: Fetching 20 products with category and images MUST execute exactly 1 SQL query")
    void shouldFetch20ProductsWithCategoryAndImagesInSingleQuery() {
        // Seed exactly 20 products, each with a category and 2 product images
        List<Product> seededProducts = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Product product = Product.builder()
                    .name("Product " + i)
                    .slug("product-" + i)
                    .description("Description for product " + i)
                    .price(BigDecimal.valueOf(19.99 * i))
                    .category(electronicsCategory)
                    .inventoryCount(100)
                    .isActive(true)
                    .build();

            ProductImage img1 = ProductImage.builder()
                    .product(product)
                    .url("https://cdn.example.com/images/prod-" + i + "-1.jpg")
                    .altText("Front view")
                    .sortOrder(0)
                    .isPrimary(true)
                    .build();

            ProductImage img2 = ProductImage.builder()
                    .product(product)
                    .url("https://cdn.example.com/images/prod-" + i + "-2.jpg")
                    .altText("Side view")
                    .sortOrder(1)
                    .isPrimary(false)
                    .build();

            product.addImage(img1);
            product.addImage(img2);
            seededProducts.add(product);
        }
        productRepository.saveAll(seededProducts);

        // Flush and clear Hibernate 1st-level cache to simulate fresh database fetch
        entityManager.flush();
        entityManager.clear();

        // Enable and reset Hibernate statistics
        Session session = entityManager.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // EXECUTE QUERY USING @EntityGraph
        List<Product> products = productRepository.findAllWithCategoryAndImages();

        // Traverse all 20 products and access lazy relationships
        assertThat(products).hasSize(20);
        for (Product product : products) {
            // Verify Category is fully loaded without firing extra queries
            assertThat(product.getCategory()).isNotNull();
            assertThat(product.getCategory().getName()).isEqualTo("Electronics");

            // Verify Images are fully loaded without firing extra queries
            assertThat(product.getImages()).hasSize(2);
            assertThat(product.getImages().get(0).getUrl()).isNotBlank();
            assertThat(product.getImages().get(1).getUrl()).isNotBlank();
        }

        // VERIFY: Exactly 1 SQL statement was prepared and executed
        long prepareStatementCount = stats.getPrepareStatementCount();
        assertThat(prepareStatementCount)
                .as("Query count must be exactly 1, proving N+1 query problem is eliminated")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should find product with category and images by slug")
    void shouldFindProductWithCategoryAndImagesBySlug() {
        Product product = Product.builder()
                .name("Wireless Noise Cancelling Headphones")
                .slug("wireless-headphones")
                .description("Premium sound quality")
                .price(BigDecimal.valueOf(299.99))
                .category(electronicsCategory)
                .inventoryCount(50)
                .isActive(true)
                .build();

        ProductImage image = ProductImage.builder()
                .product(product)
                .url("https://cdn.example.com/headphones.jpg")
                .altText("Headphones")
                .isPrimary(true)
                .build();
        product.addImage(image);

        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findWithCategoryAndImagesBySlug("wireless-headphones").orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Wireless Noise Cancelling Headphones");
        assertThat(found.getCategory().getName()).isEqualTo("Electronics");
        assertThat(found.getImages()).hasSize(1);
    }
}
