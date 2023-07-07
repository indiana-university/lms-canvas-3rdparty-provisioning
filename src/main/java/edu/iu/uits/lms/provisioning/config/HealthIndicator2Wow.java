package edu.iu.uits.lms.provisioning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@Slf4j
public class HealthIndicator2Wow implements HealthIndicator {

   private final String name;
   private final HealthIndicator delegate;

   public HealthIndicator2Wow(final String name, final HealthIndicator delegate) {
      this.name = name;
      this.delegate = delegate;
   }

   @Override
   public Health health() {
      Health health =  delegate.health();
      log.debug("Health check '{}' {}: {}", name, health.getStatus(), health.getDetails());
      return health;
   }
}
