package uk.gov.hmcts.reform.divorce.casemaintenanceservice.client;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Executor;
import org.json.JSONException;
import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.DivorceCaseMaintenancePact;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslFixtureHelper;

import java.util.Map;

import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.AssertionHelper.assertCaseDetails;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslFixtureHelper.getCaseDataContent;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslFixtureHelper.getCaseDataContentWithPath;


public class DivorceCaseMaintenanceSubmitForCitizen extends DivorceCaseMaintenancePact {

    private CaseDataContent caseDataContent;

    private static final String VALID_PAYLOAD_PATH = "json/divorce-submit.json";

    @BeforeAll
    public void setUp() throws Exception {

        caseDataContent = CaseDataContent.builder()
            .eventToken("someEventToken")
            .event(
                Event.builder()
                    .id(createEventId)
                    .summary(DIVORCE_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(DIVORCE_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            )
            .build();
    }

    @BeforeEach
    public void setUpEachTest() throws InterruptedException {
        Thread.sleep(2000);
    }

    @After
    public void teardown() {
        Executor.closeIdleConnections();
    }

    @Pact(provider = "ccdDataStoreAPI_CaseController", consumer = "divorce_caseMaintenanceService")
    RequestResponsePact submitForCitizen(PactDslWithProvider builder) throws Exception  {
        // @formatter:off
        return builder
            .given("A Submit For Citizen is triggered", getCaseDataContentAsMap(caseDataContent))
            .uponReceiving("A Submit For Citizen is triggered")
            .path("/citizens/"
                + USER_ID
                + "/jurisdictions/"
                + jurisdictionId
                + "/case-types/"
                + caseType
                + "/cases")
            .query("ignore-warning=true")
            .method("POST")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonString(getCaseDataContentWithPath(createEventId, VALID_PAYLOAD_PATH)))
            .willRespondWith()
            .status(HttpStatus.SC_CREATED)
            .body(buildCaseDetailsDsl(CASE_ID))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "submitForCitizen")
    public void verifySubmitEventForCitizen() throws Exception {
        caseDataContent = getCaseDataContentWithPath(createEventId, VALID_PAYLOAD_PATH);

        CaseDetails caseDetailsReponse = coreCaseDataApi.submitForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType,true,caseDataContent);

        assertCaseDetails(caseDetailsReponse);
    }

    @Override
    protected Map<String, Object> getCaseDataContentAsMap(CaseDataContent caseDataContent) throws JSONException {
        Map<String, Object> caseDataContentMap = super.getCaseDataContentAsMap(caseDataContent);
        caseDataContentMap.put(EVENT_ID, createEventId);
        return caseDataContentMap;
    }

}
