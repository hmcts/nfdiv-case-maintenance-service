package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.CcdRetrievalService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(value = "Case Maintenance Services", consumes = "application/json", produces = "application/json")
@Slf4j
public class CaseController {

    @Autowired
    private CaseService caseService;

    @Autowired
    private CcdRetrievalService ccdRetrievalService;

    @PostMapping(path = "/case", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Submits a draft divorce case to CCD")
    @ApiResponses(value = {
        @ApiResponse(
            code = 200,
            message = "Case Data was submitted to CCD. The body payload returns the complete case back",
            response = CaseDetails.class
        )
    })
    public ResponseEntity<CaseDetails> submitDraftCase(
        @RequestBody @ApiParam(value = "Case Data", required = true) final Map<String, Object> caseData,
        @RequestHeader(AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt) {
        return ResponseEntity.ok(caseService.submitDraftCase(caseData, jwt));
    }


    @GetMapping(path = "/case", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Retrieves a divorce case from CCD")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A Case exists. The case is in the response body"),
        @ApiResponse(code = 404, message = "When no case exists"),
        @ApiResponse(code = 300, message = "Multiple Cases found")
    })
    public ResponseEntity<CaseDetails> retrieveCase(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt) {
        try {
            CaseDetails caseDetails = ccdRetrievalService.retrieveCase(jwt);
            return caseDetails == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(caseDetails);
        } catch (DuplicateCaseException e) {
            log.warn(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES).build();
        }
    }
}
