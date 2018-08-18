package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.draftstore.model.DraftList;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.PetitionService;
import util.ReflectionTestUtil;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseRetrievalStateMap.PETITIONER_CASE_STATE_GROUPING;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseRetrievalStateMap.RESPONDENT_CASE_STATE_GROUPING;

@RunWith(MockitoJUnitRunner.class)
public class PetitionControllerUTest {
    private static final String AUTHORISATION = "user";

    @Mock
    private PetitionService petitionService;

    @InjectMocks
    private PetitionController classUnderTest;

    @Test
    public void givenCaseFound_whenRetrievePetition_thenReturnCaseDetails() throws DuplicateCaseException {

        final boolean checkCcd = true;

        final CaseDetails caseDetails = CaseDetails.builder().build();

        when(petitionService.retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd))
            .thenReturn(caseDetails);

        ResponseEntity<CaseDetails> actual = classUnderTest.retrievePetition(AUTHORISATION, checkCcd);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(caseDetails, actual.getBody());

        verify(petitionService).retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd);
    }

    @Test
    public void givenCaseFound_whenRetrieveCaseForRespondent_thenReturnCaseDetails() throws DuplicateCaseException {

        final boolean checkCcd = true;

        final CaseDetails caseDetails = CaseDetails.builder().build();

        when(petitionService.retrievePetition(AUTHORISATION, RESPONDENT_CASE_STATE_GROUPING, checkCcd))
            .thenReturn(caseDetails);

        ResponseEntity<CaseDetails> actual = classUnderTest.retrieveCaseForRespondent(AUTHORISATION, checkCcd);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(caseDetails, actual.getBody());

        verify(petitionService).retrievePetition(AUTHORISATION, RESPONDENT_CASE_STATE_GROUPING, checkCcd);
    }

    @Test
    public void givenCaseFound_whenRetrieveCase_thenReturnCaseDetails() throws DuplicateCaseException {

        final boolean checkCcd = true;

        final CaseDetails caseDetails = CaseDetails.builder().build();

        when(petitionService.retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd))
            .thenReturn(caseDetails);

        ResponseEntity<CaseDetails> actual = retrieveCase(checkCcd);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(caseDetails, actual.getBody());

        verify(petitionService).retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd);
    }

    @Test
    public void givenCheckCcdIsNullAndCaseFound_whenRetrieveCase_thenReturnCaseDetails() throws DuplicateCaseException {

        final Boolean checkCcd = null;

        final CaseDetails caseDetails = CaseDetails.builder().build();

        when(petitionService.retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, false))
            .thenReturn(caseDetails);

        ResponseEntity<CaseDetails> actual = retrieveCase(checkCcd);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(caseDetails, actual.getBody());

        verify(petitionService).retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING,false);
    }

    @Test
    public void givenNoCaseFound_whenRetrieveCase_thenReturn204() throws DuplicateCaseException {

        final boolean checkCcd = true;

        when(petitionService.retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd))
            .thenReturn(null);

        ResponseEntity<CaseDetails> actual = retrieveCase(checkCcd);

        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertNull(actual.getBody());

        verify(petitionService).retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd);
    }

    @Test
    public void givenDuplicateCase_whenRetrieveCase_thenReturnHttpStatus300() throws DuplicateCaseException {
        final boolean checkCcd = true;

        when(petitionService.retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd))
            .thenThrow(new DuplicateCaseException("Duplicate"));

        ResponseEntity<CaseDetails> actual = retrieveCase(checkCcd);

        assertEquals(HttpStatus.MULTIPLE_CHOICES, actual.getStatusCode());

        verify(petitionService).retrievePetition(AUTHORISATION, PETITIONER_CASE_STATE_GROUPING, checkCcd);
    }

    @Test
    public void givenDivorceFormatIsNull_whenSaveDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.saveDraft(AUTHORISATION, data, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).saveDraft(AUTHORISATION, data, false);
    }

    @Test
    public void givenDivorceFormatIsFalse_whenSaveDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.saveDraft(AUTHORISATION, data, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).saveDraft(AUTHORISATION, data, false);
    }

    @Test
    public void givenDivorceFormatIsTrue_whenSaveDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.saveDraft(AUTHORISATION, data, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).saveDraft(AUTHORISATION, data, true);
    }

    @Test
    public void givenDivorceFormatIsNull_whenCreateDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.createDraft(AUTHORISATION, data, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).createDraft(AUTHORISATION, data, false);
    }

    @Test
    public void givenDivorceFormatIsFalse_whenCreateDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.createDraft(AUTHORISATION, data, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).createDraft(AUTHORISATION, data, false);
    }

    @Test
    public void givenDivorceFormatIsTrue_whenCreateDraft_thenProceedAsExpected() {
        final Map<String, Object> data = Collections.emptyMap();

        ResponseEntity<Void> response = classUnderTest.createDraft(AUTHORISATION, data, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(petitionService).createDraft(AUTHORISATION, data, true);
    }

    @Test
    public void whenDeleteDraft_thenProceedAsExpected() {
        ResponseEntity<Void> response = classUnderTest.deleteDraft(AUTHORISATION);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(petitionService).deleteDraft(AUTHORISATION);
    }

    @Test
    public void whenRetrieveAllDrafts_thenProceedAsExpected() {
        final DraftList draftList = mock(DraftList.class);

        when(petitionService.getAllDrafts(AUTHORISATION)).thenReturn(draftList);

        ResponseEntity<DraftList> response = classUnderTest.retrieveAllDrafts(AUTHORISATION);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(draftList, response.getBody());

        verify(petitionService).getAllDrafts(AUTHORISATION);
    }

    private ResponseEntity<CaseDetails> retrieveCase(Boolean checkCcd) {
        try {
            return ReflectionTestUtil.invokeMethod(classUnderTest, "retrieveCase", AUTHORISATION,
                PETITIONER_CASE_STATE_GROUPING, checkCcd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
