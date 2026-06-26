package com.vencentdev.backend.user.validation;

import com.vencentdev.backend.user.dto.UserUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UserValidator implements ConstraintValidator<ValidUserUpdate, UserUpdateRequest> {

  @Override
  public boolean isValid(UserUpdateRequest value, ConstraintValidatorContext context) {
    return true;
  }
}
