package uk.gov.hmcts.reform.divorce.petition;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.divorce.support.PetitionSupport;

import static org.junit.Assert.assertEquals;

public class CcdRetrieveCaseByIdTest extends PetitionSupport {

    @Test
    public void givenOneSubmittedCaseInCcd_whenRetrieveCase_thenReturnTheCase() throws Exception {
        final String userToken = getUserToken();

        Long caseId = getCaseIdFromSubmittingANewCase(userToken);

        Response cmsResponse = retrieveCaseById(userToken, String.valueOf(caseId));

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals(caseId, cmsResponse.path("id"));
    }

    @Test
    public void givenOneSubmittedCaseInCcd_whenRetrieveCaseByCaseWorker_thenReturnTheCase() throws Exception {
        final String userToken = getUserToken();
        final String caseWorkerToken = getPureCaseWorkerToken();

        Long caseId = getCaseIdFromSubmittingANewCase(userToken);

        Response cmsResponse = retrieveCaseById(caseWorkerToken, String.valueOf(caseId));

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals(caseId, cmsResponse.path("id"));
    }
}