package com.ecommerce.product;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve hierarchical category tree")
    void shouldPersistAndRetrieveHierarchicalCategories() {
        Category parent = Category.builder()
                .name("Computers")
                .slug("computers")
                .sortOrder(1)
                .build();
        categoryRepository.save(parent);

        Category sub1 = Category.builder()
                .name("Laptops")
                .slug("laptops")
                .parent(parent)
                .sortOrder(1)
                .build();

        Category sub2 = Category.builder()
                .name("Desktops")
                .slug("desktops")
                .parent(parent)
                .sortOrder(2)
                .build();

        categoryRepository.save(sub1);
        categoryRepository.save(sub2);

        entityManager.flush();
        entityManager.clear();

        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderBySortOrderAsc();

        assertThat(rootCategories).hasSize(1);
        Category root = rootCategories.get(0);
        assertThat(root.getSlug()).isEqualTo("computers");
        assertThat(root.getSubcategories()).hasSize(2);
        assertThat(root.getSubcategories().get(0).getName()).isEqualTo("Laptops");
    }

    @Test
    @DisplayName("existsBySlug should return true for existing slug")
    void existsBySlug_ShouldDetectExistingSlug() {
        Category category = Category.builder()
                .name("Smartphones")
                .slug("smartphones")
                .build();
        categoryRepository.save(category);

        assertThat(categoryRepository.existsBySlug("smartphones")).isTrue();
        assertThat(categoryRepository.existsBySlug("non-existing-slug")).isFalse();
    }
}
