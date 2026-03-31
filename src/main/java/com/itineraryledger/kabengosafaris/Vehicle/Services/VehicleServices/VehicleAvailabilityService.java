package com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices;

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

    public boolean isVehicleAvailable(Long vehicleId, LocalDate startDate, LocalDate endDate,
                                       Long excludeSafariVehicleId, Long excludeVehicleHireId) {
        List<SafariVehicle> safariConflicts = safariVehicleRepository
            .findOverlappingAssignments(vehicleId, startDate, endDate, excludeSafariVehicleId);
        List<VehicleHire> hireConflicts = vehicleHireRepository
            .findOverlappingHires(vehicleId, startDate, endDate, excludeVehicleHireId);

        return safariConflicts.isEmpty() && hireConflicts.isEmpty();
    }

    public List<String> getConflictDescriptions(Long vehicleId, LocalDate startDate, LocalDate endDate,
                                                 Long excludeSafariVehicleId, Long excludeVehicleHireId) {
        List<String> conflicts = new ArrayList<>();

        safariVehicleRepository.findOverlappingAssignments(vehicleId, startDate, endDate, excludeSafariVehicleId)
            .forEach(sv -> conflicts.add(String.format(
                "Safari '%s' (%s to %s) - Status: %s",
                sv.getSafari().getName(), sv.getStartDate(),
                sv.getEndDate(), sv.getStatus().getDisplayName()
            )));

        vehicleHireRepository.findOverlappingHires(vehicleId, startDate, endDate, excludeVehicleHireId)
            .forEach(vh -> conflicts.add(String.format(
                "Hire to '%s' (%s to %s) - Status: %s",
                vh.getClientName(), vh.getStartDate(),
                vh.getEndDate(), vh.getStatus().getDisplayName()
            )));

        return conflicts;
    }
}
