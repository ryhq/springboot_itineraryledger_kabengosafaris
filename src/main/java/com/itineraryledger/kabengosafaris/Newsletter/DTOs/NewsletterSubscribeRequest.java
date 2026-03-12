package com.itineraryledger.kabengosafaris.Newsletter.DTOs;

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
}
