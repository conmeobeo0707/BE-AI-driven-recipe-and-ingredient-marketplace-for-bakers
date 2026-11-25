package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.provider.ShippingProvider;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipCreateResponse;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoshipShippingProviderTest {

    @Mock
    GoshipClient client;
    @InjectMocks
    GoshipShippingProvider provider;

    private static final String OLD_RATE = "OLD_RATE";
    private static final String NEW_RATE = "NEW_RATE";
    private static final String PROVINCE = "79";
    private static final String DISTRICT = "760";
    private static final String WARD = "26734";
    private static final String RECEIVER_NAME = "Nguyen Van A";
    private static final String RECEIVER_PHONE = "0901234567";
    private static final String ADDRESS_DETAIL = "12 Le Loi";
    private static final String NOTE = "Giao gio hanh chinh";
    private static final int WEIGHT = 1000;
    private static final BigDecimal COD = new BigDecimal("100000");
    private static final BigDecimal PRODUCTS_PRICE = new BigDecimal("120000");

    private Order order() {
        return Order.builder()
                .id(UUID.randomUUID())
                .shippingRateId(OLD_RATE)
                .carrierName("GHN")
                .shippingService("Standard")
                .provinceCode(PROVINCE)
                .districtCode(DISTRICT)
                .wardCode(WARD)
                .receiverName(RECEIVER_NAME)
                .receiverPhone(RECEIVER_PHONE)
                .addressDetail(ADDRESS_DETAIL)
                .note(NOTE)
                .productsPrice(PRODUCTS_PRICE)
                .build();
    }

    private GoshipRate rate(String id, String carrierName, String service) {
        return new GoshipRate(id, carrierName, null, service, null, null, null, null);
    }

    private GoshipCreateResponse created(String id, String tracking, String carrier) {
        return new GoshipCreateResponse(200, "success", id, tracking, "SHORT", carrier,
                "100000", "25000", "2026-11-25");
    }

    private void stubCreate(String rateId, GoshipCreateResponse res) {
        when(client.createShipment(rateId, RECEIVER_NAME, RECEIVER_PHONE, ADDRESS_DETAIL,
                PROVINCE, DISTRICT, WARD, WEIGHT, COD, PRODUCTS_PRICE, NOTE)).thenReturn(res);
    }

    private void stubCreateFails(String rateId, RuntimeException ex) {
        when(client.createShipment(rateId, RECEIVER_NAME, RECEIVER_PHONE, ADDRESS_DETAIL,
                PROVINCE, DISTRICT, WARD, WEIGHT, COD, PRODUCTS_PRICE, NOTE)).thenThrow(ex);
    }

    private void stubRates(List<GoshipRate> rates) {
        when(client.rates(PROVINCE, DISTRICT, WARD, WEIGHT, COD, PRODUCTS_PRICE)).thenReturn(rates);
    }

    private void verifyCreateCalled(String rateId, int times) {
        verify(client, times(times)).createShipment(rateId, RECEIVER_NAME, RECEIVER_PHONE, ADDRESS_DETAIL,
                PROVINCE, DISTRICT, WARD, WEIGHT, COD, PRODUCTS_PRICE, NOTE);
    }

    private void verifyRatesNeverCalled() {
        verify(client, never()).rates(any(), any(), any(), anyInt(), any(), any());
    }

    // Create shipment
    // UTCID01
    @Test
    void createShipment_firstAttemptSucceeds_shouldReturnResultWithoutRetry() {
        stubCreate(OLD_RATE, created("EXT-1", "TRK-1", "GHN"));

        ShippingProvider.ShipmentCreateResult result = provider.createShipment(order(), COD, WEIGHT);

        assertThat(result.externalId()).isEqualTo("EXT-1");
        assertThat(result.trackingCode()).isEqualTo("TRK-1");
        assertThat(result.carrier()).isEqualTo("GHN");
        verifyCreateCalled(OLD_RATE, 1);
        verifyRatesNeverCalled();
    }

    // UTCID02
    @Test
    void createShipment_storedRateFails_shouldRetryWithFreshMatchingRate() {
        stubCreateFails(OLD_RATE, new AppException(ShipmentErrorCode.GOSHIP_CREATE_FAILED));
        stubRates(List.of(
                rate("X1", "GHTK", "Standard"),
                rate(NEW_RATE, "GHN", "Standard")));
        stubCreate(NEW_RATE, created("EXT-2", "TRK-2", "GHN"));

        ShippingProvider.ShipmentCreateResult result = provider.createShipment(order(), COD, WEIGHT);

        assertThat(result.externalId()).isEqualTo("EXT-2");
        assertThat(result.trackingCode()).isEqualTo("TRK-2");
        assertThat(result.carrier()).isEqualTo("GHN");
        verifyCreateCalled(OLD_RATE, 1);
        verifyCreateCalled(NEW_RATE, 1);
    }

    // UTCID03
    @Test
    void createShipment_storedRateFailsAndNoMatchingRate_shouldRethrowOriginal() {
        AppException original = new AppException(ShipmentErrorCode.GOSHIP_CREATE_FAILED);
        stubCreateFails(OLD_RATE, original);
        stubRates(List.of(
                rate("R1", "GHN", "Express"),      // đúng hãng, sai dịch vụ
                rate("R2", "GHTK", "Standard")));  // sai hãng, đúng dịch vụ

        assertThatThrownBy(() -> provider.createShipment(order(), COD, WEIGHT))
                .isSameAs(original);

        verifyCreateCalled(OLD_RATE, 1);
        verify(client, times(1)).createShipment(any(), any(), any(), any(), any(), any(), any(),
                anyInt(), any(), any(), any());
    }

    // UTCID04
    @Test
    void createShipment_storedRateFailsAndNoRates_shouldRethrowOriginal() {
        AppException original = new AppException(ShipmentErrorCode.GOSHIP_CREATE_FAILED);
        stubCreateFails(OLD_RATE, original);
        stubRates(List.of());

        assertThatThrownBy(() -> provider.createShipment(order(), COD, WEIGHT))
                .isSameAs(original);

        verifyCreateCalled(OLD_RATE, 1);
    }

    // UTCID05
    @Test
    void createShipment_retryAlsoFails_shouldThrowSecondErrorWithoutThirdAttempt() {
        stubCreateFails(OLD_RATE, new AppException(ShipmentErrorCode.GOSHIP_CREATE_FAILED));
        stubRates(List.of(rate(NEW_RATE, "GHN", "Standard")));
        stubCreateFails(NEW_RATE, new AppException(ShipmentErrorCode.GOSHIP_UNAVAILABLE));

        assertThatThrownBy(() -> provider.createShipment(order(), COD, WEIGHT))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.GOSHIP_UNAVAILABLE);

        verifyCreateCalled(OLD_RATE, 1);
        verifyCreateCalled(NEW_RATE, 1);
    }

    // UTCID06
    @Test
    void createShipment_nonAppException_shouldPropagateWithoutRetry() {
        stubCreateFails(OLD_RATE, new IllegalStateException("boom"));

        assertThatThrownBy(() -> provider.createShipment(order(), COD, WEIGHT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verifyRatesNeverCalled();
    }
}
