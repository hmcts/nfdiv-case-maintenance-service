package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CaseAccessApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.UserId;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.UserDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.CcdAccessService;

@Service
public class CcdAccessServiceImpl extends BaseCcdCaseService implements CcdAccessService {
    private static final String LETTER_HOLDER_CASE_FIELD = "AosLetterHolderId";
    private static final String RECEIVED_AOS_FIELD = "ReceivedAOSfromResp";

    @Autowired
    private CaseAccessApi caseAccessApi;

    @Override
    public void linkRespondent(String authorisation, String caseId, String letterHolderId) {
        UserDetails caseworkerUser = getAnonymousCaseWorkerDetails();

        CaseDetails caseDetails = coreCaseDataApi.readForCaseWorker(
            caseworkerUser.getAuthToken(),
            getServiceAuthToken(),
            caseworkerUser.getId(),
            jurisdictionId,
            caseType,
            caseId
        );

        if (!letterHolderIdMatchesAndNotLinked(caseDetails, letterHolderId)) {
            throw new CaseNotFoundException(String.format("Case with caseId [%s] and letter holder id [%s] not found "
                    + "or case already has linked respondent",
                caseId, letterHolderId));
        }

        UserDetails respondentUser = getUserDetails(authorisation);

        grantAccessToCase(caseworkerUser, caseId, respondentUser.getId());
    }

    private void grantAccessToCase(UserDetails anonymousCaseWorker, String caseId, String respondentId) {
        caseAccessApi.grantAccessToCase(
            anonymousCaseWorker.getAuthToken(),
            getServiceAuthToken(),
            anonymousCaseWorker.getId(),
            jurisdictionId,
            caseType,
            caseId,
            new UserId(respondentId)
        );
    }

    private boolean letterHolderIdMatchesAndNotLinked(CaseDetails caseDetails, String letterHolderId) {
        if (caseDetails == null || caseDetails.getData() == null || StringUtils.isBlank(letterHolderId)) {
            return false;
        }

        return !"YES".equalsIgnoreCase(String.valueOf(caseDetails.getData().get(RECEIVED_AOS_FIELD)))
            && letterHolderId.equals(caseDetails.getData().get(LETTER_HOLDER_CASE_FIELD));
    }
}
