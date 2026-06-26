package com.vencentdev.backend.user.mapper;

import com.vencentdev.backend.user.entity.ContactInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {
  ContactInfo copy(ContactInfo contactInfo);
}
