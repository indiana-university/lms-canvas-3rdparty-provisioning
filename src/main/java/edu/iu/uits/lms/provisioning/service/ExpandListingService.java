package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.config.ExpandConfiguration;
import edu.iu.uits.lms.provisioning.model.expand.EnrollmentGetWrapper;
import edu.iu.uits.lms.provisioning.model.expand.EnrollmentPostWrapper;
import edu.iu.uits.lms.provisioning.model.expand.ExpandEnrollment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.net.URI;

@Service
@Slf4j
public class ExpandListingService {
    private static final String EXPAND_LISTING_URI = "{url}/enrollments/";
    private static final UriTemplate EXPAND_LISTING_TEMPLATE = new UriTemplate(EXPAND_LISTING_URI);

    @Autowired
    @Qualifier("ExpandRestTemplate")
    protected RestTemplate restTemplate;

    @Autowired
    protected ExpandConfiguration expandConfiguration;

   public String getEnrollments(@PathVariable String listingId) {
        URI uri = EXPAND_LISTING_TEMPLATE.expand(expandConfiguration.getBaseApiUrl());
        log.debug(uri.toString());

        EnrollmentGetWrapper enrollmentGetWrapper = new EnrollmentGetWrapper(listingId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            HttpEntity<EnrollmentGetWrapper> enrollmentWrapperHttpEntity = new HttpEntity<>(enrollmentGetWrapper, headers);

            HttpEntity<String> getEnrollmentsResponse = this.restTemplate.exchange(uri, HttpMethod.GET, enrollmentWrapperHttpEntity, String.class);
            log.debug(getEnrollmentsResponse.toString());

            return getEnrollmentsResponse.getBody();

        } catch (HttpClientErrorException hcee) {
            log.error("Error getting expand enrollments", hcee);
        }

        return null;
    }

    public boolean addUserToListing(@RequestParam(name = "canvas_user_id") String canvasUserId,
                                    @RequestParam(name="listing_id") String listingId) {
        URI uri = EXPAND_LISTING_TEMPLATE.expand(expandConfiguration.getBaseApiUrl());
        log.debug(uri.toString());

        EnrollmentPostWrapper enrollmentPostWrapper = new EnrollmentPostWrapper(new ExpandEnrollment(canvasUserId, listingId));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            HttpEntity<EnrollmentPostWrapper> enrollmentWrapperHttpEntity = new HttpEntity<>(enrollmentPostWrapper, headers);

            HttpEntity<String> createEnrollmentResponse = this.restTemplate.exchange(uri, HttpMethod.POST, enrollmentWrapperHttpEntity, String.class);
            log.debug(createEnrollmentResponse.toString());

            HttpStatus responseStatus = ((ResponseEntity<String>) createEnrollmentResponse).getStatusCode();

            if (HttpStatus.CREATED.equals(responseStatus)) {
                return true;
            } else {
                log.error("Error creating enrollment term. Request to Canvas was not successful. Response code: "
                        + responseStatus + ", reason: " + responseStatus.getReasonPhrase()
                        + ", entity: " + createEnrollmentResponse);
            }

        } catch (HttpClientErrorException hcee) {
            log.error("Error adding user to listing", hcee);
        }

        return false;
    }
}
