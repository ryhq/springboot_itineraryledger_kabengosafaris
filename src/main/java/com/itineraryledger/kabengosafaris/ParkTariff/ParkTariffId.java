package com.itineraryledger.kabengosafaris.ParkTariff;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for ParkTariff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkTariffId implements Serializable {

    private Park park;
    private Tariff tariff;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkTariffId that = (ParkTariffId) o;
        return Objects.equals(park, that.park) && Objects.equals(tariff, that.tariff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(park, tariff);
    }
}
