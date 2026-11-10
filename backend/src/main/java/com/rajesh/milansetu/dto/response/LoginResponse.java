package com.rajesh.milansetu.dto.response;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, ProfileResponse user) { }
