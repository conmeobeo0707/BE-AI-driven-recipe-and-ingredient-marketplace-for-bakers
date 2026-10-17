package com.manhnv.capstonebakerai.shipment.provider.goship;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.provider.GoshipProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoshipClient {

    private final RestClient goshipRestClient;
    private final GoshipProperties props;

    public List<GoshipCity> cities() {
        return list("/cities", new ParameterizedTypeReference<GoshipListResponse<GoshipCity>>(){});
    }

    public List<GoshipDistrict> districts(String cityId) {
        return list("/cities/{id}/districts", new ParameterizedTypeReference<GoshipListResponse<GoshipDistrict>>() {}, cityId);
    }

    public List<GoshipWard> wards(String districtId) {
        return list("/districts/{id}/wards", new ParameterizedTypeReference<GoshipListResponse<GoshipWard>>() {}, districtId);
    }

    public List<GoshipRate> rates(String city, String district, String ward, int weightGram, BigDecimal cod, BigDecimal amount) {
        var from = props.pickup();
        Map<String, Object> body = Map.of("shipment", Map.of(
                "address_from", Map.of("city", from.city(), "district", from.district(), "ward", from.ward()),
                "address_to", Map.of("city", city, "district", district, "ward", ward),
                "parcel", parcel(weightGram, cod, amount, null)
        ));

        GoshipListResponse<GoshipRate> res = call(() -> goshipRestClient.post().uri("/rates")
                .body(body).retrieve()
                .body(new ParameterizedTypeReference<GoshipListResponse<GoshipRate>>() {}));
        return res == null || res.data() == null ? List.of() : res.data();
    }

    public GoshipCreateResponse createShipment(String rateId, String toName, String toPhone, String toStreet, String city, String district, String ward, int weightGram, BigDecimal cod, BigDecimal amount, String note) {
        var from = props.pickup();
        Map<String, Object> body = Map.of("shipment", Map.of(
                "rate", rateId,
                "payer", 1,
                "address_from", Map.of("name", from.name(), "phone", from.phone(), "street", from.street(), "city", from.city(), "district", from.district(), "ward", from.ward()),
                "address_to", Map.of("name", toName, "phone", toPhone, "street", toStreet, "city", city, "district", district, "ward", ward),
                "parcel", parcel(weightGram, cod, amount, note)
        ));

        GoshipCreateResponse res = call(() -> goshipRestClient.post().uri("/shipments")
                .body(body).retrieve()
                .body(GoshipCreateResponse.class));
        if (res == null || res.code() != 200 || res.trackingNumber() == null) {
            log.error("Goship create shipment failed: {}", res);
            throw new AppException(ShipmentErrorCode.GOSHIP_CREATE_FAILED);
        }
        return res;
    }

    private Map<String, Object> parcel(int weightGram, BigDecimal cod, BigDecimal amount, String note) {
        var size = props.parcel();
        Map<String, Object> m = new HashMap<>();
        m.put("cod", money(cod));
        m.put("amount", money(amount));
        m.put("amout", money(amount));
        m.put("weight", String.valueOf(weightGram));
        m.put("width", String.valueOf(size.width()));
        m.put("height", String.valueOf(size.height()));
        m.put("length", String.valueOf(size.length()));
        if (note != null && !note.isBlank()) m.put("metadata", note);
        return m;
    }

    private String money(BigDecimal v) {
        return v == null ? "0" : v.toBigInteger().toString();
    }

    private <T> List<T> list(String uri, ParameterizedTypeReference<GoshipListResponse<T>> type, Object... vars) {
        GoshipListResponse<T> res = call(() -> goshipRestClient.get().uri(uri, vars).retrieve().body(type));
        return res == null || res.data() == null ? List.of() : res.data();
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            log.error("Goship error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ShipmentErrorCode.GOSHIP_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Goship unreachable", e);
            throw new AppException(ShipmentErrorCode.GOSHIP_UNAVAILABLE);
        }
    }

}
