package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.*;
import com.manhnv.capstonebakerai.product.entities.*;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.exception.ProductErrorCode;
import com.manhnv.capstonebakerai.product.repository.*;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository products;
    private final ProductUnitRepository units;
    private final ChildProductRepository children;
    private final UnitPriceHistoryRepository priceHistory;
    private final ProductCategoryRepository categories;

    public ProductServiceImpl(ProductRepository products, ProductUnitRepository units,
            ChildProductRepository children, UnitPriceHistoryRepository priceHistory,
            ProductCategoryRepository categories) {
        this.products = products;
        this.units = units;
        this.children = children;
        this.priceHistory = priceHistory;
        this.categories = categories;
    }

    @Override
    public ProductResponse create(ProductUpsertRequest request) {
        validateProductRequest(request);
        if (request.units() == null || request.units().isEmpty())
            throw invalid("At least one product unit is required when creating a product");
        if (request.units().stream().anyMatch(unit -> unit != null && unit.unitId() != null))
            throw invalid("Unit IDs must not be provided when creating a product");
        if (products.existsBySkuIgnoreCase(request.sku().trim()))
            throw new AppException(ProductErrorCode.DUPLICATE_SKU);
        validateCategory(request.categoryId());
        Product product = new Product();
        apply(product, request, true);
        Product savedProduct = products.save(product);
        synchronizeUnits(savedProduct.getProductId(), request.units());
        return toResponse(savedProduct);
    }

    @Override
    public ProductResponse update(UUID productId, ProductUpsertRequest request) {
        validateProductRequest(request);
        Product product = getEntity(productId);
        if (!product.getSku().equalsIgnoreCase(request.sku().trim())
                && products.existsBySkuIgnoreCase(request.sku().trim()))
            throw new AppException(ProductErrorCode.DUPLICATE_SKU);
        validateCategory(request.categoryId());
        apply(product, request, false);
        Product updatedProduct = products.save(product);
        if (request.units() != null) {
            if (request.units().isEmpty()) throw invalid("The unit list cannot be empty");
            synchronizeUnits(productId, request.units());
        }
        return toResponse(updatedProduct);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse get(UUID productId) { return toResponse(getEntity(productId)); }

    @Override @Transactional(readOnly = true)
    public Page<ProductResponse> list(UUID categoryId, String brand, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged() || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw invalid("Page must be non-negative and page size cannot exceed 100");
        if (brand != null && brand.length() > 255) throw invalid("Brand filter cannot exceed 255 characters");
        Set<String> sortableFields = Set.of("productId", "sku", "name", "brand", "status", "createdAt", "updatedAt", "stockQuantity");
        for (Sort.Order order : pageable.getSort()) {
            if (!sortableFields.contains(order.getProperty())) throw invalid("Unsupported product sort field");
        }
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (categoryId != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoryId"), categoryId));
        if (brand != null && !brand.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
        return products.findAll(spec, pageable).map(this::toResponse);
    }

    /** Soft delete: the schema has status values but no deleted_at column. */
    @Override
    public void deactivate(UUID productId) {
        Product product = getEntity(productId);
        product.setStatus(ProductStatus.INACTIVE);
        products.save(product);
    }

    private void synchronizeUnits(UUID productId, List<ProductUnitRequest> requests) {
        getEntity(productId);
        if (requests == null || requests.isEmpty()) throw invalid("At least one product unit is required");
        List<ProductUnit> existingUnits = units.findByProductId(productId);
        Set<UUID> seen = new HashSet<>();
        Set<String> requestedNames = new HashSet<>();
        long activeBaseCount = 0;
        for (ProductUnitRequest request : requests) {
            validateUnit(request);
            if (request.unitId() != null) {
                if (!seen.add(request.unitId())) throw invalid("Duplicate unit id");
                if (units.findByUnitIdAndProductId(request.unitId(), productId).isEmpty())
                    throw new AppException(ProductErrorCode.NOT_FOUND, "Unit was not found for this product");
            }
            boolean active = request.status() == null || request.status() == UnitStatus.ACTIVE;
            if (!requestedNames.add(request.unitName().trim().toLowerCase(Locale.ROOT)))
                throw invalid("Unit names must be unique within a product");
            if (active) {
                if (Boolean.TRUE.equals(request.base())) activeBaseCount++;
            }
            if (active && Boolean.TRUE.equals(request.base())
                    && request.conversionFactor().compareTo(BigDecimal.ONE) != 0)
                throw invalid("Base unit conversion_factor must be 1");
        }
        if (activeBaseCount != 1) throw invalid("Exactly one active unit must be the base unit");

        Map<UUID, ProductUnit> existingById = new HashMap<>();
        existingUnits.forEach(unit -> existingById.put(unit.getUnitId(), unit));
        UUID updatedBy = null;
        for (ProductUnitRequest request : requests) {
            ProductUnit unit = request.unitId() == null ? new ProductUnit() : existingById.get(request.unitId());
            boolean priceChanged = unit.getUnitId() == null || unit.getSellingPrice() == null
                    || unit.getSellingPrice().compareTo(request.sellingPrice()) != 0;
            unit.setProductId(productId);
            unit.setUnitName(request.unitName().trim());
            unit.setConversionFactor(request.conversionFactor());
            unit.setBase(Boolean.TRUE.equals(request.base()));
            unit.setSale(request.sale() == null || request.sale());
            unit.setSellingPrice(request.sellingPrice());
            unit.setStatus(request.status() == null ? UnitStatus.ACTIVE : request.status());
            unit.setWeightGram(request.weightGram());
            ProductUnit saved = units.save(unit);
            if (priceChanged) {
                if (updatedBy == null) updatedBy = SecurityUtils.getCurrentUserId();
                UnitPriceHistory history = new UnitPriceHistory();
                history.setUnitId(saved.getUnitId());
                history.setUpdatedBy(updatedBy);
                history.setSellingPrice(saved.getSellingPrice());
                priceHistory.save(history);
            }
        }
        for (ProductUnit existing : existingUnits) {
            if (!seen.contains(existing.getUnitId())) {
                existing.setStatus(UnitStatus.INACTIVE);
                existing.setBase(false);
                units.save(existing);
            }
        }
    }

    @Override @Transactional(readOnly = true)
    public List<UnitPriceHistoryResponse> getUnitPriceHistory(UUID productId, UUID unitId) {
        if (units.findByUnitIdAndProductId(unitId, productId).isEmpty())
            throw new AppException(ProductErrorCode.NOT_FOUND, "Unit was not found for this product");
        return priceHistory.findByUnitIdOrderByCreatedAtDesc(unitId).stream()
                .map(priceHistoryEntry -> new UnitPriceHistoryResponse(priceHistoryEntry.getUnitHistoryId(), priceHistoryEntry.getUnitId(), priceHistoryEntry.getUpdatedBy(),
                        priceHistoryEntry.getSellingPrice(), priceHistoryEntry.getCreatedAt())).toList();
    }

    @Override
    public List<ComboItemRequest> setComboItems(UUID productId, List<ComboItemRequest> items) {
        Product parent = getEntity(productId);
        if (!parent.isCombo()) throw invalid("Product must have is_combo=true before adding combo items");
        if (items == null || items.isEmpty()) throw invalid("A combo must contain at least one product");
        Set<UUID> unique = new HashSet<>();
        for (ComboItemRequest item : items) {
            if (item == null || item.productId() == null || item.quantity() == null || item.quantity().signum() <= 0
                    || !fits(item.quantity(), 10, 2))
                throw invalid("Each combo item needs a product and a positive quantity");
            if (item.productId().equals(productId)) throw invalid("A combo cannot contain itself");
            if (!unique.add(item.productId())) throw invalid("A product can only appear once in a combo");
            getEntity(item.productId());
            if (canReach(item.productId(), productId, new HashSet<>()))
                throw invalid("Combo composition cannot contain a cycle");
        }
        children.deleteByParentProductId(productId);
        for (ComboItemRequest item : items) {
            ChildProduct child = new ChildProduct();
            child.setParentProductId(productId);
            child.setChildrenProductId(item.productId());
            child.setQuantity(item.quantity());
            children.save(child);
        }
        return List.copyOf(items);
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal calculateComboStock(UUID productId) {
        Product product = getEntity(productId);
        if (!product.isCombo()) throw invalid("Product is not a combo");
        return calculateStock(productId, new HashSet<>());
    }

    @Override
    public List<InventoryProduct> lockInventoryProducts(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) throw invalid("At least one product is required");
        List<UUID> sorted = productIds.stream().distinct().sorted().toList();
        List<Product> locked = products.lockProducts(sorted);
        if (locked.size() != sorted.size()) throw new AppException(ProductErrorCode.NOT_FOUND, "One or more products were not found");
        return locked.stream().map(product -> new InventoryProduct(product.getProductId(), nz(product.getStockQuantity()), product.isCombo())).toList();
    }

    @Override @Transactional(readOnly = true)
    public InventoryProduct getInventoryProduct(UUID productId) {
        Product product = getEntity(productId);
        return new InventoryProduct(product.getProductId(), nz(product.getStockQuantity()), product.isCombo());
    }

    @Override @Transactional(readOnly = true)
    public InventoryUnit getActiveInventoryUnit(UUID productId, UUID unitId) {
        ProductUnit unit=units.findByUnitIdAndProductId(unitId,productId)
                .filter(productUnit -> productUnit.getStatus() == UnitStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND,"Active unit was not found for this product"));
        return new InventoryUnit(unit.getUnitId(),unit.getProductId(),unit.getConversionFactor(),unit.isBase());
    }

    @Override @Transactional(readOnly = true)
    public InventoryUnit getActiveBaseInventoryUnit(UUID productId) {
        ProductUnit unit=units.findByProductIdAndBaseTrueAndStatus(productId,UnitStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND,"Product has no active base unit"));
        return new InventoryUnit(unit.getUnitId(),unit.getProductId(),unit.getConversionFactor(),unit.isBase());
    }

    @Override
    public void adjustInventoryStock(Map<UUID, BigDecimal> stockDeltas) {
        if(stockDeltas==null||stockDeltas.isEmpty())return;
        List<UUID> productIds = stockDeltas.keySet().stream().sorted().toList();
        List<Product> lockedProducts = products.lockProducts(productIds);
        if (lockedProducts.size() != productIds.size()) throw new AppException(ProductErrorCode.NOT_FOUND, "One or more products were not found");
        Map<UUID, Product> productsById = new HashMap<>();
        lockedProducts.forEach(product -> productsById.put(product.getProductId(), product));
        for (UUID productId : productIds) {
            Product product = productsById.get(productId);
            BigDecimal delta = stockDeltas.get(productId);
            if(product.isCombo())throw invalid("Inventory cannot be adjusted directly for combo products");
            if(delta==null)continue;
            BigDecimal updated=nz(product.getStockQuantity()).add(delta);
            if(updated.signum()<0)throw invalid("Inventory adjustment would make stock negative for product: "+productId);
            if(!fits(updated,10,2))throw invalid("Inventory quantity exceeds the database precision limit");
            product.setStockQuantity(updated);
        }
    }

    @Override @Transactional(readOnly = true)
    public List<ProductResponse> listLowStockProducts() {
        return products.findLowStockProducts(ProductStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    private BigDecimal calculateStock(UUID productId, Set<UUID> visiting) {
        Product product = getEntity(productId);
        if (!product.isCombo()) return nz(product.getStockQuantity());
        if (!visiting.add(productId)) throw invalid("Combo composition contains a cycle");
        List<ChildProduct> parts = children.findByParentProductId(productId);
        if (parts.isEmpty()) { visiting.remove(productId); return BigDecimal.ZERO; }
        BigDecimal available = null;
        for (ChildProduct part : parts) {
            BigDecimal componentStock = calculateStock(part.getChildrenProductId(), visiting);
            BigDecimal possible = componentStock.divide(part.getQuantity(), 6, RoundingMode.DOWN);
            if (available == null || possible.compareTo(available) < 0) available = possible;
        }
        visiting.remove(productId);
        return available == null ? BigDecimal.ZERO : available;
    }

    private boolean canReach(UUID from, UUID target, Set<UUID> visited) {
        if (from.equals(target)) return true;
        if (!visited.add(from)) return false;
        for (ChildProduct child : children.findByParentProductId(from))
            if (canReach(child.getChildrenProductId(), target, visited)) return true;
        return false;
    }

    private ProductResponse toResponse(Product product) {
        BigDecimal stock = product.isCombo() ? calculateStock(product.getProductId(), new HashSet<>())
                : nz(product.getStockQuantity());
        List<ProductUnitResponse> productUnits = units.findByProductId(product.getProductId()).stream()
                .map(this::toUnitResponse).toList();
        return new ProductResponse(product.getProductId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategoryId(), product.getImgUrl(), product.getDiscountPercent(), product.getStorageGuide(),
                product.getMinStock(), product.getMaxStock(), stock, product.getStatus(), product.getBrand(),
                product.isCombo(), product.getCreatedAt(), product.getUpdatedAt(), productUnits);
    }

    private ProductUnitResponse toUnitResponse(ProductUnit unit) {
        return new ProductUnitResponse(unit.getUnitId(), unit.getUnitName(), unit.getConversionFactor(),
                unit.isBase(), unit.isSale(), unit.getSellingPrice(), unit.getStatus(), unit.getWeightGram());
    }

    private Product getEntity(UUID id) {
        return products.findById(id).orElseThrow(() -> new AppException(ProductErrorCode.NOT_FOUND));
    }
    private void validateCategory(UUID id) {
        if (id != null && !categories.existsById(id)) throw new AppException(ProductErrorCode.NOT_FOUND, "Category was not found");
    }
    private void apply(Product product, ProductUpsertRequest request, boolean creating) {
        boolean clearingCombo = product.isCombo() && Boolean.FALSE.equals(request.combo());
        product.setSku(request.sku().trim()); product.setName(request.name().trim()); product.setDescription(request.description());
        product.setCategoryId(request.categoryId()); product.setImgUrl(request.imgUrl());
        product.setDiscountPercent(request.discountPercent() == null ? BigDecimal.ZERO : request.discountPercent());
        product.setStorageGuide(request.storageGuide()); product.setMinStock(request.minStock() == null ? BigDecimal.ZERO : request.minStock());
        product.setMaxStock(request.maxStock()); product.setBrand(request.brand());
        if (request.status() != null) product.setStatus(request.status());
        if (request.combo() != null) product.setCombo(request.combo()); else if (creating) product.setCombo(false);
        if (clearingCombo) children.deleteByParentProductId(product.getProductId());
    }
    private void validateProductRequest(ProductUpsertRequest request) {
        if (request == null || request.sku() == null || request.sku().isBlank() || request.name() == null || request.name().isBlank())
            throw invalid("SKU and name are required");
        if (request.discountPercent() != null && (request.discountPercent().signum() < 0 || request.discountPercent().compareTo(new BigDecimal("100")) > 0))
            throw invalid("discountPercent must be between 0 and 100");
        if ((request.discountPercent() != null && !fits(request.discountPercent(), 5, 2))
                || (request.minStock() != null && !fits(request.minStock(), 10, 2))
                || (request.maxStock() != null && !fits(request.maxStock(), 10, 2)))
            throw invalid("A numeric product field exceeds its database precision");
        if (request.minStock() != null && request.minStock().signum() < 0) throw invalid("minStock cannot be negative");
        if (request.maxStock() != null && request.maxStock().signum() < 0) throw invalid("maxStock cannot be negative");
        if (request.minStock() != null && request.maxStock() != null && request.maxStock().compareTo(request.minStock()) < 0)
            throw invalid("maxStock cannot be lower than minStock");
    }
    private void validateUnit(ProductUnitRequest unitRequest) {
        if (unitRequest == null || unitRequest.unitName() == null || unitRequest.unitName().isBlank() || unitRequest.conversionFactor() == null
                || unitRequest.conversionFactor().signum() <= 0 || !fits(unitRequest.conversionFactor(), 10, 2)
                || unitRequest.base() == null || unitRequest.sellingPrice() == null || unitRequest.sellingPrice().signum() < 0
                || !fits(unitRequest.sellingPrice(), 15, 2) || unitRequest.unitName().trim().length() > 100)
            throw invalid("Unit name, positive conversion factor, and non-negative selling price are required");
        boolean sale = r.sale() == null || r.sale();
        if (sale && (r.weightGram() == null || r.weightGram() <= 0))
            throw invalid("weightGram is required and must be positive when the unit is for sale");
    }
    private AppException invalid(String message) { return new AppException(ProductErrorCode.INVALID_REQUEST, message); }
    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean fits(BigDecimal value, int precision, int scale) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() <= scale && normalized.precision() - normalized.scale() <= precision - scale;
    }
}
