package uk.gov.hmcts.reform.divorce.petition;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.PetitionSupport;

import static org.junit.Assert.assertEquals;

public class RetrieveCcdCaseTest extends PetitionSupport {

    @Test
    public void givenNoCaseExistsInCcd_whenGetCase_thenReturn404() {
        Response cmsResponse = getCaseFromCcd(getUserToken());

        assertEquals(HttpStatus.NOT_FOUND.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenSingleCaseExistsInCcd_whenGetCase_thenReturnTheCase() throws Exception {
        final UserDetails userDetails = getUserDetails();

        Long caseId = getCaseIdFromSubmittingANewCase(userDetails);

        Response cmsResponse = getCaseFromCcd(userDetails.getAuthToken());

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals(caseId, cmsResponse.path("id"));
    }

    @Test
    public void givenMultipleSubmittedCaseInCcd_whenGetCase_thenReturn300() throws Exception {
        final UserDetails userDetails = getUserDetails();

        getCaseIdFromSubmittingANewCase(userDetails);
        getCaseIdFromSubmittingANewCase(userDetails);

        Response cmsResponse = getCaseFromCcd(userDetails.getAuthToken());

        assertEquals(HttpStatus.MULTIPLE_CHOICES.value(), cmsResponse.getStatusCode());
    }
}
