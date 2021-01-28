package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.FormatterServiceClient;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.AmendCaseRemovedProps;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseState;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseStateGrouping;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CmsConstants;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.DivorceSessionProperties;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.event.ccd.submission.CaseSubmittedEvent;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.CcdRetrievalService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.PetitionService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.UserService;
import uk.gov.hmcts.reform.idam.client.models.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseRetrievalStateMap.RESPONDENT_CASE_STATE_GROUPING;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REFUSAL_ORDER_REJECTION_REASONS;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_INSUFFICIENT_DETAILS;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_NO_CRITERIA;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.REJECTION_NO_JURISDICTION;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.DivCaseRole.PETITIONER;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.DivCaseRole.RESPONDENT;

@Service
@Slf4j
public class PetitionServiceImpl implements PetitionService,
    ApplicationListener<CaseSubmittedEvent> {

    public static final String IS_DRAFT_KEY = "fetchedDraft";

    @Autowired
    private CcdRetrievalService ccdRetrievalService;

    @Autowired
    private FormatterServiceClient formatterServiceClient;

    @Autowired
    private UserService userService;

    @Override
    public CaseDetails retrievePetition(String authorisation, Map<CaseStateGrouping, List<CaseState>> caseStateGrouping) {
        return ccdRetrievalService.retrieveCase(authorisation, caseStateGrouping, PETITIONER);
    }

    @Override
    public CaseDetails retrievePetition(String authorisation) {
        return ccdRetrievalService.retrieveCase(authorisation, PETITIONER);
    }

    @Override
    public CaseDetails retrievePetitionForAos(String authorisation) {
        return ccdRetrievalService.retrieveCase(authorisation, RESPONDENT_CASE_STATE_GROUPING, RESPONDENT);
    }

    @Override
    public CaseDetails retrievePetitionByCaseId(String authorisation, String caseId) {
        return ccdRetrievalService.retrieveCaseById(authorisation, caseId);
    }

    @Override
    public void onApplicationEvent(@Nonnull CaseSubmittedEvent event) {
        // redo: delete in CCD
    }

    @Override
    public Map<String, Object> createAmendedPetitionDraft(String authorisation) {
        CaseDetails oldCase = retrieveAndValidatePetitionCase(authorisation);

        if (oldCase == null) {
            return null;
        }

        final Map<String, Object> amendmentCaseDraft = this.getDraftAmendmentCase(oldCase, authorisation);
        // redo: recreate in CCD? should return with null ID I think

        return amendmentCaseDraft;
    }

    @Override
    public Map<String, Object> createAmendedPetitionDraftRefusal(String authorisation) {
        CaseDetails oldCase = retrieveAndValidatePetitionCase(authorisation);

        if (oldCase == null) {
            return null;
        }

        final Map<String, Object> amendmentCaseDraft = this.getDraftAmendmentCaseRefusal(oldCase, authorisation);
        // redo: recreate in CCD? Should return with null ID I think

        return amendmentCaseDraft;
    }

    @Override
    public Map<String, Object> createAmendedPetitionDraftRefusalFromCaseId(String authorisation, String caseId) {
        CaseDetails oldCase = retrieveAndValidatePetitionCase(authorisation, caseId);

        if (oldCase == null) {
            return null;
        }

        return this.getDraftAmendmentCaseRefusal(oldCase, authorisation);
    }

    private CaseDetails retrieveAndValidatePetitionCase(String authorisation) {
        return retrieveAndValidatePetitionCase(authorisation, null);
    }

    private CaseDetails retrieveAndValidatePetitionCase(String authorisation, String caseId) {
        User userDetails = userService.retrieveUser(authorisation);
        if (userDetails == null) {
            log.warn("No user found for token");
            return null;
        }

        final CaseDetails oldCase;
        if (caseId != null) {
            User caseworkerUser = userService.retrieveAnonymousCaseWorkerDetails();
            oldCase = this.retrievePetitionByCaseId(caseworkerUser.getAuthToken(), caseId);
        } else {
            oldCase = this.retrievePetition(authorisation);
        }

        return caseAfterValidation(oldCase, userDetails);
    }

    private CaseDetails caseAfterValidation(CaseDetails caseDetails, User userDetails) {
        if (caseDetails == null) {
            log.warn("No case found for the user [{}]", userDetails.getUserDetails().getId());
            return null;
        } else if (!caseDetails.getData().containsKey(CcdCaseProperties.D8_CASE_REFERENCE)) {
            log.warn("Case [{}] has not progressed to have a Family Man reference found for the user [{}]",
                caseDetails.getId(), userDetails.getUserDetails().getId());
            return null;
        }
        return caseDetails;
    }

    private Map<String, Object> getDraftAmendmentCaseRefusal(CaseDetails oldCase, String authorisation) {
        return getDraftAmendmentCase(oldCase, authorisation, true);
    }

    private Map<String, Object> getDraftAmendmentCase(CaseDetails oldCase, String authorisation) {
        return getDraftAmendmentCase(oldCase, authorisation, false);
    }

    private Map<String, Object> getDraftAmendmentCase(CaseDetails oldCase, String authorisation, boolean refusal) {
        final List<?> previousReasonsForDivorce;
        if (refusal) {
            previousReasonsForDivorce = getPreviousReasonsForDivorce(oldCase, CcdCaseProperties.PREVIOUS_REASONS_DIVORCE_REFUSAL);
        } else {
            previousReasonsForDivorce = getPreviousReasonsForDivorce(oldCase, CcdCaseProperties.PREVIOUS_REASONS_DIVORCE);
        }

        Map<String, Object> caseData = oldCase.getData();
        Object issueDateFromOriginalCase = caseData.get(CcdCaseProperties.ISSUE_DATE);
        if (issueDateFromOriginalCase != null) {
            caseData.put(CcdCaseProperties.PREVIOUS_ISSUE_DATE, issueDateFromOriginalCase);
        }

        // remove all props from old case we do not want in new draft case
        if (refusal) {
            removePropertiesBasedOnListOfRejectionReasons(caseData);
        } else {
            Arrays.stream(AmendCaseRemovedProps.getPropertiesToRemove()).forEach(caseData::remove);
        }

        caseData.put(CcdCaseProperties.D8_DIVORCE_UNIT, CmsConstants.CTSC_SERVICE_CENTRE);

        Map<String, Object> amendmentCaseDraft = formatterServiceClient.transformToDivorceFormat(caseData, authorisation);

        if (refusal) {
            amendmentCaseDraft.put(DivorceSessionProperties.PREVIOUS_REASONS_FOR_DIVORCE_REFUSAL, previousReasonsForDivorce);
        } else {
            amendmentCaseDraft.put(DivorceSessionProperties.PREVIOUS_REASONS_FOR_DIVORCE, previousReasonsForDivorce);
        }
        amendmentCaseDraft.put(DivorceSessionProperties.PREVIOUS_CASE_ID, String.valueOf(oldCase.getId()));

        return amendmentCaseDraft;
    }

    private void removePropertiesBasedOnListOfRejectionReasons(Map<String, Object> caseData) {
        List<?> rejectionReasons = Optional.ofNullable(caseData.get(REFUSAL_ORDER_REJECTION_REASONS))
            .map(List.class::cast)
            .orElse(emptyList());

        List<String> propertiesToRemove = new ArrayList<>(Arrays.asList(AmendCaseRemovedProps.getPropertiesToRemoveForRejection()));

        if (rejectionReasons.contains(REJECTION_NO_JURISDICTION)) {
            propertiesToRemove.addAll(Arrays.asList(AmendCaseRemovedProps.getPropertiesToRemoveForRejectionJurisdiction()));
        }

        if (rejectionReasons.contains(REJECTION_NO_CRITERIA) || rejectionReasons.contains(REJECTION_INSUFFICIENT_DETAILS)) {
            propertiesToRemove.addAll(Arrays.asList(AmendCaseRemovedProps.getPropertiesToRemoveForRejectionAboutDivorce()));
        }

        propertiesToRemove.forEach(caseData::remove);
    }

    private List<?> getPreviousReasonsForDivorce(CaseDetails caseDetails, String keyForPreviousDivorceReasons) {
        Map<String, Object> caseData = new HashMap<>(caseDetails.getData());

        List<? super Object> previousReasons = Optional.ofNullable(caseData.get(keyForPreviousDivorceReasons))
            .map(List.class::cast)
            .orElse(new ArrayList<>());

        if (!previousReasons.isEmpty()) {
            log.info("Previous reasons for divorce already exist for Case id: {}", caseDetails.getId());
        }

        previousReasons.add(caseData.get(CcdCaseProperties.D8_REASON_FOR_DIVORCE));

        return previousReasons;
    }

}
