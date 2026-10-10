package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.dto.CustomerProfileResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.customer.dto.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.UpdateCustomerProfileRequest;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.entity.CustomerHobby;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerHobbyRepository;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final CustomerHobbyRepository customerHobbyRepository;
    private final HobbyService hobbyService;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID userId) {
        Customer customer = findCustomer(userId);
        Set<UUID> hobbyIds = customerHobbyRepository.findAllByCustomerId(customer.getId())
                .stream()
                .map(CustomerHobby::getHobbyId)
                .collect(Collectors.toSet());
        List<HobbyResponse> hobbies = hobbyService.getByIds(hobbyIds);
        return toResponse(customer, hobbies);
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request) {
        Customer customer = findCustomer(userId);
        customer.setSkillLevel(request.skillLevel());
        return toResponse(customerRepository.save(customer), selectedHobbies(customer));
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateHobbies(UUID userId, UpdateCustomerHobbiesRequest request) {
        Customer customer = findCustomer(userId);
        Set<UUID> hobbyIds = request.hobbyIds();

        if (request.primaryHobbyId() != null && !hobbyIds.contains(request.primaryHobbyId())) {
            throw new AppException(CustomerErrorCode.INVALID_HOBBY_SELECTION);
        }

        List<HobbyResponse> hobbies = hobbyService.getActiveByIds(hobbyIds);
        if (hobbies.size() != hobbyIds.size()) {
            throw new AppException(CustomerErrorCode.INVALID_HOBBY_SELECTION);
        }

        customer.setHobbyId(request.primaryHobbyId());
        customerRepository.save(customer);

        customerHobbyRepository.deleteAllByCustomerId(customer.getId());
        List<CustomerHobby> customerHobbies = hobbyIds.stream()
                .map(hobbyId -> new CustomerHobby(customer.getId(), hobbyId))
                .toList();
        customerHobbyRepository.saveAll(customerHobbies);

        return toResponse(customer, hobbies);
    }

    private Customer findCustomer(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private List<HobbyResponse> selectedHobbies(Customer customer) {
        Set<UUID> hobbyIds = customerHobbyRepository.findAllByCustomerId(customer.getId())
                .stream()
                .map(CustomerHobby::getHobbyId)
                .collect(Collectors.toSet());
        return hobbyService.getByIds(hobbyIds);
    }

    private CustomerProfileResponse toResponse(Customer customer, List<HobbyResponse> hobbies) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getSkillLevel(),
                customer.getHobbyId(),
                customer.isConfirmed(),
                hobbies
        );
    }
}
