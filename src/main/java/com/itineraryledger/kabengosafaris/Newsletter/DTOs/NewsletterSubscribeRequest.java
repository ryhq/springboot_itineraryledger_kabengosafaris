package com.itineraryledger.kabengosafaris.Newsletter.DTOs;

import com.itineraryledger.kabengosafaris.Attribution.AttributionRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewsletterSubscribeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private String locale = "en";

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    /**
     * How the visitor reached us, collected by the page from the URL and the referrer.
     * Optional: a form posted without it simply records nothing rather than failing.
     */
    private AttributionRequest attribution;

    public AttributionRequest getAttribution() { return attribution; }

    public void setAttribution(AttributionRequest attribution) { this.attribution = attribution; }

}
