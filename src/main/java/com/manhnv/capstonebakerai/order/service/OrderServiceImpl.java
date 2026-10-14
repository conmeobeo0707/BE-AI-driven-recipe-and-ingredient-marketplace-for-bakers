package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.order.dto.request.*;
import com.manhnv.capstonebakerai.order.dto.response.*;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.entity.OrderItem;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.enums.RevenuePeriodType;
import com.manhnv.capstonebakerai.order.exception.OrderErrorCode;
import com.manhnv.capstonebakerai.order.mapper.OrderMapper;
import com.manhnv.capstonebakerai.order.repository.OrderRepository;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.service.ShipmentService;
import com.manhnv.capstonebakerai.shipment.service.ShippingRateService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    CustomerRepository customerRepository;
    ProductRepository productRepository;
    ProductUnitRepository productUnitRepository;
    OrderMapper mapper;
    ShipmentService shipmentService;
    ShippingRateService shippingRateService;

    @Override
    @Transactional
    public OrderResponse createForCustomer(UUID userId, CreateOrderRequest request) {
        Customer customer = findCustomerByUserId(userId);
        Order order = buildOrder(customer, userId, request, BigDecimal.ZERO);
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Customer customer = findCustomerByUserId(userId);
        return orderRepository.findAll(spec(customer.getId(), status, null), pageable)
                .map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID userId, UUID orderId) {
        return mapper.toResponse(findOwnedOrder(userId, orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse trackMyOrder(UUID userId, UUID orderId) {
        return mapper.toTracking(findOwnedOrder(userId, orderId));
    }

    @Override
    @Transactional
    public OrderResponse cancelByCustomer(UUID userId, UUID orderId, CancelOrderRequest request) {
        Order order = findOwnedOrder(userId, orderId);
        if (!order.getStatus().isCancellableByCustomer()) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_CANCEL);
        }
        applyCancel(order, request);
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse confirmReceived(UUID userId, UUID orderId) {
        Order order = findOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERED);
        }
        if (order.getReceivedConfirmedAt() == null) {
            order.setReceivedConfirmedAt(LocalDateTime.now());
        }
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void syncStatusFromShipment(UUID orderId, OrderStatus next) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() == next) return;
        applyTransition(order, next);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingRateResponse> previewShippingRates(ShippingRatesRequest request) {
        List<ResolvedLine> lines = resolveLines(request.items());

        int totalWeightGram = lines.stream()
                .mapToInt(l -> l.weightGram() * l.quantity())
                .sum();

        BigDecimal productsPrice = lines.stream()
                .map(ResolvedLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cod = request.paymentMethod() == PaymentMethod.COD
                ? productsPrice : BigDecimal.ZERO;

        return shippingRateService.getRates(
                request.provinceCode(),
                request.districtCode(),
                request.wardCode(),
                totalWeightGram,
                cod,
                productsPrice
        );
    }

    @Override
    @Transactional
    public OrderResponse createByStaff(UUID staffUserId, StaffCreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new AppException(OrderErrorCode.CUSTOMER_NOT_FOUND));
        BigDecimal discount = request.discountPercent() != null
                ? request.discountPercent()
                : BigDecimal.ZERO;
        Order order = buildOrder(customer, staffUserId, request.order(), discount);
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllForStaff(OrderStatus status, PaymentMethod paymentMethod, Pageable pageable) {
        return orderRepository.findAll(spec(null, status, paymentMethod), pageable)
                .map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getForStaff(UUID orderId) {
        return mapper.toResponse(findOrThrow(orderId));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus next) {
        if (next == OrderStatus.CANCELLED) {
            throw new AppException(OrderErrorCode.ORDER_USE_CANCEL_ENDPOINT);
        }
        Order order = findOrThrow(orderId);
        applyTransition(order, next);
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelByStaff(UUID orderId, CancelOrderRequest request) {
        Order order = findOrThrow(orderId);
        if (!order.getStatus().isCancellableByStaff()) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_CANCEL);
        }
        applyCancel(order, request);
        return mapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(RevenuePeriodType type, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new AppException(OrderErrorCode.INVALID_DATE_RANGE);
        }
        List<Order> orders = orderRepository
                .findByStatusAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(OrderStatus.DELIVERED, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        Function<LocalDate, String> keyOf = switch (type) {
            case DAY -> LocalDate::toString;
            case WEEK -> d -> d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTH -> d -> YearMonth.from(d).toString();
        };

        Map<String, List<Order>> grouped = orders.stream().collect(
                Collectors.groupingBy(
                        o -> keyOf.apply(o.getDeliveredAt().toLocalDate()),
                                TreeMap::new,
                                Collectors.toList()));

        List<RevenueReportResponse.RevenuePoint> points = grouped.entrySet().stream()
                .map(e -> new RevenueReportResponse.RevenuePoint(e.getKey(),sumRevenue(e.getValue()), e.getValue().size())).toList();

                return new RevenueReportResponse(type, from, to, sumRevenue(orders), orders.size(), points);
    }

    private Order buildOrder(Customer customer, UUID createdBy, CreateOrderRequest request, BigDecimal discountPercent) {
        Order order = Order.builder()
                .code(generateCode())
                .customer(customer)
                .createdBy(createdBy)
                .paymentMethod(request.paymentMethod())
                .note(request.note())
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .provinceCode(request.provinceCode())
                .districtCode(request.districtCode())
                .wardCode(request.wardCode())
                .fullAddress(request.fullAddress())
                .addressDetail(request.addressDetail())
                .discountPercent(discountPercent)
                .build();

        addItems(order, request.items());
        applyShipping(order, request.shippingRateId());
        return order;
    }

    private record ResolvedLine(Product product, ProductUnit unit, int quantity,
                                int weightGram, BigDecimal unitPrice, BigDecimal lineTotal) {}

    private List<ResolvedLine> resolveLines(List<OrderItemRequest> lines) {
        List<UUID> unitIds = lines.stream().map(OrderItemRequest::productUnitId).distinct().toList();
        Map<UUID, ProductUnit> units = productUnitRepository.findAllById(unitIds).stream()
                .collect(Collectors.toMap(ProductUnit::getUnitId, Function.identity()));

        List<UUID> productIds = units.values().stream().map(ProductUnit::getProductId).distinct().toList();
        Map<UUID, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        List<ResolvedLine> result = new ArrayList<>();
        for (OrderItemRequest line : lines) {
            ProductUnit unit = units.get(line.productUnitId());
            if (unit == null) throw new AppException(OrderErrorCode.PRODUCT_UNIT_NOT_FOUND);

            Product product = products.get(unit.getProductId());
            if (product == null || product.getStatus() != ProductStatus.ACTIVE
                    || unit.getStatus() != UnitStatus.ACTIVE || !unit.isSale()) {
                throw new AppException(OrderErrorCode.PRODUCT_NOT_AVAILABLE);
            }

            Integer weight = unit.getWeightGram();
            if (weight == null || weight <= 0) {
                throw new AppException(OrderErrorCode.PRODUCT_WEIGHT_MISSING);
            }

            BigDecimal unitPrice = resolveUnitPrice(product, unit);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));
            result.add(new ResolvedLine(product, unit, line.quantity(), weight, unitPrice, lineTotal));
        }
        return result;
    }

    private BigDecimal resolveUnitPrice(Product product, ProductUnit unit) {
        BigDecimal discount = product.getDiscountPercent() != null
                ? product.getDiscountPercent()
                : BigDecimal.ZERO;
        return unit.getSellingPrice()
                .multiply(BigDecimal.valueOf(100).subtract(discount))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    private void addItems(Order order, List<OrderItemRequest> lines) {
        BigDecimal productsPrice = BigDecimal.ZERO;
        for (ResolvedLine l : resolveLines(lines)) {
            order.addItem(OrderItem.builder()
                    .productId(l.product().getProductId())
                    .productUnitId(l.unit().getUnitId())
                    .productName(l.product().getName())
                    .sku(l.product().getSku())
                    .unitName(l.unit().getUnitName())
                    .quantity(l.quantity())
                    .weightGram(l.weightGram())
                    .unitPrice(l.unitPrice())
                    .lineTotal(l.lineTotal())
                    .build());
            productsPrice = productsPrice.add(l.lineTotal());
        }
        order.setProductsPrice(productsPrice);
    }

    private void applyShipping(Order order, String rateId) {
        ShippingRateResponse rate = shippingRateService.requireRate(
                order.getProvinceCode(), order.getDistrictCode(), order.getWardCode(),
                order.totalWeightGram(), estimateCod(order), order.getProductsPrice(), rateId);
        order.setShippingRateId(rate.id());
        order.setCarrierName(rate.carrierName());
        order.setShippingService(rate.service());
        order.setShippingFee(rate.totalFee().add(rate.codFee() != null ? rate.codFee() : BigDecimal.ZERO));
        order.recalculateTotals();
    }

    private BigDecimal estimateCod(Order order) {
        return order.getPaymentMethod() == PaymentMethod.COD ? order.getProductsPrice() : BigDecimal.ZERO;
    }

    private void applyTransition(Order order, OrderStatus next) {
        if (!order.getStatus().canTransitionTo(next)) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION);
        }
        switch (next) {
            case CONFIRMED -> {
                // TODO Phase 4: nếu paymentMethod = PAYOS thì chỉ cho CONFIRMED khi payment đã PAID
                // TODO: gọi InventoryService.deduct(...) trừ kho (quantity x conversionFactor)
                shipmentService.createForOrder(order);
            }
            case DELIVERED -> {
                order.setDeliveredAt(LocalDateTime.now());
                order.setRevenue(order.getProductsPrice().subtract(order.getDiscountAmount()));
                // TODO Phase 4: nếu COD thì payment PAID, ghi payment_transaction, tạo phiếu thu
            }
            default -> { }
        }
        order.setStatus(next);
    }

    private void applyCancel(Order order, CancelOrderRequest request) {
        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request != null ? request.reason() : null);
        if (previous != OrderStatus.PENDING) {
            // TODO: hoàn kho (InventoryService.restock)
            // TODO Phase 3: hủy vận đơn trên Goship
            shipmentService.cancelForOrder(order.getId());
            // TODO Phase 4: hoàn tiền nếu đã thanh toán PayOS
        }
    }

    private Specification<Order> spec(UUID customerId, OrderStatus status, PaymentMethod paymentMethod) {
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> ps = new ArrayList<>();
            if (customerId != null) ps.add((Predicate) criteriaBuilder.equal(root.get("customer").get("id"), customerId));
            if (status != null) ps.add((Predicate) criteriaBuilder.equal(root.get("status"), status));
            if (paymentMethod != null) ps.add((Predicate) criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            return criteriaBuilder.and(ps.toArray(new Predicate[0]));
        });
    }

    private BigDecimal sumRevenue(List<Order> orders) {
        return orders.stream().map(o -> o.getRevenue() != null ? o.getRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Customer findCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
    }


    private Order findOwnedOrder(UUID userId, UUID orderId) {
        Customer customer = findCustomerByUserId(userId);
        Order order = findOrThrow(orderId);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String generateCode() {
        String code;
        do {
            code = "OD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (orderRepository.existsByCode(code));
        return code;
    }

}
