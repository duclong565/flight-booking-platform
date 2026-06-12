package com.deanflights.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * VIA_DTO makes list endpoints serialize as a stable { content, page } shape instead of
 * Spring's internal PageImpl form (which logs an "unstable" warning). See api-conventions.md §D.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
