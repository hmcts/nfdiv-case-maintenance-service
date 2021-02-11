package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.UserService;
import uk.gov.hmcts.reform.idam.client.models.User;

import java.util.Map;

import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.util.AuthUtil.getBearerToken;

@Service
public class CaseService {

    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY = "Divorce draft case submission event";
    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION = "Submitting Draft Divorce Case";

    private static final String DIVORCE_CASE_PATCH_EVENT_SUMMARY = "Divorce case patch event";
    private static final String DIVORCE_CASE_PATCH_EVENT_DESCRIPTION = "Patching Divorce Case";

    @Value("${ccd.jurisdictionid}")
    private String jurisdictionId;

    @Value("${ccd.casetype}")
    private String caseType;

    @Value("${ccd.eventid.create-draft}")
    private String createDraftEventId;

    @Value("${ccd.eventid.patch}")
    private String patchEventId;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    public CaseDetails submitDraftCase(final Map<String, Object> caseData, final String authorisation) {

        final User userDetails = userService.retrieveUser(authorisation);
        final String bearerToken = getBearerToken(authorisation);
        final String serviceAuthToken = authTokenGenerator.generate();
        final String userId = userDetails.getUserDetails().getId();

        final StartEventResponse startEventResponse = coreCaseDataApi.startForCitizen(
            bearerToken,
            serviceAuthToken,
            userId,
            jurisdictionId,
            caseType,
            createDraftEventId
        );

        final CaseDataContent caseDataContent = buildCaseDataContentWith(
            caseData,
            startEventResponse,
            DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY,
            DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION);

        return coreCaseDataApi.submitForCitizen(
            bearerToken,
            serviceAuthToken,
            userId,
            jurisdictionId,
            caseType,
            true,
            caseDataContent
        );
    }

    public CaseDetails patchCase(final String caseId, final Map<String, Object> caseData, final String authorisation) {

        final User userDetails = userService.retrieveUser(authorisation);
        final String bearerToken = getBearerToken(authorisation);
        final String serviceAuthToken = authTokenGenerator.generate();
        final String userId = userDetails.getUserDetails().getId();

        final StartEventResponse startEventResponse = coreCaseDataApi.startEventForCitizen(
            bearerToken,
            serviceAuthToken,
            userId,
            jurisdictionId,
            caseType,
            caseId,
            patchEventId
        );

        final CaseDataContent caseDataContent = buildCaseDataContentWith(
            caseData,
            startEventResponse,
            DIVORCE_CASE_PATCH_EVENT_SUMMARY,
            DIVORCE_CASE_PATCH_EVENT_DESCRIPTION);

        return coreCaseDataApi.submitEventForCitizen(
            bearerToken,
            serviceAuthToken,
            userId,
            jurisdictionId,
            caseType,
            caseId,
            true,
            caseDataContent
        );
    }

    private CaseDataContent buildCaseDataContentWith(final Map<String, Object> caseData,
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
