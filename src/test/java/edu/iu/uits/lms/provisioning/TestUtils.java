package edu.iu.uits.lms.provisioning;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TestUtils {

   public static String defaultUseragent() {
      return "foobar";
   }

   public static Jwt createJwtToken(String client) {
      return createJwtToken(client, client);
   }

   public static Jwt createJwtToken(String client, String username) {
      Jwt jwt = Jwt.withTokenValue("fake-token")
            .header("typ", "JWT")
            .header("alg", SignatureAlgorithm.RS256.getValue())
            .claim("user_name", username)
            .claim("client_id", client)
            .notBefore(Instant.now())
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .subject(client)
            .build();

      return jwt;
   }

}
