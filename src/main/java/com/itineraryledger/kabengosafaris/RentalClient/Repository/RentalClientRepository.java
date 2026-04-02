package com.itineraryledger.kabengosafaris.RentalClient.Repository;

import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalClientRepository extends JpaRepository<RentalClient, Long>, JpaSpecificationExecutor<RentalClient> {

    @Query("SELECT e.id FROM RentalClient e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM RentalClient e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM RentalClient e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM RentalClient e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
