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
    public ResponseEntity<?> getRentalClientById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) RentalClientType clientType,
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        @RequestParam(required = false) String companyName,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy
    ) {
        return rentalClientGetService.getRentalClientById(idObfuscated, clientType, firstName, lastName, companyName, phone, email, isActive, keyword, sortBy);
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
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return rentalClientGetService.getAllRentalClients(
            clientType, firstName, lastName, companyName, phone, email,
            isActive, keyword, includeStats, page, size, sortBy, sortDirection
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

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_RENTAL_CLIENT')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("rental client", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
