package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseState;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseStateGrouping;

import java.util.List;
import java.util.Map;

public interface PetitionService {
    CaseDetails retrievePetition(String authorisation, Map<CaseStateGrouping, List<CaseState>> caseStateGrouping);

    CaseDetails retrievePetition(String authorisation);

    CaseDetails retrievePetitionForAos(String authorisation);

    CaseDetails retrievePetitionByCaseId(String authorisation, String caseId);

    Map<String, Object> createAmendedPetitionDraft(String authorisation);

    Map<String, Object> createAmendedPetitionDraftRefusal(String authorisation);

    Map<String, Object> createAmendedPetitionDraftRefusalFromCaseId(String authorisation, String caseId);
}
