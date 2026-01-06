package com.itineraryledger.kabengosafaris.ParkActivity;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Park.Park;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for ParkActivity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkActivityId implements Serializable {

    private Park park;
    private Activity activity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkActivityId that = (ParkActivityId) o;
        return Objects.equals(park, that.park) && Objects.equals(activity, that.activity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(park, activity);
    }
}
