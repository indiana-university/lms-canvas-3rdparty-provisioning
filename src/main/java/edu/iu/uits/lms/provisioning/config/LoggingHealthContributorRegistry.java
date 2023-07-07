package edu.iu.uits.lms.provisioning.config;

import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class LoggingHealthContributorRegistry extends AutoConfiguredHealthContributorRegistryCopy {

   private static HealthContributor loggingContributor(final Map.Entry<String, HealthContributor> entry) {
      return loggingContributor(entry.getKey(), entry.getValue());
   }

   private static HealthContributor loggingContributor(final String name, final HealthContributor contributor) {
      if (contributor instanceof HealthIndicator){
         return new HealthIndicator2Wow(name, (HealthIndicator)contributor);
      }
      return contributor;
   }

   public LoggingHealthContributorRegistry(Map<String, HealthContributor> contributors, Collection<String> groupNames) {
      // The constructor does not use `registerContributor` on the input map entries
      super(contributors.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, LoggingHealthContributorRegistry::loggingContributor)),
            groupNames);
   }

   @Override
   public void registerContributor(String name, HealthContributor contributor) {
      super.registerContributor(name, loggingContributor(name, contributor));
   }
}
