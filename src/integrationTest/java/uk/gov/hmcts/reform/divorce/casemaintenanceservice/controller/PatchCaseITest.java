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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
public class PatchCaseITest extends MockSupport {

    private static final String API_URL = "/case";
    private static final String PATCH_CASE_JSON = "ccd-submission-payload/patch-case.json";
    private static final String CASE_ID = "123456789";

    private static final String DIVORCE_CASE_PATCH_EVENT_SUMMARY = "Divorce case patch event";
    private static final String DIVORCE_CASE_PATCH_EVENT_DESCRIPTION = "Patching Divorce Case";

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.eventid.patch}")
    private String patchEventId;

    @Autowired
    private MockMvc webClient;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @Test
    public void shouldPatchCase() throws Exception {

        final String caseData = loadJson(PATCH_CASE_JSON);
        final String userDetails = getUserDetails();
        final CaseDetails caseDetails = CaseDetails.builder().build();
        final String expectedCaseDetails = convertObjectToJsonString(caseDetails);

        final StartEventResponse startEventResponse = createStartEventResponse();

        final CaseDataContent caseDataContent = createCaseDataContent(caseData, startEventResponse);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startEventForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                CASE_ID,
                patchEventId))
            .thenReturn(startEventResponse);
        when(coreCaseDataApi
            .submitEventForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                CASE_ID,
                true,
                caseDataContent))
            .thenReturn(caseDetails);

        webClient.perform(patch(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(expectedCaseDetails)));
    }

    @Test
    public void shouldReturnBadRequestIfCaseIdMissingFromPayload() throws Exception {

        final String caseData = "{"
            + "\"D8PetitionerFirstName\":\"John\""
            + "}";

        webClient.perform(patch(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Missing 'id' in payload."));
    }

    @Test
    public void shouldReturnBadRequestIfSubmitForCitizenCallReturnsError() throws Exception {

        final String caseData = loadJson(PATCH_CASE_JSON);
        final String userDetails = getUserDetails();
        final int feignStatusCode = BAD_REQUEST.value();
        final FeignException feignException = getMockedFeignException(feignStatusCode);

        final StartEventResponse startEventResponse = createStartEventResponse();
        final CaseDataContent caseDataContent = createCaseDataContent(caseData, startEventResponse);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startEventForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                CASE_ID,
                patchEventId))
            .thenReturn(startEventResponse);
        when(coreCaseDataApi
            .submitEventForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                CASE_ID,
                true,
                caseDataContent))
            .thenThrow(feignException);

        webClient.perform(patch(API_URL)
            .content(caseData)
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString(FEIGN_ERROR)));
    }

    @Test
    public void shouldReturnBadRequestIfStartForCitizenCallReturnsError() throws Exception {

        final String userDetails = getUserDetails();
        final int feignStatusCode = BAD_REQUEST.value();
        final FeignException feignException = getMockedFeignException(feignStatusCode);

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), userDetails);

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .startEventForCitizen(
                USER_TOKEN,
                TEST_SERVICE_TOKEN,
                USER_ID,
                jurisdictionId,
                caseType,
                CASE_ID,
                patchEventId))
            .thenThrow(feignException);

        webClient.perform(patch(API_URL)
            .content(loadJson(PATCH_CASE_JSON))
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString(FEIGN_ERROR)));
    }

    @Test
    public void shouldReturnServiceUnavailableIfUnableToConnectToAuthorizationService() throws Exception {

        stubUserDetailsEndpoint(OK, new EqualToPattern(USER_TOKEN), getUserDetails());
        when(serviceTokenGenerator.generate()).thenThrow(new HttpClientErrorException(SERVICE_UNAVAILABLE));

        webClient.perform(post(API_URL)
            .content(loadJson(PATCH_CASE_JSON))
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnForbiddenErrorIfUserTokenIsInvalid() throws Exception {

        final String forbiddenMessage = "forbidden message";

        stubUserDetailsEndpoint(FORBIDDEN, new EqualToPattern(USER_TOKEN), forbiddenMessage);

        webClient.perform(post(API_URL)
            .content(loadJson(PATCH_CASE_JSON))
            .header(AUTHORIZATION, USER_TOKEN)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString(forbiddenMessage)));
    }

    @Test
    public void shouldReturnBadRequestIfJwtUserTokenIsNull() throws Exception {

        webClient.perform(post(API_URL)
            .content(loadJson(PATCH_CASE_JSON))
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

    @SuppressWarnings("unchecked")
    private CaseDataContent createCaseDataContent(final String caseData, final StartEventResponse startEventResponse) {

        final Map<String, Object> caseDataMap = convertStringToObject(caseData, Map.class);
        caseDataMap.remove("id");

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(DIVORCE_CASE_PATCH_EVENT_SUMMARY)
                    .description(DIVORCE_CASE_PATCH_EVENT_DESCRIPTION)
                    .build()
            ).data(caseDataMap)
            .build();
    }
}
