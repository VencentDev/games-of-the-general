package com.vencentdev.backend.todo.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.todo.dto.PageResponse;
import com.vencentdev.backend.todo.dto.TodoCreateRequest;
import com.vencentdev.backend.todo.dto.TodoResponse;
import com.vencentdev.backend.todo.dto.TodoUpdateRequest;
import com.vencentdev.backend.todo.entity.TodoStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface TodoService {

  PageResponse<TodoResponse> list(
      AuthenticatedUser principal, TodoStatus status, Pageable pageable);

  TodoResponse create(AuthenticatedUser principal, TodoCreateRequest request);

  TodoResponse get(AuthenticatedUser principal, UUID id);

  TodoResponse update(AuthenticatedUser principal, UUID id, TodoUpdateRequest request);

  void delete(AuthenticatedUser principal, UUID id);
}
