package com.vencentdev.backend.todo.dto;

import com.vencentdev.backend.todo.entity.TodoStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TodoResponse(
    UUID id,
    String title,
    String description,
    TodoStatus status,
    LocalDate dueDate,
    Instant createdAt,
    Instant updatedAt) {}
