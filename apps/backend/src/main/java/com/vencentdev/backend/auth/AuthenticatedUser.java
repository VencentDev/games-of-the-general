package com.vencentdev.backend.auth;

import java.util.Set;

public record AuthenticatedUser(
    String subject, String email, String displayName, Set<String> roles) {}
