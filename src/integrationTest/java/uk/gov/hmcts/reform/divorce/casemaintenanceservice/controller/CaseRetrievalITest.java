package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.common.collect.ImmutableMap;
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
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CitizenCaseState;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.DuplicateCaseException;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_SERVICE_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_USER_EMAIL;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.D8_PETITIONER_EMAIL;

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
public class CaseRetrievalITest extends MockSupport {
    private static final String API_URL = "/case";
    private static final String DRAFTS_CONTEXT_PATH = "/drafts";

    private static final String AWAITING_PAYMENT_STATE = CitizenCaseState.AWAITING_PAYMENT.getValue();
    private static final String SUBMITTED_PAYMENT_STATE = CitizenCaseState.SUBMITTED.getValue();

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private MockMvc webClient;

    @Test
    public void givenUserTokenIsNull_whenRetrieveCase_thenReturnBadRequest() throws Exception {
        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenInvalidUserToken_whenRetrieveCase_thenReturnForbiddenError() throws Exception {
        final String message = "some message";
        stubUserDetailsEndpoint(HttpStatus.FORBIDDEN, new EqualToPattern(USER_TOKEN), message);

        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString(message)));
    }

    @Test
    public void givenCouldNotConnectToServiceAuth_whenRetrieveCase_thenReturnHttp503() throws Exception {
        final String message = getUserDetails();
        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), message);

        when(serviceTokenGenerator.generate()).thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void givenNoCaseExistsInCcd_whenRetrieveCase_thenReturn404() throws Exception {
        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), getUserDetails());

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);

        when(coreCaseDataApi
            .searchForCitizen(USER_TOKEN, TEST_SERVICE_TOKEN, USER_ID, jurisdictionId, caseType, Collections.emptyMap()))
            .thenReturn(null);

        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    public void givenOnlyOneCaseExistsInCcd_whenRetrieveCase_thenReturnTheCase() throws Exception {
        final String message = getUserDetails();

        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), message);

        final CaseDetails caseDetails = createCaseDetails(1L, CaseState.SUBMITTED.getValue());

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .searchForCitizen(USER_TOKEN, TEST_SERVICE_TOKEN, USER_ID, jurisdictionId, caseType, Collections.emptyMap()))
            .thenReturn(Collections.singletonList(caseDetails));

        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .json(ObjectMapperTestUtil.convertObjectToJsonString(caseDetails))
            );
    }

    @Test
    public void givenMultipleCasesExistsInCcd_whenRetrieveCase_thenReturnTheFirstCase() throws Exception {
        final String message = getUserDetails();
        stubUserDetailsEndpoint(HttpStatus.OK, new EqualToPattern(USER_TOKEN), message);

        final Long caseId1 = 1L;
        final Long caseId2 = 2L;
        final Long caseId3 = 3L;
        final Long caseId4 = 4L;

        final CaseDetails caseDetails1 = createCaseDetails(caseId1, SUBMITTED_PAYMENT_STATE);
        final CaseDetails caseDetails2 = createCaseDetails(caseId2, CitizenCaseState.ISSUED.getValue());
        final CaseDetails caseDetails3 = createCaseDetails(caseId3, CitizenCaseState.PENDING_REJECTION.getValue());
        final CaseDetails caseDetails4 = createCaseDetails(caseId4, CitizenCaseState.PENDING_REJECTION.getValue());

        when(serviceTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);
        when(coreCaseDataApi
            .searchForCitizen(USER_TOKEN, TEST_SERVICE_TOKEN, USER_ID, jurisdictionId, caseType, Collections.emptyMap()))
            .thenReturn(Arrays.asList(caseDetails1, caseDetails2, caseDetails3, caseDetails4));

        webClient.perform(MockMvcRequestBuilders.get(API_URL)
            .header(HttpHeaders.AUTHORIZATION, USER_TOKEN)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isMultipleChoices());
    }

    private CaseDetails createCaseDetails(Long id, String state) {
        return CaseDetails.builder()
            .id(id)
            .state(state)
            .data(ImmutableMap.of(D8_PETITIONER_EMAIL, TEST_USER_EMAIL))
            .build();
    }
}
