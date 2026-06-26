package com.vencentdev.backend.user.dto;

import com.vencentdev.backend.user.entity.KycStatus;
import com.vencentdev.backend.user.entity.Role;
import com.vencentdev.backend.user.entity.UserType;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    Role role,
    UserType userType,
    KycStatus kycStatus,
    Instant createdAt,
    Instant updatedAt) {}
