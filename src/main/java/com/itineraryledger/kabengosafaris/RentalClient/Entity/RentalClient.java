package com.itineraryledger.kabengosafaris.RentalClient.Entity;

import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rental_clients",
    indexes = {
        @Index(name = "idx_rc_client_type", columnList = "client_type"),
        @Index(name = "idx_rc_company_name", columnList = "company_name"),
        @Index(name = "idx_rc_last_name", columnList = "last_name"),
        @Index(name = "idx_rc_is_active", columnList = "is_active"),
        @Index(name = "idx_rc_phone", columnList = "phone")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Client type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    @Builder.Default
    private RentalClientType clientType = RentalClientType.INDIVIDUAL;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String address;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public String getDisplayName() {
        if (clientType == RentalClientType.COMPANY) {
            return companyName != null ? companyName : "";
        }
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        return (first + " " + last).trim();
    }
}
