package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerPublicProfileResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerProfileRequest;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.entity.CustomerHobby;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerHobbyRepository;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recommendation.event.CustomerProfileChangedEvent;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
    @Transactional(readOnly = true)
    public CustomerPublicProfileResponse getPublicProfileByUserId(UUID userId) {
        Customer customer = findCustomer(userId);
        var user = customer.getUser();

        return new CustomerPublicProfileResponse(
                user.getUserId(),
                customer.getId(),
                user.getFullName(),
                user.getAvatarUrl(),
                customer.getSkillLevel(),
                user.getGender(),
                user.getBod(),
                selectedHobbies(customer)
        );
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request) {
        Customer customer = findCustomer(userId);
        customer.setSkillLevel(request.skillLevel());
        Customer savedCustomer = customerRepository.save(customer);
        eventPublisher.publishEvent(new CustomerProfileChangedEvent(savedCustomer.getId()));
        return toResponse(savedCustomer, selectedHobbies(savedCustomer));
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateHobbies(UUID userId, UpdateCustomerHobbiesRequest request) {
        Customer customer = findCustomer(userId);
        Set<UUID> hobbyIds = request.hobbyIds();

        List<HobbyResponse> hobbies = hobbyService.getActiveByIds(hobbyIds);
        if (hobbies.size() != hobbyIds.size()) {
            throw new AppException(CustomerErrorCode.INVALID_HOBBY_SELECTION);
        }

        customerHobbyRepository.deleteAllByCustomerId(customer.getId());
        List<CustomerHobby> customerHobbies = hobbyIds.stream()
                .map(hobbyId -> new CustomerHobby(customer.getId(), hobbyId))
                .toList();
        customerHobbyRepository.saveAll(customerHobbies);
        eventPublisher.publishEvent(new CustomerProfileChangedEvent(customer.getId()));

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
                customer.isConfirmed(),
                hobbies
        );
    }
}
