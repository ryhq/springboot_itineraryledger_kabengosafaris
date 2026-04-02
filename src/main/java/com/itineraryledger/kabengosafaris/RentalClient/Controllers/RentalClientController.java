package com.itineraryledger.kabengosafaris.RentalClient.Controllers;

import com.itineraryledger.kabengosafaris.RentalClient.DTOs.CreateRentalClientDTO;
import com.itineraryledger.kabengosafaris.RentalClient.DTOs.UpdateRentalClientDTO;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import com.itineraryledger.kabengosafaris.RentalClient.Services.CreateRentalClientService;
import com.itineraryledger.kabengosafaris.RentalClient.Services.DeleteRentalClientService;
import com.itineraryledger.kabengosafaris.RentalClient.Services.RentalClientGetService;
import com.itineraryledger.kabengosafaris.RentalClient.Services.UpdateRentalClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rental-clients")
@Slf4j
@RequiredArgsConstructor
public class RentalClientController {

    private final RentalClientGetService rentalClientGetService;
    private final CreateRentalClientService createRentalClientService;
    private final UpdateRentalClientService updateRentalClientService;
    private final DeleteRentalClientService deleteRentalClientService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_RENTAL_CLIENT')")
    public ResponseEntity<?> createRentalClient(@Valid @RequestBody CreateRentalClientDTO createDTO) {
        return createRentalClientService.createRentalClient(createDTO);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_RENTAL_CLIENT')")
    public ResponseEntity<?> getRentalClientById(@PathVariable String idObfuscated) {
        return rentalClientGetService.getRentalClientById(idObfuscated);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_READ_RENTAL_CLIENT')")
    public ResponseEntity<?> getRentalClientsList() {
        return rentalClientGetService.getRentalClientsList();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_RENTAL_CLIENT')")
    public ResponseEntity<?> getAllRentalClients(
        @RequestParam(required = false) RentalClientType clientType,
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        @RequestParam(required = false) String companyName,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return rentalClientGetService.getAllRentalClients(
            clientType, firstName, lastName, companyName, phone, email,
            isActive, keyword, page, size, sortBy, sortDirection
        );
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_RENTAL_CLIENT')")
    public ResponseEntity<?> updateRentalClient(
        @PathVariable String idObfuscated,
        @RequestBody UpdateRentalClientDTO updateDTO
    ) {
        return updateRentalClientService.updateRentalClient(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_RENTAL_CLIENT')")
    public ResponseEntity<?> deleteRentalClients(@RequestBody List<String> idObfuscatedList) {
        return deleteRentalClientService.deleteRentalClients(idObfuscatedList);
    }
}
