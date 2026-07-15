package com.itineraryledger.kabengosafaris.BookingInquiry.Entity;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripInterest;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "booking_inquiries", indexes = {
    @Index(name = "idx_inquiry_code", columnList = "code"),
    @Index(name = "idx_inquiry_email", columnList = "email"),
    @Index(name = "idx_inquiry_status", columnList = "status"),
    @Index(name = "idx_inquiry_created_at", columnList = "created_at"),
    @Index(name = "idx_inquiry_itinerary_id", columnList = "itinerary_id")
},
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inquiry_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private Integer adults = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer children = 0;

    private LocalDate preferredStartDate;

    private LocalDate preferredEndDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BudgetCategory budgetCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TripType tripType;

    /** Experiences the client is interested in (planner step 1, multi-select). */
    @ElementCollection(targetClass = TripInterest.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_inquiry_interests",
            joinColumns = @JoinColumn(name = "inquiry_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 30)
    @Builder.Default
    private Set<TripInterest> interests = new HashSet<>();

    /** Preferred trip length in days (planner step 2). */
    private Integer preferredDurationDays;

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(length = 50)
    @Builder.Default
    private String source = "WEBSITE";

    @Column(length = 10)
    @Builder.Default
    private String preferredLocale = "en";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @Column(length = 200)
    private String itineraryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDateTime contactedAt;

    private LocalDateTime convertedAt;

    @Transient
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    @Transient
    public int getTotalTravelers() {
        return (adults != null ? adults : 0) + (children != null ? children : 0);
    }
}
