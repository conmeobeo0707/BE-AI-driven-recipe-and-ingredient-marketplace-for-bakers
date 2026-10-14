package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.dto.request.CustomerAddressRequest;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.entity.CustomerAddress;
import com.manhnv.capstonebakerai.customer.exception.CustomerAddressErrorCode;
import com.manhnv.capstonebakerai.customer.mapper.CustomerAddressMapper;
import com.manhnv.capstonebakerai.customer.repository.CustomerAddressRepository;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerAddressServiceImpl implements CustomerAddressService{

    static int MAX_ADDRESSES = 5;

    CustomerAddressRepository addressRepository;
    CustomerRepository customerRepository;
    CustomerAddressMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getMyAddresses(UUID userId) {
        Customer customer = findCustomerByUserId(userId);
        return list(customer.getId());
    }

    @Override
    @Transactional
    public CustomerAddressResponse create(UUID userId, CustomerAddressRequest request) {
        Customer customer = findCustomerByUserId(userId);

        long count = addressRepository.countByCustomerId(customer.getId());
        if (count >= MAX_ADDRESSES) {
            throw new AppException(CustomerAddressErrorCode.ADDRESS_LIMIT_REACHED);
        }

        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault) {
            unsetDefault(customer.getId());
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .provinceCode(request.provinceCode())
                .districtCode(request.districtCode())
                .wardCode(request.wardCode())
                .addressDetail(request.addressDetail().trim())
                .fullAddress(request.fullAddress().trim())
                .defaultAddress(makeDefault)
                .build();

        return mapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerAddressResponse update(UUID userId, UUID addressId, CustomerAddressRequest request) {
        Customer customer = findCustomerByUserId(userId);
        CustomerAddress address = findOwned(customer.getId(), addressId);

        address.setReceiverName(request.receiverName().trim());
        address.setReceiverPhone(request.receiverPhone().trim());
        address.setProvinceCode(request.provinceCode());
        address.setDistrictCode(request.districtCode());
        address.setWardCode(request.wardCode());
        address.setAddressDetail(request.addressDetail().trim());
        address.setFullAddress(request.fullAddress().trim());

        // Chỉ xử lý trường hợp bật mặc định. Muốn bỏ mặc định thì đặt địa chỉ khác làm mặc định.
        if (Boolean.TRUE.equals(request.defaultAddress()) && !address.isDefaultAddress()) {
            unsetDefault(customer.getId());
            address.setDefaultAddress(true);
        }

        return mapper.toResponse(addressRepository.save(address));

    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Customer customer = findCustomerByUserId(userId);
        CustomerAddress address = findOwned(customer.getId(), addressId);
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        addressRepository.flush();

        // Xóa địa chỉ mặc định thì đẩy địa chỉ mới nhất còn lại lên làm mặc định
        if (wasDefault) {
            addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customer.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    public CustomerAddressResponse setDefault(UUID userId, UUID addressId) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getByCustomerId(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(CustomerAddressErrorCode.CUSTOMER_NOT_FOUND);
        }
        return list(customerId);
    }

    private List<CustomerAddressResponse> list(UUID customerId) {
        return addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId).stream().map(mapper::toResponse).toList();
    }

    private void unsetDefault(UUID customerId) {
        addressRepository.findByCustomerId(customerId).stream()
                .filter(CustomerAddress::isDefaultAddress)
                .forEach(a -> a.setDefaultAddress(false));
    }

    private Customer findCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(CustomerAddressErrorCode.CUSTOMER_NOT_FOUND));
    }

    private CustomerAddress findOwned(UUID customerId, UUID addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new AppException(CustomerAddressErrorCode.ADDRESS_NOT_FOUND));
    }
}
