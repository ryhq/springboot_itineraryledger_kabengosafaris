package com.itineraryledger.kabengosafaris.CompanyProfile.Controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyAssetService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyProfileGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The company as the outside world sees it: brand, contact details, logo files.
 *
 * Public because the things here are published by definition — an {@code <img>} in an email or on
 * the website carries no bearer token, and the footer of a PDF is handed to clients anyway. Tax
 * numbers, registration and licence numbers are NOT here; documents get those from the model.
 */
@RestController
@RequestMapping("/api/public/company")
@RequiredArgsConstructor
@Slf4j
public class PublicCompanyController {

    private final CompanyProfileGetService getService;
    private final CompanyAssetService assetService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getCompany() {
        return getService.getPublicCompany();
    }

    /** {@code logo-light} · {@code logo-dark} · {@code favicon-light} · {@code favicon-dark} · {@code logo-email} */
    @GetMapping("/assets/{kind}")
    public ResponseEntity<Resource> getAsset(@PathVariable String kind) {
        return assetService.serve(kind);
    }
}
