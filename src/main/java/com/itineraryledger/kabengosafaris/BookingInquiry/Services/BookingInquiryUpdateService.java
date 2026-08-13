package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.UpdateBookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class BookingInquiryUpdateService {

    private final BookingInquiryRepository repository;
    private final IdObfuscator idObfuscator;
    private final BookingInquiryGetService getService;
    private final com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository customerRepository;
    private final com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository customerEmailRepository;

    @Autowired
    public BookingInquiryUpdateService(
        BookingInquiryRepository repository,
        IdObfuscator idObfuscator,
        BookingInquiryGetService getService,
        com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository customerRepository,
        com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository customerEmailRepository
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.getService = getService;
        this.customerRepository = customerRepository;
        this.customerEmailRepository = customerEmailRepository;
    }

    @AuditLogAnnotation(action = "UPDATE_BOOKING_INQUIRY", description = "Updating booking inquiry", entityType = "BookingInquiry", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateInquiry(String idObfuscated, UpdateBookingInquiryDTO updateDTO) {
        log.info("Updating booking inquiry with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode inquiry ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid inquiry ID", "INVALID_INQUIRY_ID")
                );
            }

            BookingInquiry inquiry = repository.findById(id).orElse(null);
            if (inquiry == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Booking inquiry not found", "INQUIRY_NOT_FOUND")
                );
            }

            if (updateDTO.getAdminNotes() != null) inquiry.setAdminNotes(updateDTO.getAdminNotes());

            if (updateDTO.getStatus() != null) {
                InquiryStatus oldStatus = inquiry.getStatus();
                // the status arrives as a String so a blank can CLEAR it; parse it once
                InquiryStatus requestedStatus = updateDTO.getStatus().isBlank()
                    ? null
                    : InquiryStatus.valueOf(updateDTO.getStatus().trim());
                inquiry.setStatus(requestedStatus);

                if (requestedStatus == InquiryStatus.CONTACTED && oldStatus != InquiryStatus.CONTACTED && inquiry.getContactedAt() == null) {
                    inquiry.setContactedAt(LocalDateTime.now());
                }
                if (requestedStatus == InquiryStatus.CONVERTED && oldStatus != InquiryStatus.CONVERTED && inquiry.getConvertedAt() == null) {
                    inquiry.setConvertedAt(LocalDateTime.now());
                }
            }

            inquiry = repository.save(inquiry);

            BookingInquiryDTO dto = getService.convertToDTO(inquiry);

            log.info("Booking inquiry updated successfully: {}", inquiry.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Booking inquiry updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating booking inquiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update booking inquiry", "INQUIRY_UPDATE_FAILED")
            );
        }
    }

    /**
     * Turn the enquirer into a customer, carrying what they already told us.
     *
     * This is the join between the website and everything downstream: until an
     * enquirer is a customer they cannot be quoted, and retyping their name and
     * email into a blank customer form is exactly how the two records end up
     * disagreeing about the same person.
     *
     * <p>Idempotent. An inquiry that has already been converted returns the
     * customer it made rather than making a second one — double-clicking a button
     * should not create two people.
     *
     * <p>An existing customer with the same email is adopted rather than
     * duplicated: somebody who enquired last year and books again is the same
     * person, and their history is worth more than a clean new record.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> convertToCustomer(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            BookingInquiry inquiry = repository.findById(id).orElse(null);
            if (inquiry == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Inquiry not found", "INQUIRY_NOT_FOUND"));
            }

            if (inquiry.getCustomer() != null) {
                return ResponseEntity.ok(ApiResponse.success(200,
                    "This inquiry is already a customer",
                    java.util.Map.of(
                        "customerId", idObfuscator.encodeId(inquiry.getCustomer().getId()),
                        "customerName", inquiry.getCustomer().getDisplayName(),
                        "created", false)));
            }

            Customer customer = null;
            boolean created = false;

            /*
             * The same person, enquiring again. Matching on email is the only
             * signal a web form gives us, and it is a better one than a name.
             */
            if (inquiry.getEmail() != null && !inquiry.getEmail().isBlank()) {
                customer = customerEmailRepository.findByEmail(inquiry.getEmail().trim())
                    .map(CustomerEmail::getCustomer)
                    .orElse(null);
            }

            if (customer == null) {
                customer = Customer.builder()
                    .customerType(CustomerType.INDIVIDUAL)
                    .firstName(inquiry.getFirstName())
                    .lastName(inquiry.getLastName())
                    .nationality(inquiry.getCountry())
                    .country(inquiry.getCountry())
                    .preferredLanguage(inquiry.getPreferredLocale() != null
                        ? inquiry.getPreferredLocale() : "en")
                    .preferredCurrency("USD")
                    .source(parseSource(inquiry.getSource()))
                    .specialRequests(inquiry.getSpecialRequests())
                    .internalNotes(buildNotes(inquiry))
                    .isActive(true)
                    .isVip(false)
                    .build();

                customer = customerRepository.save(customer);
                customer.setCode(customer.generateCode());

                if (inquiry.getEmail() != null && !inquiry.getEmail().isBlank()) {
                    customer.addEmail(CustomerEmail.builder()
                        .email(inquiry.getEmail().trim())
                        .emailType(CustomerEmail.EmailType.PERSONAL)
                        .isPrimary(true)
                        .isActive(true)
                        .build());
                }
                if (inquiry.getPhone() != null && !inquiry.getPhone().isBlank()) {
                    customer.addPhone(CustomerPhone.builder()
                        .phoneNumber(inquiry.getPhone().trim())
                        .phoneType(CustomerPhone.PhoneType.MOBILE)
                        .isPrimary(true)
                        .isActive(true)
                        .build());
                }

                customer = customerRepository.save(customer);
                created = true;
            }

            inquiry.setCustomer(customer);
            inquiry.setConvertedAt(LocalDateTime.now());
            /*
             * Contacted, not converted. CONVERTED means a booking happened; making
             * somebody a customer is only the first reply, and overstating it
             * would hide them from the queue of people still waiting on a quote.
             */
            if (inquiry.getStatus() == InquiryStatus.NEW) {
                inquiry.setStatus(InquiryStatus.CONTACTED);
                inquiry.setContactedAt(LocalDateTime.now());
            }
            repository.save(inquiry);

            return ResponseEntity.ok(ApiResponse.success(200,
                created ? "Customer created from this inquiry" : "Linked to an existing customer",
                java.util.Map.of(
                    "customerId", idObfuscator.encodeId(customer.getId()),
                    "customerName", customer.getDisplayName(),
                    "created", created)));

        } catch (Exception e) {
            log.error("Error converting inquiry to customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to convert this inquiry", "INQUIRY_CONVERT_FAILED"));
        }
    }

    /**
     * The inquiry's source as the customer records it.
     *
     * The inquiry keeps a free-text source because a web form can post anything;
     * the customer keeps an enum. Anything unrecognised is WEBSITE rather than
     * OTHER, because that is where inquiries come from unless told otherwise.
     */
    private CustomerSource parseSource(String source) {
        if (source == null || source.isBlank()) return CustomerSource.WEBSITE;
        try {
            return CustomerSource.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CustomerSource.WEBSITE;
        }
    }

    /** What they asked for, kept where the office will read it. */
    private String buildNotes(BookingInquiry inquiry) {
        StringBuilder notes = new StringBuilder("From booking inquiry ")
            .append(inquiry.getCode() != null ? inquiry.getCode() : "");
        if (inquiry.getPreferredStartDate() != null) {
            notes.append("\nPreferred dates: ").append(inquiry.getPreferredStartDate());
            if (inquiry.getPreferredEndDate() != null) {
                notes.append(" → ").append(inquiry.getPreferredEndDate());
            }
        }
        if (inquiry.getAdults() != null) {
            notes.append("\nTravellers: ").append(inquiry.getAdults()).append(" adult(s)");
            if (inquiry.getChildren() != null && inquiry.getChildren() > 0) {
                notes.append(", ").append(inquiry.getChildren()).append(" child(ren)");
            }
        }
        if (inquiry.getMessage() != null && !inquiry.getMessage().isBlank()) {
            notes.append("\n\nWhat they said:\n").append(inquiry.getMessage());
        }
        return notes.toString();
    }
}
