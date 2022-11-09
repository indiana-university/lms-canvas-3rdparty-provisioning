package edu.iu.uits.lms.provisioning.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;

public class ExpandTokenAuthorizationInterceptor implements ClientHttpRequestInterceptor {

    private final String token;

    /**
     * Create a new interceptor which adds a Bearer authorization header
     * for the given token.
     * @param token the token to use
     */
    public ExpandTokenAuthorizationInterceptor(String token) {
        Assert.hasLength(token, "Token must not be empty");
        this.token = token;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Token token=" + token);
        return execution.execute(request, body);
    }
}
