package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.InvalidRequestException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.CcdRetrievalService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.util.CaseDataUtil;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_AUTH_TOKEN;

@RunWith(MockitoJUnitRunner.class)
public class CaseControllerTest {

    @Mock
    private CaseDataUtil caseDataUtil;

    @Mock
    private CaseService caseService;

    @Mock
    private CcdRetrievalService ccdRetrievalService;

    @InjectMocks
    private CaseController caseController;

    @Test
    public void shouldSubmitDraftCase() {

        final Map<String, Object> caseData = new HashMap<>();
        final CaseDetails caseDetails = mock(CaseDetails.class);

        when(caseService.submitDraftCase(caseData, TEST_AUTH_TOKEN)).thenReturn(caseDetails);

        final ResponseEntity<CaseDetails> responseEntity = caseController.submitDraftCase(caseData, TEST_AUTH_TOKEN);

        assertThat(responseEntity.getStatusCodeValue(), is(OK.value()));
        assertThat(responseEntity.getBody(), is(caseDetails));

        verify(caseService).submitDraftCase(caseData, TEST_AUTH_TOKEN);
    }

    @Test
    public void givenCaseExists_whenRetrieveCase_thenReturnCaseDetails() {

        final CaseDetails caseDetails = CaseDetails.builder().build();

        when(ccdRetrievalService.retrieveCase(TEST_AUTH_TOKEN))
            .thenReturn(caseDetails);

        ResponseEntity<CaseDetails> actual = caseController.retrieveCase(TEST_AUTH_TOKEN);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(caseDetails, actual.getBody());

        verify(ccdRetrievalService).retrieveCase(TEST_AUTH_TOKEN);
    }

    @Test
    public void givenDuplicateCaseExists_whenRetrieveCase_thenReturnHttpStatus300() {
        when(ccdRetrievalService.retrieveCase(TEST_AUTH_TOKEN))
            .thenThrow(new DuplicateCaseException("Duplicate cases found"));

        ResponseEntity<CaseDetails> actual = caseController.retrieveCase(TEST_AUTH_TOKEN);

        assertEquals(HttpStatus.MULTIPLE_CHOICES, actual.getStatusCode());

        verify(ccdRetrievalService).retrieveCase(TEST_AUTH_TOKEN);
    }


    @Test
    public void givenNoCaseExists_whenRetrieveCase_thenReturn404() {

        when(ccdRetrievalService.retrieveCase(TEST_AUTH_TOKEN)).thenReturn(null);

        ResponseEntity<CaseDetails> actual = caseController.retrieveCase(TEST_AUTH_TOKEN);

        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
        assertNull(actual.getBody());

        verify(ccdRetrievalService).retrieveCase(TEST_AUTH_TOKEN);
    }

    @Test
    public void shouldPatchCaseAndReturnCaseDetails() {

        final String caseIdKey = "id";
        final Long caseId = 1234567L;
        final Map<String, Object> caseData = new HashMap<>();
        caseData.put(caseIdKey, caseId);
        caseData.put("field", "field value");

        final Map<String, Object> caseDataCopy = new HashMap<>();
        caseDataCopy.put("field", "field value");

        final CaseDetails caseDetails = mock(CaseDetails.class);

        when(caseDataUtil.copyAndRemoveKeys(caseData, caseIdKey)).thenReturn(caseDataCopy);
        when(caseService.patchCase(caseId.toString(), caseDataCopy, TEST_AUTH_TOKEN)).thenReturn(caseDetails);

        final ResponseEntity<CaseDetails> responseEntity = caseController.patchCase(caseData, TEST_AUTH_TOKEN);

        assertThat(responseEntity.getStatusCodeValue(), is(OK.value()));
        assertThat(responseEntity.getBody(), is(caseDetails));

        verify(caseDataUtil).copyAndRemoveKeys(caseData, caseIdKey);
        verify(caseService).patchCase(caseId.toString(), caseDataCopy, TEST_AUTH_TOKEN);
    }

    @Test
    public void shouldThrowInvalidRequestExceptionIfCaseIdMissingFromPayload() {

        final Map<String, Object> caseData = new HashMap<>();
        caseData.put("field", "field value");

        try {
            caseController.patchCase(caseData, TEST_AUTH_TOKEN);
            fail();
        } catch (final InvalidRequestException exception) {
            assertThat(exception.getMessage(), is("Missing 'id' in payload."));
            verifyNoInteractions(caseService);
        }
    }
}
