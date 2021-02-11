package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import feign.FeignException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.CaseMaintenanceServiceApplication;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_EVENT_ID;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_SERVICE_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller.ObjectMapperTestUtil.convertStringToObject;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller.ResourceLoader.loadJson;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CaseMaintenanceServiceApplication.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@PropertySource(value = "classpath:application.yml")
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
public class DraftCaseSubmissionITest extends MockSupport {

    private static final String API_URL = "/case";
    private static final String NO_HELP_WITH_FEES_PATH = "ccd-submission-payload/addresses-no-hwf.json";

    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY = (String) getField(
        CaseService.class,
        "DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY");
    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION = (String) getField(
        CaseService.class,
        "DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION");

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.eventid.create-draft}")
    String createDraftEventId;

    @Autowired
    private MockMvc webClient;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @Test
    public void shouldSubmitDraftCase() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);
        final String userDetails = getUserDetails();
        final CaseDetails caseDetails = CaseDetails.builder().build();
        final String expectedCaseDetails = convertObjectToJsonString(caseDetails);

        final StartEventResponse startEventResponse = createStartEventResponse();
        final CaseDataContent caseDataContent = createCaseDataContent(caseData, startEventResponse);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                createDraftEventId))
            .thenReturn(startEventResponse);
        when(coreCaseDataApi
            .submitForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                true,
                caseDataContent))
            .thenReturn(caseDetails);

        webClient.perform(post(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(expectedCaseDetails)));
    }

    @Test
    public void shouldReturnErrorMessageIfSubmitForCitizenCallReturnsError() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);
        final String userDetails = getUserDetails();
        final int feignStatusCode = BAD_REQUEST.value();
        final FeignException feignException = getMockedFeignException(feignStatusCode);

        final StartEventResponse startEventResponse = createStartEventResponse();
        final CaseDataContent caseDataContent = createCaseDataContent(caseData, startEventResponse);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                createDraftEventId))
            .thenReturn(startEventResponse);
        when(coreCaseDataApi
            .submitForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                true,
                caseDataContent))
            .thenThrow(feignException);

        webClient.perform(post(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString(FEIGN_ERROR)));
    }

    @Test
    public void shouldReturnErrorMessageIfStartForCitizenCallReturnsError() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);
        final String userDetails = getUserDetails();
        final int feignStatusCode = BAD_REQUEST.value();
        final FeignException feignException = getMockedFeignException(feignStatusCode);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                createDraftEventId))
            .thenThrow(feignException);

        webClient.perform(post(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString(FEIGN_ERROR)));
    }

    @Test
    public void shouldReturnServiceUnavailableIfUnableToConnectToAuthorizationService() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);
        final String message = getUserDetails();

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), message);
        when(serviceTokenGenerator.generate()).thenThrow(new HttpClientErrorException(SERVICE_UNAVAILABLE));

        webClient.perform(post(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnForbiddenErrorIfUserTokenIsInvalid() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);
        final String forbiddenMessage = "forbidden message";

        stubUserDetailsEndpoint(FORBIDDEN, new EqualToPattern(USER_TOKEN), forbiddenMessage);

        webClient.perform(post(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString(forbiddenMessage)));
    }

    @Test
    public void shouldReturnBadRequestIfJwtUserTokenIsNull() throws Exception {

        final String caseData = loadJson(NO_HELP_WITH_FEES_PATH);

        webClient.perform(post(API_URL)
            .content(caseData)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestIfCaseDataIsNull() throws Exception {

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, TEST_AUTH_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    private StartEventResponse createStartEventResponse() {

        return StartEventResponse.builder()
            .eventId(TEST_EVENT_ID)
            .token(TEST_TOKEN)
            .build();
    }

    private CaseDataContent createCaseDataContent(final String caseData, final StartEventResponse startEventResponse) {

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(convertStringToObject(caseData, Map.class))
            .build();
    }
}
