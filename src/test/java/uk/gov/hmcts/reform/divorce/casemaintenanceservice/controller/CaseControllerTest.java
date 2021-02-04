package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.TestConstants.TEST_AUTH_TOKEN;

@RunWith(MockitoJUnitRunner.class)
public class CaseControllerTest {

    @Mock
    private CaseService caseService;

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
}