package com.pizzaflow.order.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.order.dto.AddressResponse;
import com.pizzaflow.order.dto.CreateAddressRequest;
import com.pizzaflow.order.service.AddressService;
import com.pizzaflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        List<AddressResponse> list = addressService.getUserAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAddressRequest req) {
        AddressResponse res = addressService.createAddress(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Адрес успешно сохранен", res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        addressService.deleteAddress(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Адрес удален", null));
    }
}
