package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequestStay;

public interface AvailabilityRequestStayRepository extends JpaRepository<AvailabilityRequestStay, Long> {

    List<AvailabilityRequestStay> findByAvailabilityRequestId(Long requestId);

    List<AvailabilityRequestStay> findByStayId(Long stayId);
}
