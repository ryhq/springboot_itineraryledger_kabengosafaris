package com.itineraryledger.kabengosafaris.CompanyProfile.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One of the company's numbers.
 *
 * `isWhatsApp` is not decoration: a voucher that prints a landline for a guest who lands at
 * midnight is worse than one that prints the number somebody actually answers.
 */
@Entity
@Table(name = "company_phones", indexes = @Index(name = "idx_company_phone_profile", columnList = "company_profile_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPhone {

    public enum PhoneType { MOBILE, LANDLINE, WHATSAPP, RECEPTION, RESERVATIONS, EMERGENCY, FAX, TOLL_FREE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile companyProfile;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type", nullable = false, length = 50)
    @Builder.Default
    private PhoneType phoneType = PhoneType.MOBILE;

    @Column(length = 100)
    private String label;

    @Column(name = "is_whatsapp", nullable = false)
    @Builder.Default
    private Boolean isWhatsApp = false;

    /** When somebody actually answers it, for a document that promises support. */
    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

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

    /** "+255 624 110 836" — what a document prints. */
    public String formatted() {
        String cc = countryCode == null ? "" : countryCode.trim();
        String number = phoneNumber == null ? "" : phoneNumber.trim();
        if (cc.isEmpty()) return number;
        return (cc.startsWith("+") ? cc : "+" + cc) + " " + number;
    }
}
