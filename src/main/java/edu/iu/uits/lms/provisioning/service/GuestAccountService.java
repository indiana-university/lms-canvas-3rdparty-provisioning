package edu.iu.uits.lms.provisioning.service;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.model.GuestAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonrcook on 1/16/15.
 */
@Service
@Slf4j
public class GuestAccountService {

    @Autowired
    @Qualifier("uaaWebClient")
    private WebClient uaaWebClient;

    @Autowired
    private ToolConfig toolConfig;

    public GuestAccount createGuest(GuestAccount guest) {
        String url = toolConfig.getGuestAccountCreationUrl() + "/accounts/external/invite";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GuestInput input = new GuestInput(guest.getRegistrationEmail(), guest.getFirstName(), guest.getLastName(), guest.getServiceName());
        HttpEntity<GuestInput> requestEntity = new HttpEntity<>(input, headers);

        // use this for returning error messages, if the requestEntity doesn't work out
        GuestAccount ga = new GuestAccount();

        try {
            ResponseEntity<GuestAccount> guestAccountResponseEntity = uaaWebClient.post().uri(url)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(Mono.just(input), GuestInput.class)
                  .retrieve()
                  .toEntity(GuestAccount.class)
                  .block();
            return guestAccountResponseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                GuestErrorResponse ger = mapper.readValue(e.getResponseBodyAsString(), GuestErrorResponse.class);
                log.error("Error creating guest account " + input + ": " + ger.getErrorMessage(), e);
                List<String> errorMessageList = consolidateErrorMessages(ger);
                ga.setErrorMessages(errorMessageList);
            } catch (IOException ioe) {
                log.error("Error parsing error message", ioe);
            }
        }
        return ga;
    }

    public GuestAccount lookupGuestByEmail(String emailAddress) {
        String url = toolConfig.getGuestAccountCreationUrl() + "/accounts/external/search";

        UriTemplate GUEST_TEMPLATE = new UriTemplate(url);

        URI uri = GUEST_TEMPLATE.expand(url, emailAddress);
        log.debug("{}", uri);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
        builder.queryParam("internetAddress", emailAddress);

        try {
            ResponseEntity<GuestAccount> courseResponseEntity = uaaWebClient.get().uri(builder.build().toUri())
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .retrieve()
                  .toEntity(GuestAccount.class)
                  .block();
            log.debug("{}", courseResponseEntity);

            if (courseResponseEntity != null) {
                return courseResponseEntity.getBody();
            }
        } catch (HttpStatusCodeException e) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                GuestErrorResponse ger = mapper.readValue(e.getResponseBodyAsString(), GuestErrorResponse.class);
                log.error("Error retrieving guest account for " + emailAddress + ": " + ger.getErrorMessage(), e);
            } catch (IOException ioe) {
                log.error("Error parsing error message", ioe);
            }
        }

        return null;
    }


    @Data
    @AllArgsConstructor
    private static class GuestInput {
        private String email;
        private String firstName;
        private String lastName;
        private String serviceName;
    }

    @Data
    private static class GuestErrorResponse {
        // There's currently at least 2 styles of error messages that return from uaa.
        // This class covers both scenarios
        private int errorCode;
        private String errorMessage;
        private GuestCreationError error;
    }

    @Data
    private static class GuestCreationError {
        private int code;
        private String message;
        private List<String> messages;
    }

    /**
     * Use this to consolidate error messages in their various formats down to a list
     * @param ger
     * @return List of error messages from GuestErrorResponse
     */
    private List<String> consolidateErrorMessages(GuestErrorResponse ger) {
        List<String> errorMessages = new ArrayList<>();

        if (ger.getErrorMessage()!=null) {
            errorMessages.add(ger.getErrorMessage());
        }

        if (ger.getError()!=null) {
            if (ger.getError().getMessage()!=null) {
                errorMessages.add(ger.getError().getMessage());
            }
            if (ger.getError().getMessages()!=null && !ger.getError().getMessages().isEmpty()) {
                errorMessages.addAll(ger.getError().getMessages());
            }
        }

        return errorMessages;
    }
}
