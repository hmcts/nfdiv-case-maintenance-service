package uk.gov.hmcts.reform.divorce.casemaintenanceservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.InvalidRequestException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.CcdRetrievalService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.impl.CaseService;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.util.CaseDataUtil;

import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(value = "Case Maintenance Services", consumes = "application/json", produces = "application/json")
@Slf4j
public class CaseController {

    @Autowired
    private CaseDataUtil caseDataUtil;

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

    @PatchMapping(path = "/case", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Patch a divorce case in CCD")
    @ApiResponses(value = {
        @ApiResponse(
            code = 200,
            message = "A request to patch the case details is sent to CCD. The body payload "
                + "will return the latest version of the case after the patch.",
            response = CaseDetails.class
        ),
        @ApiResponse(code = 400, message = "Returned when id is missing from json payload"),
    })
    public ResponseEntity<CaseDetails> patchCase(
        @RequestBody @ApiParam(value = "Case Data", required = true) final Map<String, Object> caseData,
        @RequestHeader(AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt) {

        return Optional.ofNullable(caseData.get("id"))
            .map(caseId -> ResponseEntity.ok(
                caseService.patchCase(
                    caseId.toString(),
                    caseDataUtil.copyAndRemoveKeys(caseData, "id"),
                    jwt)))
            .orElseThrow(() -> new InvalidRequestException("Missing 'id' in payload."));
    }
}
