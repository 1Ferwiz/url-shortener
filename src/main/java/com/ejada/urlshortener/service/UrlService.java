package com.ejada.urlshortener.service;

import com.ejada.urlshortener.dto.CreateUrlRequest;
import com.ejada.urlshortener.dto.UrlResponse;

import java.util.List;

public interface UrlService {

    UrlResponse createShortUrl(CreateUrlRequest request);

    UrlResponse getOriginalUrl(String shortCode);

    List<UrlResponse> getAllUrls();
}