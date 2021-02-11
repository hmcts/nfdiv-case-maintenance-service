package uk.gov.hmcts.reform.divorce.ccd;

import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.divorce.context.IntegrationTest;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.SubmissionUtils;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.divorce.util.RestUtil.patchToRestService;
import static uk.gov.hmcts.reform.divorce.util.RestUtil.postToRestService;

public class PatchCaseTest extends IntegrationTest {

    private final SubmissionUtils submissionUtils = new SubmissionUtils();

    @Value("${env}")
    private String testEnvironment;

    @Value("${case.maintenance.case.path}")
    private String casePath;

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldPatchCaseFields() {

        final UserDetails userDetails = idamTestSupport.createAnonymousCitizenUser();
        final String jsonCase = submissionUtils.loadJson("initial-case-for-patch.json", testEnvironment, userDetails);

        final Response response = submitDraftCaseJson(jsonCase, userDetails.getAuthToken());

        assertThat(response.getStatusCode(), is(OK.value()));
        final ResponseBody responseBody = response.getBody();
        assertThat(responseBody.path("state"), is("Draft"));
        assertThat(responseBody.path("case_data.D8ScreenHasMarriageBroken"), is("YES"));
        assertThat(responseBody.path("case_data.D8PetitionerFirstName"), nullValue());
        assertThat(responseBody.path("case_data.D8PetitionerLastName"), nullValue());
        assertThat(responseBody.path("case_data.D8DerivedPetitionerCurrentFullName"), nullValue());
        assertThat(responseBody.path("case_data.D8FinancialOrderFor"), contains("petitioner", "children"));

        final Long caseId = responseBody.path("id");
        final String patchWithIdJson = submissionUtils
            .loadJson("patch-case.json", testEnvironment, userDetails)
            .replace("\"caseId\"", caseId.toString());

        final Response patchResponse = patchCaseJson(patchWithIdJson, userDetails.getAuthToken());

        assertThat(patchResponse.getStatusCode(), is(OK.value()));
        assertThat(patchResponse.getBody().path("id"), is(caseId));
        assertThat(patchResponse.getBody().path("state"), is("Draft"));
        assertThat(patchResponse.getBody().path("case_data.D8ScreenHasMarriageBroken"), is("YES"));
        assertThat(patchResponse.getBody().path("case_data.D8PetitionerFirstName"), is("John"));
        assertThat(patchResponse.getBody().path("case_data.D8PetitionerLastName"), is("Jones"));
        assertThat(patchResponse.getBody().path("case_data.D8DerivedPetitionerCurrentFullName"), is("John Jones"));
        assertThat(patchResponse.getBody().path("case_data.D8FinancialOrderFor"), contains("petitioner"));
    }

    private Response submitDraftCaseJson(final String jsonCase, final String userToken) {
        return postToRestService(
            serverUrl + casePath,
            submissionUtils.createHeadersWith(userToken),
            jsonCase
        );
    }

    private Response patchCaseJson(final String jsonCase, final String userToken) {
        return patchToRestService(
            serverUrl + casePath,
            submissionUtils.createHeadersWith(userToken),
            jsonCase,
            emptyMap()
        );
    }
}
