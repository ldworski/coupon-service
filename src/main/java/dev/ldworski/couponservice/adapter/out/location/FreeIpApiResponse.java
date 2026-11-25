package dev.ldworski.couponservice.adapter.out.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record FreeIpApiResponse(String countryCode) {}
