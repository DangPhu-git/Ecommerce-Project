package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity.
 * Uses @EntityGraph to eagerly fetch Category and Images in a single query, preventing N+1 problems.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "images"})
    @Query("SELECT p FROM Product p WHERE p.slug = :slug AND p.isActive = true")
    Optional<Product> findWithCategoryAndImagesBySlug(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"category", "images"})
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true")
    Optional<Product> findWithCategoryAndImagesById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"category", "images"})
    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    List<Product> findAllWithCategoryAndImages();

    @EntityGraph(attributePaths = {"category", "images"})
    Page<Product> findByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "images"})
    Page<Product> findByCategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);
}
