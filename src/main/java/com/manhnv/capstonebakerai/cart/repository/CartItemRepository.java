package com.manhnv.capstonebakerai.cart.repository;

import com.manhnv.capstonebakerai.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    @Query("""
        select i from CartItem i
          join fetch i.product
          join fetch i.uom
         where i.cart.id = :cartId
         order by i.addedAt desc
        """)
    List<CartItem> findAllByCartId(@Param("cartId") UUID cartId);

    @Query("""
        select i from CartItem i
         where i.cart.id = :cartId
           and i.product.productId = :productId
           and i.uom.unitId = :uomId
        """)
    Optional<CartItem> findExisting(@Param("cartId") UUID cartId,
                                    @Param("productId") UUID productId,
                                    @Param("uomId") UUID uomId);

    // đảm bảo item thuộc cart của đúng user (chống sửa/xóa item của người khác)
    Optional<CartItem> findByIdAndCartUserId(UUID id, UUID userId);
}
