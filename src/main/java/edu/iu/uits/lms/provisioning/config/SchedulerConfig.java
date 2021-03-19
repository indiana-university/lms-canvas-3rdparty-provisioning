package edu.iu.uits.lms.provisioning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@Profile("batch")
public class SchedulerConfig {

   @Bean
   public ScheduledExecutorService taskExecutor() {
      ScheduledExecutorService delegateExecutor = Executors.newSingleThreadScheduledExecutor();
      SecurityContext schedulerContext = createSchedulerSecurityContext();
      return new DelegatingSecurityContextScheduledExecutorService(delegateExecutor, schedulerContext);
   }

   private SecurityContext createSchedulerSecurityContext() {
      SecurityContext context = SecurityContextHolder.createEmptyContext();

//      Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(AuthoritiesUtil.ROLE_RAC_ADMIN);
//      Authentication authentication = new UsernamePasswordAuthenticationToken(
//            batchUsername,
//            "N/A",
//            authorities
//      );
//      context.setAuthentication(authentication);

      return context;
   }
}