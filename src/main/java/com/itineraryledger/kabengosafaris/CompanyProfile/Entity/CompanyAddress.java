package com.itineraryledger.kabengosafaris.CompanyProfile.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One of the company's addresses.
 *
 * A postal box is not a street, and the office a guest walks into is not always the one on the
 * invoice — so the type says which is which, and `isPrimary` says which one a document prints when
 * it only has room for one.
 */
@Entity
@Table(name = "company_addresses", indexes = @Index(name = "idx_company_address_profile", columnList = "company_profile_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAddress {

    public enum AddressType { OFFICE, POSTAL, BILLING, WAREHOUSE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile companyProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 50)
    @Builder.Default
    private AddressType addressType = AddressType.OFFICE;

    @Column(length = 100)
    private String label;

    @Column(name = "line_one", length = 200)
    private String lineOne;

    @Column(name = "line_two", length = 200)
    private String lineTwo;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** The address as a document prints it: one line per part that exists, nothing empty. */
    public String formatted() {
        List<String> parts = new ArrayList<>();
        for (String part : new String[] { lineOne, lineTwo, city, region, postalCode, country }) {
            if (part != null && !part.isBlank()) parts.add(part.trim());
        }
        return String.join(", ", parts);
    }
}
