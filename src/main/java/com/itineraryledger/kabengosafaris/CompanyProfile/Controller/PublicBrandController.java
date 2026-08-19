package com.itineraryledger.kabengosafaris.CompanyProfile.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyProfileGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * How the app should look for this company: name, mark, accent, radius, font, and the URLs of the
 * logo and favicon files.
 *
 * Its own path rather than a corner of /api/public/company, because this is the FIRST request the
 * panel makes — before login, before a token exists — and what it answers decides the colour of the
 * sign-in screen. It also means a bundle built for one company but pointed at another company's API
 * shows the API's company rather than the one compiled into it.
 */
@RestController
@RequestMapping("/api/public/brand")
@RequiredArgsConstructor
public class PublicBrandController {

    private final CompanyProfileGetService getService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getBrand() {
        return getService.getBrand();
    }
}
