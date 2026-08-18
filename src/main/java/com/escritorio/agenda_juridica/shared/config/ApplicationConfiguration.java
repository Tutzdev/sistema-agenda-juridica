package com.escritorio.agenda_juridica.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
