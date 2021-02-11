package uk.gov.hmcts.reform.divorce.ccd;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.divorce.context.IntegrationTest;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.SubmissionUtils;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.divorce.util.RestUtil.postToRestService;

public class DraftCaseSubmissionTest extends IntegrationTest {

    private final SubmissionUtils submissionUtils = new SubmissionUtils();

    @Value("${env}")
    private String testEnvironment;

    @Value("${case.maintenance.case.path}")
    private String casePath;

    @Test
    public void shouldSubmitDraftCase() {

        final UserDetails userDetails = idamTestSupport.createAnonymousCitizenUser();
        final String jsonCase = submissionUtils.loadJson("base-case.json", testEnvironment, userDetails);

        final Response response = submitDraftCaseJson(jsonCase, userDetails.getAuthToken());

        assertThat(response.getStatusCode(), is(OK.value()));
        assertThat(response.getBody().path("id"), notNullValue());
        assertThat(response.getBody().path("state"), is("Draft"));
    }

    private Response submitDraftCaseJson(final String jsonCase, final String userToken) {
        return postToRestService(
            serverUrl + casePath,
            submissionUtils.createHeadersWith(userToken),
            jsonCase
        );
    }
}
