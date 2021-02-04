package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.models.User;

import java.util.Map;

import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.util.AuthUtil.getBearerToken;

@Service
public class CaseService extends BaseCcdCaseService {

    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY = "Divorce draft case submission event";
    private static final String DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION = "Submitting Draft Divorce Case";

    public CaseDetails submitDraftCase(final Map<String, Object> caseData, final String authorisation) {

        final User userDetails = getUser(authorisation);
        final String bearerToken = getBearerToken(authorisation);
        final String serviceAuthToken = getServiceAuthToken();
        final String userId = userDetails.getUserDetails().getId();

        final StartEventResponse startEventResponse = coreCaseDataApi.startForCitizen(
            bearerToken,
            serviceAuthToken,
            userId,
            jurisdictionId,
            caseType,
            createDraftEventId
        );

        final CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(DIVORCE_DRAFT_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(caseData)
            .build();

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
}
