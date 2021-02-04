package uk.gov.hmcts.reform.divorce.support;

import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.util.ResourceLoader;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SubmissionUtils {

    private static final String PAYLOAD_CONTEXT_PATH = "ccd-submission-payload/";
    private static final String PETITIONER_DEFAULT_EMAIL = "simulate-delivered@notifications.service.gov.uk";

    public String loadJson(final String fileName, final String testEnvironment, final UserDetails userDetails) {

        String payload = loadJson(fileName, PAYLOAD_CONTEXT_PATH);

        if (!testEnvironment.equals("local")) {
            payload = payload.replaceAll("-aat", "-".concat(testEnvironment));
        }

        return payload.replaceAll(PETITIONER_DEFAULT_EMAIL, userDetails.getEmailAddress());
    }

    public String loadJson(final String fileName, final String contextPath) {

        try {
            return ResourceLoader.loadJson(contextPath + fileName);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error loading JSON file: %s", fileName), e);
        }
    }

    public Map<String, Object> createHeadersWith(final String userToken) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        headers.put(AUTHORIZATION, userToken);

        return headers;
    }
}
