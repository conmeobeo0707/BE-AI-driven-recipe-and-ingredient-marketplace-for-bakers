package com.manhnv.capstonebakerai.quote.mapper;

import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteItemResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteSummaryResponse;
import com.manhnv.capstonebakerai.quote.entity.RequestQuote;
import com.manhnv.capstonebakerai.quote.entity.RequestQuoteDetail;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestQuoteMapper {
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "details", target = "items")
    RequestQuoteResponse toResponse(RequestQuote quote, @Context QuoteLookup lookup);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "itemCount", expression = "java(quote.getDetails().size())")
    RequestQuoteSummaryResponse toSummary(RequestQuote quote);

    // MapStruct tự dùng method này để map List<RequestQuoteDetail> thành List<RequestQuoteItemResponse>
    default RequestQuoteItemResponse toItemResponse(RequestQuoteDetail detail, @Context QuoteLookup lookup) {
        ProductUnit unit = lookup.units().get(detail.getProductUnitId());
        Product product = unit != null ? lookup.products().get(unit.getProductId()) : null;
        return new RequestQuoteItemResponse(
                detail.getProductUnitId(),
                unit != null ? unit.getProductId() : null,
                product != null ? product.getName() : null,
                product != null ? product.getSku() : null,
                unit != null ? unit.getUnitName() : null,
                detail.getQuantity());
    }
}
