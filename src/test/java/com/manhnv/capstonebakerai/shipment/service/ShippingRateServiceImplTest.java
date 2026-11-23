package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingRateServiceImplTest {

    @Mock
    GoshipClient client;
    @InjectMocks
    ShippingRateServiceImpl service;

    private static final String PROVINCE = "79";
    private static final String DISTRICT = "760";
    private static final String WARD = "26734";
    private static final int WEIGHT = 1000;
    private static final BigDecimal COD = new BigDecimal("100000");
    private static final BigDecimal DECLARED = new BigDecimal("120000");

    private GoshipRate rateOf(String id, String carrierName, String service, String totalAmount) {
        GoshipRate r = mock(GoshipRate.class);
        when(r.id()).thenReturn(id);
        when(r.carrierName()).thenReturn(carrierName);
        when(r.service()).thenReturn(service);
        when(r.codFee()).thenReturn(new BigDecimal("5000"));
        when(r.totalFee()).thenReturn(new BigDecimal("25000"));
        when(r.totalAmount()).thenReturn(new BigDecimal(totalAmount));
        return r;
    }

    private void stubRates(List<GoshipRate> rates) {
        when(client.rates(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED)).thenReturn(rates);
    }

    // Get rates
    // UTCID01
    @Test
    void getRates_ratesAvailable_shouldMapAllRates() {
        stubRates(List.of(
                rateOf("R1", "GHN", "Standard", "30000"),
                rateOf("R2", "GHTK", "Express", "45000")));

        List<ShippingRateResponse> result = service.getRates(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("R1");
        assertThat(result.get(0).carrierName()).isEqualTo("GHN");
        assertThat(result.get(0).service()).isEqualTo("Standard");
        assertThat(result.get(0).codFee()).isEqualByComparingTo("5000");
        assertThat(result.get(0).totalFee()).isEqualByComparingTo("25000");
//        assertThat(result.get(0).totalAmount()).isEqualByComparingTo("30000");
        assertThat(result.get(1).id()).isEqualTo("R2");
//        assertThat(result.get(1).totalAmount()).isEqualByComparingTo("45000");
    }

    // UTCID02
    @Test
    void getRates_noRate_shouldReturnEmptyList() {
        stubRates(List.of());

        List<ShippingRateResponse> result = service.getRates(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED);

        assertThat(result).isEmpty();
    }

    // Require rate
    // UTCID01
    @Test
    void requireRate_rateExists_shouldReturnMatchingRate() {
        stubRates(List.of(
                rateOf("R1", "GHN", "Standard", "30000"),
                rateOf("R2", "GHTK", "Express", "45000")));

        ShippingRateResponse result = service.requireRate(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED, "R2");

        assertThat(result.id()).isEqualTo("R2");
        assertThat(result.carrierName()).isEqualTo("GHTK");
//        assertThat(result.totalAmount()).isEqualByComparingTo("45000");
    }

    // UTCID02
    @Test
    void requireRate_rateIdNotInList_shouldThrowRateNotFound() {
        stubRates(List.of(rateOf("R1", "GHN", "Standard", "30000")));

        assertThatThrownBy(() -> service.requireRate(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED, "R9"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.SHIPPING_RATE_NOT_FOUND);
    }

    // UTCID03
    @Test
    void requireRate_noRateReturned_shouldThrowRateNotFound() {
        stubRates(List.of());

        assertThatThrownBy(() -> service.requireRate(PROVINCE, DISTRICT, WARD, WEIGHT, COD, DECLARED, "R1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.SHIPPING_RATE_NOT_FOUND);
    }
}
