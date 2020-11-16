package uk.gov.hmcts.reform.divorce.casemaintenanceservice.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslBuilderForCaseDetailsList.buildSearchResultDsl;

import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Executor;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "ccd", port = "8891")
@SpringBootTest({
    "core_case_data.api.url : localhost:8891"
})
public class DivorceCaseMaintenance_SearchCases {

    public static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    public static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";
    private static final String TOKEN = "someToken";
    private static final String DIVORCE_CASE_SUBMISSION_EVENT_SUMMARY = "DIVORCE_CASE_SUBMISSION_EVENT_SUMMARY";
    private static final String DIVORCE_CASE_SUBMISSION_EVENT_DESCRIPTION = "DIVORCE_CASE_SUBMISSION_EVENT_DESCRIPTION";

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.eventid.create}")
    private String createEventId;

    private static final String USER_ID ="123456";
    private static final String CASE_ID = "654321";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private CaseDataContent caseDataContent;
    private CaseDetails caseDetails;


    @BeforeAll
    public void setUp() throws Exception {
        caseDetails = getCaseDetails("base-case.json");
        StartEventResponse startEventResponse = StartEventResponse.builder()
            .token(TOKEN)
            .caseDetails(caseDetails)
            .eventId(createEventId)
            .build();
        caseDataContent = buildCaseDataContent(startEventResponse);
    }

    @BeforeEach
    public void setUpEachTest() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Pact(provider = "ccd", consumer = "divorce_caseMaintenanceService")
    public RequestResponsePact searchCasesForCitizen(PactDslWithProvider builder) throws Exception {
        // @formatter:off
        return builder
            .given("Search Cases for Citizen is requested")
            .uponReceiving("a request for a valid search case for citizen")
            .path("/searchCases")
            .query("ctid=DIVORCE")
            .method("POST")
            .body(convertObjectToJsonString("searchString"))
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .headers(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, "\\w+\\/[-+.\\w]+;charset=(utf|UTF)-8")
            .status(200)
            .body(buildSearchResultDsl(Long.valueOf(CASE_ID),"somemailaddress@gmail.com",false,false))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "searchCasesForCitizen")
    public void verifySearchCasesForCitizen() throws IOException, JSONException {

        SearchResult searchResult = coreCaseDataApi.searchCases(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, "DIVORCE", convertObjectToJsonString("searchString"));

        assertEquals(searchResult.getTotal() , 123);
        assertEquals(searchResult.getCases().size() ,2 ) ;

        Map<String,Object> dataMap = searchResult.getCases().get(0).getData();
        assertThat(dataMap.get("primaryApplicantForenames"), Is.is("Jon"));
        assertThat(dataMap.get("primaryApplicantSurname"), Is.is("Snow"));

    }

    private File getFile(String fileName) throws FileNotFoundException {
        return org.springframework.util.ResourceUtils.getFile(this.getClass().getResource("/json/" + fileName));
    }

    private CaseDataContent buildCaseDataContent(StartEventResponse startEventResponse) {

        // TODO EMpty for now , may need to Fill it up ?/ TBD

        final Map<String, Object> CASE_DATA_CONTENT = new HashMap<>();
        CASE_DATA_CONTENT.put("eventToken", "token");
        CASE_DATA_CONTENT.put("caseReference","case_reference");

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(DIVORCE_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(DIVORCE_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(CASE_DATA_CONTENT)
            .build();
    }

    protected CaseDetails getCaseDetails(String fileName) throws JSONException, IOException {
        File file = getFile(fileName);
        return  objectMapper.readValue(file, CaseDetails.class);
    }

    @After
    void teardown() {
        Executor.closeIdleConnections();
    }
}
