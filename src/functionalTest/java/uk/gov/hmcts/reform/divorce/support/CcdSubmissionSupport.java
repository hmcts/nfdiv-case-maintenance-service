package uk.gov.hmcts.reform.divorce.support;

import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.divorce.context.IntegrationTest;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.util.RestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

public abstract class CcdSubmissionSupport extends IntegrationTest {

    protected final SubmissionUtils submissionUtils = new SubmissionUtils();

    @Value("${case.maintenance.submission.context-path}")
    private String contextPath;

    @Value("${case.maintenance.solicitor.submission.context-path}")
    private String contextSolicitorPath;

    @Value("${case.maintenance.bulk.submission.context-path}")
    private String contextBulkCasePath;

    @Value("${env}")
    private String testEnvironment;

    protected void submitAndAssertSuccess(String fileName) {
        Response cmsResponse = submitCase(fileName);
        assertOkResponseAndCaseIdIsNotZero(cmsResponse);
    }

    protected void solicitorSubmitAndAssertSuccess(String fileName) {
        Response cmsResponse = solicitorSubmitCase(fileName);
        assertOkResponseAndCaseIdIsNotZero(cmsResponse);
    }

    private Response submitCase(String fileName) {
        return submitCase(fileName, getUserDetails());
    }

    protected Response submitCase(String fileName, UserDetails userDetails) {
        final String jsonCase = submissionUtils.loadJson(fileName, testEnvironment, userDetails);
        return submitCaseJson(jsonCase, userDetails.getAuthToken(), getSubmissionRequestUrl());
    }

    private Response solicitorSubmitCase(String fileName) {
        return solicitorSubmitCase(fileName, getSolicitorUser());
    }

    protected Response solicitorSubmitCase(String fileName, UserDetails userDetails) {
        final String jsonCase = submissionUtils.loadJson(fileName, testEnvironment, userDetails);
        return submitCaseJson(jsonCase, userDetails.getAuthToken(), getSolicitorSubmissionRequestUrl());
    }

    protected Response submitCaseJson(String jsonCase, String userToken, String contextUrl) {
        return
            RestUtil.postToRestService(
                contextUrl,
                submissionUtils.createHeadersWith(userToken),
                jsonCase
            );
    }

    protected Response submitBulkCase(String fileName, UserDetails userDetails) {
        final String jsonCase = submissionUtils.loadJson(fileName, testEnvironment, userDetails);
        return submitCaseJson(jsonCase, userDetails.getAuthToken(), getBulkCaseSubmissionRequestUrl());
    }

    protected String getSubmissionRequestUrl() {
        return serverUrl + contextPath;
    }

    protected String getSolicitorSubmissionRequestUrl() {
        return serverUrl + contextSolicitorPath;
    }

    protected String getBulkCaseSubmissionRequestUrl() {
        return serverUrl + contextBulkCasePath;
    }

    protected void assertOkResponseAndCaseIdIsNotZero(Response cmsResponse) {
        assertEquals(cmsResponse.getBody().asString(), OK.value(), cmsResponse.getStatusCode());
        assertNotEquals((Long) 0L, cmsResponse.getBody().path("caseId"));
    }

    protected void assertCaseStatus(Response cmsResponse, String caseStatus) {
        assertTrue(String.format("Expected [%s] status not found", caseStatus),
            cmsResponse.getBody().asString().contains(caseStatus));
    }
}
