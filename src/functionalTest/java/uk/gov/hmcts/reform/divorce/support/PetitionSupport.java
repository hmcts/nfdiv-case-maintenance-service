package uk.gov.hmcts.reform.divorce.support;

import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.divorce.util.RestUtil;

import java.util.Collections;

public abstract class PetitionSupport extends CcdUpdateSupport {
    protected static final String CCD_FORMAT_DRAFT_CONTEXT_PATH = "ccd-format-draft/";
    protected static final String DIVORCE_FORMAT_DRAFT_CONTEXT_PATH = "divorce-format-draft/";
    protected static final String DIVORCE_FORMAT_KEY = "divorceFormat";

    @Value("${case.maintenance.petition.context-path}")
    private String petitionContextPath;

    @Value("${case.maintenance.get-case.context-path}")
    private String getCaseContextPath;

    @Value("${case.maintenance.amend-petition-draft.context-path}")
    private String amendPetitionContextPath;

    @Value("${case.maintenance.amend-petition-draft-refusal.context-path}")
    private String amendPetitionRefusalContextPath;

    private String searchContextPath = "/casemaintenance/version/1/search";

    protected Response retrieveCase(String userToken) {
        return
            RestUtil.getFromRestService(
                getRetrieveCaseRequestUrl(),
                submissionUtils.createHeadersWith(userToken)
            );
    }

    protected Response retrieveCaseById(String userToken, String caseId) {
        return
            RestUtil.getFromRestService(
                getCaseRequestUrl() + "/" + caseId,
                submissionUtils.createHeadersWith(userToken)
            );
    }

    protected Response searchCases(String userToken, String query) {
        return
            RestUtil.postToRestService(
                getSearchRequestUrl(),
                submissionUtils.createHeadersWith(userToken),
                query
            );
    }

    protected Response getCase(String userToken) {
        return
            RestUtil.getFromRestService(
                getCaseRequestUrl(),
                submissionUtils.createHeadersWith(userToken)
            );
    }

    protected Response putAmendedPetitionDraft(String userToken) {
        return
            RestUtil.putToRestService(
                getGetAmendPetitionContextPath(),
                submissionUtils.createHeadersWith(userToken),
                "",
                Collections.emptyMap()
            );
    }

    protected Response putAmendedPetitionDraftForRefusal(String userToken) {
        return
            RestUtil.putToRestService(
                getGetAmendPetitionRefusalContextPath(),
                submissionUtils.createHeadersWith(userToken),
                "",
                Collections.emptyMap()
            );
    }

    protected Response putAmendedPetitionDraftForRefusalFromCaseId(String userToken, Long userId) {
        return
            RestUtil.putToRestService(
                getGetAmendPetitionRefusalContextPathFromCaseId(userId),
                submissionUtils.createHeadersWith(userToken),
                "",
                Collections.emptyMap()
            );
    }

    private String getGetAmendPetitionContextPath() {
        return serverUrl + amendPetitionContextPath;
    }

    private String getGetAmendPetitionRefusalContextPath() {
        return serverUrl + amendPetitionRefusalContextPath;
    }

    private String getGetAmendPetitionRefusalContextPathFromCaseId(Long caseId) {
        return serverUrl + amendPetitionRefusalContextPath + "/" + caseId;
    }

    protected String getRetrieveCaseRequestUrl() {
        return serverUrl + petitionContextPath;
    }

    private String getCaseRequestUrl() {
        return serverUrl + getCaseContextPath;
    }

    private String getSearchRequestUrl() {
        return serverUrl + searchContextPath;
    }
}
