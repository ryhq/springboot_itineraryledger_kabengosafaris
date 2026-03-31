package com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices;

import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class VehicleHireSpecification {

    public static Specification<VehicleHire> hasVehicleId(Long vehicleId) {
        return (root, query, cb) -> {
            if (vehicleId == null) return cb.conjunction();
            return cb.equal(root.get("vehicle").get("id"), vehicleId);
        };
    }

    public static Specification<VehicleHire> clientNameLike(String clientName) {
        return (root, query, cb) -> {
            if (clientName == null || clientName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("clientName")), "%" + clientName.toLowerCase() + "%");
        };
    }

    public static Specification<VehicleHire> hasStatus(HireStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<VehicleHire> hasPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) -> {
            if (paymentStatus == null) return cb.conjunction();
            return cb.equal(root.get("paymentStatus"), paymentStatus);
        };
    }

    public static Specification<VehicleHire> startDateAfter(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("startDate"), date);
        };
    }

    public static Specification<VehicleHire> startDateBefore(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("startDate"), date);
        };
    }

    public static Specification<VehicleHire> endDateAfter(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("endDate"), date);
        };
    }

    public static Specification<VehicleHire> endDateBefore(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("endDate"), date);
        };
    }

    public static Specification<VehicleHire> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("clientName")), pattern),
                cb.like(cb.lower(root.get("clientEmail")), pattern),
                cb.like(cb.lower(root.get("clientPhone")), pattern),
                cb.like(cb.lower(root.get("pickupLocation")), pattern),
                cb.like(cb.lower(root.get("dropoffLocation")), pattern)
            );
        };
    }
}
