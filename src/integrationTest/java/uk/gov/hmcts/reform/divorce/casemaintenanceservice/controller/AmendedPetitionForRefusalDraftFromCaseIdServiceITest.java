package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.CaseMaintenanceServiceApplication;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseState;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.DivorceSessionProperties;
import uk.gov.hmcts.reform.divorce.service.CaseFormatterService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_CASE_REF;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_REASON_ADULTERY;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_RELATIONSHIP;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_SERVICE_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_USER_EMAIL;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REFUSAL_ORDER_REJECTION_REASONS;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_INSUFFICIENT_DETAILS;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_NO_CRITERIA;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_NO_JURISDICTION;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants.YES_VALUE;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CaseMaintenanceServiceApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@PropertySource(value = "classpath:application.yml")
@TestPropertySource(properties = {
    "feign.hystrix.enabled=false",
    "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AmendedPetitionForRefusalDraftFromCaseIdServiceITest extends MockSupport {
    private static final String API_URL = "/casemaintenance/version/1/amended-petition-draft-refusal";
    private static final String TEST_CASE_ID = "1234567891234567";

    @Autowired
    private MockMvc webClient;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private CaseFormatterService caseFormatterService;

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Test
    public void givenJWTTokenIsNull_whenAmendedPetitionDraftForRefusalFromCaseId_thenReturnBadRequest() throws Exception {
        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenInvalidUserToken_whenAmendedPetitionDraftForRefusalFromCaseId_thenReturnForbiddenError() throws Exception {
        final String message = "some message";
        stubUserDetailsEndpoint(HttpStatus.FORBIDDEN, new EqualToPattern(USER_TOKEN), message);
        stubCaseWorkerAuthentication(HttpStatus.OK);

        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString(message)));
    }

    @Test
    public void givenCouldNotConnectToAuthService_whenAmendedPetitionDraftForRefusalFromCaseId_thenReturnHttp503() throws Exception {
        final String solicitorUserDetails = getSolicitorUserDetails();

        when(serviceTokenGenerator.generate()).thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), solicitorUserDetails);
        stubCaseWorkerAuthentication(HttpStatus.OK);

        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void givenValidRequestToAmend_whenAmendedPetitionDraftForRefusalFromCaseId_thenCreateAmendedPetitionDraft() throws Exception {
        final String solicitorUserDetails = getSolicitorUserDetails();

        final Map<String, Object> caseData = new HashMap<>();
        caseData.put(CcdCaseProperties.D8_CASE_REFERENCE, TEST_CASE_REF);
        caseData.put(CcdCaseProperties.D8_LEGAL_PROCEEDINGS, YES_VALUE);
        caseData.put(CcdCaseProperties.D8_DIVORCE_WHO, TEST_RELATIONSHIP);
        caseData.put(CcdCaseProperties.D8_SCREEN_HAS_MARRIAGE_BROKEN, YES_VALUE);
        caseData.put(CcdCaseProperties.D8_PETITIONER_EMAIL, TEST_USER_EMAIL);
        caseData.put(CcdCaseProperties.D8_REASON_FOR_DIVORCE, TEST_REASON_ADULTERY);
        caseData.put(REFUSAL_ORDER_REJECTION_REASONS, ImmutableList.of(
            REJECTION_NO_JURISDICTION, REJECTION_NO_CRITERIA, REJECTION_INSUFFICIENT_DETAILS
        ));
        final CaseDetails oldCase = CaseDetails.builder().data(caseData)
            .id(Long.decode(TEST_CASE_ID)).build();

        final Map<String, Object> draftData = new HashMap<>();

        draftData.put(DivorceSessionProperties.PREVIOUS_CASE_ID, TEST_CASE_ID);
        draftData.put(CcdCaseProperties.D8_DIVORCE_WHO, TEST_RELATIONSHIP);
        draftData.put(CcdCaseProperties.D8_SCREEN_HAS_MARRIAGE_BROKEN, YES_VALUE);
        draftData.put(CcdCaseProperties.D8_DIVORCE_UNIT, CmsConstants.CTSC_SERVICE_CENTRE);
        draftData.put(DivorceSessionProperties.PREVIOUS_REASONS_FOR_DIVORCE_REFUSAL, Collections.singletonList(TEST_REASON_ADULTERY));

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .readForCaseWorker(
                BEARER_CASE_WORKER_TOKEN,
                TEST_SERVICE_TOKEN,
                CASE_WORKER_USER_ID,
                jurisdictionId,
                caseType,
                TEST_CASE_ID))
            .thenReturn(oldCase);

        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), solicitorUserDetails);
        stubCaseWorkerAuthentication(HttpStatus.OK);
        when(caseFormatterService.transformToDivorceSession(any(Map.class))).thenReturn(draftData);

        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .json(ObjectMapperTestUtil
                    .convertObjectToJsonString(draftData)));
    }

    @Test
    public void givenInvalidRequestToAmend_whenAmendedPetitionDraftForRefusalFromCaseId_thenReturn404() throws Exception {
        final String solicitorUserDetails = getSolicitorUserDetails();
        final Map<String, Object> caseData = new HashMap<>();
        caseData.put(REFUSAL_ORDER_REJECTION_REASONS, Collections.singletonList("other"));

        final Long caseId = 1L;
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(caseData).id(caseId).state(CaseState.SUBMITTED.getValue()).build();

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .readForCaseWorker(
                BEARER_CASE_WORKER_TOKEN,
                TEST_SERVICE_TOKEN,
                CASE_WORKER_USER_ID,
                jurisdictionId,
                caseType,
                TEST_CASE_ID))
            .thenReturn(caseDetails);

        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), solicitorUserDetails);
        stubCaseWorkerAuthentication(HttpStatus.OK);

        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenNoCaseToAmend_whenAmendedPetitionDraftForRefusal_thenReturn404() throws Exception {
        final String solicitorUserDetails = getSolicitorUserDetails();

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .readForCaseWorker(
                BEARER_CASE_WORKER_TOKEN,
                TEST_SERVICE_TOKEN,
                CASE_WORKER_USER_ID,
                jurisdictionId,
                caseType,
                TEST_CASE_ID))
            .thenReturn(null);

        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), solicitorUserDetails);
        stubCaseWorkerAuthentication(HttpStatus.OK);

        webClient.perform(MockMvcRequestBuilders.put(API_URL + "/" + TEST_CASE_ID)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
