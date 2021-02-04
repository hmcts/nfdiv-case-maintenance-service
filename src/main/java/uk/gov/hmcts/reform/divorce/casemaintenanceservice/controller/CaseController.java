package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(value = "Case Maintenance Services", consumes = "application/json", produces = "application/json")
public class CaseController {

    @Autowired
    private CaseService caseService;

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
}
