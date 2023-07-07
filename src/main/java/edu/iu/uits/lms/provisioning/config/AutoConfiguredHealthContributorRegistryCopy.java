package edu.iu.uits.lms.provisioning.config;

import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;

class AutoConfiguredHealthContributorRegistryCopy extends DefaultHealthContributorRegistry {
   private final Collection<String> groupNames;

   AutoConfiguredHealthContributorRegistryCopy(Map<String, HealthContributor> contributors, Collection<String> groupNames) {
      super(contributors);
      this.groupNames = groupNames;
      contributors.keySet().forEach(this::assertDoesNotClashWithGroup);
   }

   public void registerContributor(String name, HealthContributor contributor) {
      this.assertDoesNotClashWithGroup(name);
      super.registerContributor(name, contributor);
   }

   private void assertDoesNotClashWithGroup(String name) {
      Assert.state(!this.groupNames.contains(name), () -> {
         return "HealthContributor with name \"" + name + "\" clashes with group";
      });
   }
}
