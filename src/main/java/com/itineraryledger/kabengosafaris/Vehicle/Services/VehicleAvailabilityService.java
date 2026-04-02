package com.itineraryledger.kabengosafaris.Vehicle.Services;

import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleAvailabilityService {

    private final SafariVehicleRepository safariVehicleRepository;
    private final VehicleHireRepository vehicleHireRepository;

    /**
     * Result of an availability check, separating hard conflicts (multi-day overlap)
     * from soft conflicts (same-day boundary only, e.g. one ends and another starts on the same date).
     */
    public record AvailabilityResult(List<String> hardConflicts, List<String> softConflicts) {
        public boolean hasHardConflicts() { return !hardConflicts.isEmpty(); }
        public boolean hasSoftConflicts() { return !softConflicts.isEmpty(); }
        public boolean hasAnyConflicts() { return hasHardConflicts() || hasSoftConflicts(); }
    }

    public boolean isVehicleAvailable(Long vehicleId, LocalDate startDate, LocalDate endDate,
                                       Long excludeSafariVehicleId, Long excludeVehicleHireId) {
        AvailabilityResult result = checkAvailability(vehicleId, startDate, endDate, excludeSafariVehicleId, excludeVehicleHireId);
        return !result.hasHardConflicts();
    }

    public AvailabilityResult checkAvailability(Long vehicleId, LocalDate startDate, LocalDate endDate,
                                                 Long excludeSafariVehicleId, Long excludeVehicleHireId) {
        List<String> hardConflicts = new ArrayList<>();
        List<String> softConflicts = new ArrayList<>();

        List<SafariVehicle> safariOverlaps = safariVehicleRepository
            .findOverlappingAssignments(vehicleId, startDate, endDate, excludeSafariVehicleId);

        for (SafariVehicle sv : safariOverlaps) {
            String description = String.format("Safari '%s' (%s to %s) - Status: %s",
                sv.getSafari().getName(), sv.getStartDate(), sv.getEndDate(), sv.getStatus().getDisplayName());

            if (isBoundaryOnlyOverlap(startDate, endDate, sv.getStartDate(), sv.getEndDate())) {
                softConflicts.add(description);
            } else {
                hardConflicts.add(description);
            }
        }

        List<VehicleHire> hireOverlaps = vehicleHireRepository
            .findOverlappingHires(vehicleId, startDate, endDate, excludeVehicleHireId);

        for (VehicleHire vh : hireOverlaps) {
            String clientName = vh.getRentalClient() != null ? vh.getRentalClient().getDisplayName() : "Unknown";
            String description = String.format("Hire to '%s' (%s to %s) - Status: %s",
                clientName, vh.getStartDate(), vh.getEndDate(), vh.getStatus().getDisplayName());

            if (isBoundaryOnlyOverlap(startDate, endDate, vh.getStartDate(), vh.getEndDate())) {
                softConflicts.add(description);
            } else {
                hardConflicts.add(description);
            }
        }

        return new AvailabilityResult(hardConflicts, softConflicts);
    }

    /**
     * @deprecated Use {@link #checkAvailability} instead for categorized conflict handling.
     */
    @Deprecated
    public List<String> getConflictDescriptions(Long vehicleId, LocalDate startDate, LocalDate endDate,
                                                 Long excludeSafariVehicleId, Long excludeVehicleHireId) {
        AvailabilityResult result = checkAvailability(vehicleId, startDate, endDate, excludeSafariVehicleId, excludeVehicleHireId);
        List<String> all = new ArrayList<>(result.hardConflicts());
        all.addAll(result.softConflicts());
        return all;
    }

    /**
     * A boundary-only overlap occurs when the only shared date is a handoff day:
     * - existing assignment ends on the same day the new one starts, OR
     * - existing assignment starts on the same day the new one ends.
     *
     * This allows same-day vehicle turnover (e.g. morning drop-off, afternoon pickup).
     */
    private boolean isBoundaryOnlyOverlap(LocalDate newStart, LocalDate newEnd,
                                           LocalDate existingStart, LocalDate existingEnd) {
        return newStart.isEqual(existingEnd) || newEnd.isEqual(existingStart);
    }
}
