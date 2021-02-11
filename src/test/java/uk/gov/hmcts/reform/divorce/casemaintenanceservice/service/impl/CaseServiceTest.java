package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.UserService;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_AUTHORISATION;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_BEARER_AUTHORISATION;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_CASE_TYPE;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_JURISDICTION_ID;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_SERVICE_TOKEN;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants.D_8_HELP_WITH_FEES_NEED_HELP;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants.NO_VALUE;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants.SOLICITOR_CREATE_EVENT_ID;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    private static final String USER_ID = "someUserId";
    private static final String CREATE_DRAFT_EVENT_ID = "draftCreate";
    private static final String PATCH_EVENT_ID = "patchCase";

    @Mock
    private UserService userService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @InjectMocks
    private CaseService caseService;

    @Before
    public void setup() {
        setField(caseService, "jurisdictionId", TEST_JURISDICTION_ID);
        setField(caseService, "caseType", TEST_CASE_TYPE);
        setField(caseService, "createDraftEventId", CREATE_DRAFT_EVENT_ID);
        setField(caseService, "patchEventId", PATCH_EVENT_ID);
    }

    @Test
    public void shouldSubmitDraftCaseAndReturnCaseDetails() {

        final Map<String, Object> caseData = ImmutableMap.of(D_8_HELP_WITH_FEES_NEED_HELP, NO_VALUE);
        final User userDetails = new User(TEST_AUTH_TOKEN, UserDetails.builder().id(USER_ID).build());
        final StartEventResponse startEventResponse = StartEventResponse.builder()
            .eventId(SOLICITOR_CREATE_EVENT_ID)
            .token(TEST_AUTH_TOKEN)
            .build();

        final CaseDataContent caseDataContent = getBuild(
            caseData,
            startEventResponse,
            "Divorce draft case submission event",
            "Submitting Draft Divorce Case");

        final CaseDetails caseDetails = mock(CaseDetails.class);

        when(userService.retrieveUser(TEST_AUTHORISATION)).thenReturn(userDetails);
        when(authTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);

        when(coreCaseDataApi
            .startForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                CREATE_DRAFT_EVENT_ID))
            .thenReturn(startEventResponse);

        when(coreCaseDataApi
            .submitForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                true,
                caseDataContent))
            .thenReturn(caseDetails);

        final CaseDetails actualCaseDetails = caseService.submitDraftCase(caseData, TEST_AUTHORISATION);

        assertThat(actualCaseDetails).isEqualTo(caseDetails);

        verify(userService).retrieveUser(TEST_AUTHORISATION);
        verify(authTokenGenerator).generate();
        verify(coreCaseDataApi)
            .startForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                CREATE_DRAFT_EVENT_ID);
        verify(coreCaseDataApi)
            .submitForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                true,
                caseDataContent);
    }

    @Test
    public void shouldPatchCaseAndReturnCaseDetails() {

        final Map<String, Object> caseData = ImmutableMap.of(D_8_HELP_WITH_FEES_NEED_HELP, NO_VALUE);
        final User userDetails = new User(TEST_AUTH_TOKEN, UserDetails.builder().id(USER_ID).build());
        final StartEventResponse startEventResponse = StartEventResponse.builder()
            .eventId(SOLICITOR_CREATE_EVENT_ID)
            .token(TEST_AUTH_TOKEN)
            .build();

        final CaseDataContent caseDataContent = getBuild(
            caseData,
            startEventResponse,
            "Divorce case patch event",
            "Patching Divorce Case");

        final CaseDetails caseDetails = mock(CaseDetails.class);

        when(userService.retrieveUser(TEST_AUTHORISATION)).thenReturn(userDetails);
        when(authTokenGenerator.generate()).thenReturn(TEST_SERVICE_TOKEN);

        when(coreCaseDataApi
            .startEventForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                TEST_CASE_ID,
                PATCH_EVENT_ID))
            .thenReturn(startEventResponse);

        when(coreCaseDataApi
            .submitEventForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                TEST_CASE_ID,
                true,
                caseDataContent))
            .thenReturn(caseDetails);

        final CaseDetails actualCaseDetails = caseService.patchCase(TEST_CASE_ID, caseData, TEST_AUTHORISATION);

        assertThat(actualCaseDetails).isEqualTo(caseDetails);

        verify(userService).retrieveUser(TEST_AUTHORISATION);
        verify(authTokenGenerator).generate();
        verify(coreCaseDataApi)
            .startEventForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                TEST_CASE_ID,
                PATCH_EVENT_ID);
        verify(coreCaseDataApi)
            .submitEventForCitizen(
                TEST_BEARER_AUTHORISATION,
                TEST_SERVICE_TOKEN,
                USER_ID,
                TEST_JURISDICTION_ID,
                TEST_CASE_TYPE,
                TEST_CASE_ID,
                true,
                caseDataContent);
    }

    private CaseDataContent getBuild(final Map<String, Object> caseData,
                                     final StartEventResponse startEventResponse,
                                     final String summary,
                                     final String description) {

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(summary)
                    .description(description)
                    .build()
            ).data(caseData)
            .build();
    }
}