package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import com.itineraryledger.kabengosafaris.Attribution.AttributionRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingInquiryRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    /**
     * Optional, because the first save of a lead has only a first name and an email.
     *
     * <p>It was required for as long as the form asked everything at once. Now that the form
     * writes the lead down as soon as it knows who it is, insisting on a surname would mean
     * refusing to record somebody who is plainly interested.
     */
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Size(max = 50)
    private String phone;

    @Size(max = 100)
    private String country;

    /*
     * Null, not 1, when the request says nothing about it. These arrive one step at a time
     * now, and a step about dates that defaulted the party size would silently overwrite the
     * answer given on the step before it. The entity still defaults to one adult and no
     * children, so a lead that is never asked still counts as somebody.
     */
    @Min(value = 1, message = "At least 1 adult is required")
    private Integer adults;

    @Min(0)
    private Integer children;

    private String preferredStartDate;
    private String preferredEndDate;

    private String budgetCategory;
    private String tripType;

    /** Experiences from planner step 1 (e.g. ["SAFARI","GREAT_MIGRATION"]). */
    private List<String> interests;

    /** Preferred trip length in days from the planner. */
    private Integer preferredDurationDays;

    /** Destination park identifiers (obfuscated id or slug) from the "Where" step. */
    private List<String> destinationParkIds;

    private String specialRequests;
    private String message;

    private String safariIdentifier;

    private String locale = "en";

    /**
     * How the visitor reached us, collected by the page from the URL and the referrer.
     * Optional: a form posted without it simply records nothing rather than failing.
     */
    private AttributionRequest attribution;

    /** Which step of the form produced this, so an abandoned lead says where it was lost. */
    private Integer step;

    /**
     * A field no human ever sees, and therefore never fills in.
     *
     * <p>There is no captcha anywhere in this codebase and the draft endpoint will create a
     * row from an email alone, so this is the cheap half of the defence: the form renders it
     * hidden, a scripted submitter fills everything it finds, and anything arriving with this
     * set is accepted politely and thrown away. The expensive half is that a draft sends no
     * mail and notifies nobody, so the worst a bot achieves is a row.
     */
    private String website;

}
