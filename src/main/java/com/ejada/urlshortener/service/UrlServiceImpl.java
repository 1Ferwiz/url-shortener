package com.ejada.urlshortener.service;

import com.ejada.urlshortener.dto.CreateUrlRequest;
import com.ejada.urlshortener.dto.UrlResponse;
import com.ejada.urlshortener.entity.Url;
import com.ejada.urlshortener.exception.UrlNotFoundException;
import com.ejada.urlshortener.repository.UrlRepository;
import com.ejada.urlshortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private static final String CACHE_PREFIX = "url:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        String shortCode = generateUniqueShortCode();

        Url url = new Url();
        url.setShortCode(shortCode);
        url.setOriginalUrl(request.getUrl());

        Url savedUrl = urlRepository.save(url);

        return UrlResponse.builder()
                .id(savedUrl.getId())
                .shortCode(savedUrl.getShortCode())
                .url(savedUrl.getOriginalUrl())
                .build();
    }

    private String generateUniqueShortCode() {
        String code;
        do {
            code = shortCodeGenerator.generate();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    @Override
    public UrlResponse getOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        UrlResponse cached = (UrlResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        UrlResponse response = UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .url(url.getOriginalUrl())
                .build();

        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    @Override
    public List<UrlResponse> getAllUrls() {
        return urlRepository.findAll()
                .stream()
                .map(url -> UrlResponse.builder()
                        .id(url.getId())
                        .shortCode(url.getShortCode())
                        .url(url.getOriginalUrl())
                        .build())
                .toList();
    }
}