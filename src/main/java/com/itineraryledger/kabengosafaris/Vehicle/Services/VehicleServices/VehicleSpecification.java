package com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices;

import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class VehicleSpecification {

    public static Specification<Vehicle> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Vehicle> registrationNumberLike(String registrationNumber) {
        return (root, query, cb) -> {
            if (registrationNumber == null || registrationNumber.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("registrationNumber")), "%" + registrationNumber.toLowerCase() + "%");
        };
    }

    public static Specification<Vehicle> hasType(VehicleType type) {
        return (root, query, cb) -> {
            if (type == null) return cb.conjunction();
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<Vehicle> makeLike(String make) {
        return (root, query, cb) -> {
            if (make == null || make.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("make")), "%" + make.toLowerCase() + "%");
        };
    }

    public static Specification<Vehicle> modelLike(String model) {
        return (root, query, cb) -> {
            if (model == null || model.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("model")), "%" + model.toLowerCase() + "%");
        };
    }

    public static Specification<Vehicle> hasYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) return cb.conjunction();
            return cb.equal(root.get("year"), year);
        };
    }

    public static Specification<Vehicle> hasFuelType(FuelType fuelType) {
        return (root, query, cb) -> {
            if (fuelType == null) return cb.conjunction();
            return cb.equal(root.get("fuelType"), fuelType);
        };
    }

    public static Specification<Vehicle> minCapacity(Integer minCapacity) {
        return (root, query, cb) -> {
            if (minCapacity == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity);
        };
    }

    public static Specification<Vehicle> maxCapacity(Integer maxCapacity) {
        return (root, query, cb) -> {
            if (maxCapacity == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("capacity"), maxCapacity);
        };
    }

    public static Specification<Vehicle> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return cb.conjunction();
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Vehicle> insuranceExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return cb.conjunction();
            if (expired) {
                return cb.and(
                    cb.isNotNull(root.get("insuranceExpiryDate")),
                    cb.lessThan(root.get("insuranceExpiryDate"), LocalDate.now())
                );
            } else {
                return cb.or(
                    cb.isNull(root.get("insuranceExpiryDate")),
                    cb.greaterThanOrEqualTo(root.get("insuranceExpiryDate"), LocalDate.now())
                );
            }
        };
    }

    public static Specification<Vehicle> inspectionExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return cb.conjunction();
            if (expired) {
                return cb.and(
                    cb.isNotNull(root.get("inspectionExpiryDate")),
                    cb.lessThan(root.get("inspectionExpiryDate"), LocalDate.now())
                );
            } else {
                return cb.or(
                    cb.isNull(root.get("inspectionExpiryDate")),
                    cb.greaterThanOrEqualTo(root.get("inspectionExpiryDate"), LocalDate.now())
                );
            }
        };
    }

    public static Specification<Vehicle> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("registrationNumber")), pattern),
                cb.like(cb.lower(root.get("make")), pattern),
                cb.like(cb.lower(root.get("model")), pattern)
            );
        };
    }
}
