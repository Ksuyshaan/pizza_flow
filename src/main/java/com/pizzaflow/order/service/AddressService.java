package com.pizzaflow.order.service;

import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.delivery.entity.DeliveryZone;
import com.pizzaflow.delivery.repository.DeliveryZoneRepository;
import com.pizzaflow.order.dto.AddressResponse;
import com.pizzaflow.order.dto.CreateAddressRequest;
import com.pizzaflow.order.entity.Address;
import com.pizzaflow.order.entity.User;
import com.pizzaflow.order.repository.AddressRepository;
import com.pizzaflow.order.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository, DeliveryZoneRepository deliveryZoneRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(Long userId, CreateAddressRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        DeliveryZone zone = deliveryZoneRepository.findById(req.getDeliveryZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Зона доставки не найдена"));

        Address address = new Address(
                user,
                zone,
                req.getCity(),
                req.getStreet(),
                req.getHouseNumber(),
                req.getApartment(),
                req.getEntrance(),
                req.getFloor(),
                req.isDefault()
        );
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Адрес не найден"));
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Адрес не найден");
        }
        addressRepository.delete(address);
    }
}
