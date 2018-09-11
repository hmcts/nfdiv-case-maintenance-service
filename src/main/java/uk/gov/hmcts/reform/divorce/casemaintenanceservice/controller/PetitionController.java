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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseRetrievalStateMap;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseState;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CaseStateGrouping;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.draftstore.model.DraftList;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.service.PetitionService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "casemaintenance/version/1")
@Api(value = "Case Maintenance Services", consumes = "application/json", produces = "application/json")
@Slf4j
public class PetitionController {

    @Autowired
    private PetitionService petitionService;

    @GetMapping(path = "/retrieveCase", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves a divorce case from CCD of Draft store")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A Petition exists. The petition is in the response body"),
        @ApiResponse(code = 204, message = "When there are no petition exists"),
        @ApiResponse(code = 300, message = "Multiple Petition found")
        })
    public ResponseEntity<CaseDetails> retrievePetition(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt,
        @RequestParam(value = "checkCcd", required = false)
        @ApiParam(value = "Boolean flag enabling CCD check for petition") final Boolean checkCcd) {

        return retrieveCase(jwt, CaseRetrievalStateMap.PETITIONER_CASE_STATE_GROUPING, checkCcd);
    }

    @GetMapping(path = "/retrieveAosCase", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves a divorce case from CCD of Draft store")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A Petition exists. The petition is in the response body"),
        @ApiResponse(code = 204, message = "When there are no petition exists"),
        @ApiResponse(code = 300, message = "Multiple Petition found")
        })
    public ResponseEntity<CaseDetails> retrieveCaseForRespondent(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt,
        @RequestParam(value = "checkCcd", required = false)
        @ApiParam(value = "Boolean flag enabling CCD check for petition") final Boolean checkCcd) {

        return retrieveCase(jwt, CaseRetrievalStateMap.RESPONDENT_CASE_STATE_GROUPING, checkCcd);
    }

    private ResponseEntity<CaseDetails> retrieveCase(String jwt,
                                                     Map<CaseStateGrouping, List<CaseState>> caseStateGrouping,
                                                     Boolean checkCcd) {

        try {
            CaseDetails caseDetails = petitionService.retrievePetition(jwt, caseStateGrouping,
                Optional.ofNullable(checkCcd).orElse(false));

            return caseDetails == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(caseDetails);
        } catch (DuplicateCaseException e) {
            log.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES).build();
        }
    }

    @PutMapping(path = "/drafts", consumes = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Saves or updates a draft to draft store")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Draft saved")})
    public ResponseEntity<Void> saveDraft(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt,
        @RequestBody
        @ApiParam(value = "The case draft", required = true)
        @NotNull final Map<String, Object> data,
        @RequestParam(value = "divorceFormat", required = false)
        @ApiParam(value = "Boolean flag indicting the data is in divorce format") final Boolean divorceFormat) {
        log.debug("Received request to save a draft");
        petitionService.saveDraft(jwt, data, Optional.ofNullable(divorceFormat).orElse(false));
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/drafts", consumes = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new draft in draft store")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Draft saved")})
    public ResponseEntity<Void> createDraft(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        @ApiParam(value = "JWT authorisation token issued by IDAM", required = true) final String jwt,
        @RequestBody
        @ApiParam(value = "The  case draft", required = true)
        @NotNull final Map<String, Object> data,
        @RequestParam(value = "divorceFormat", required = false)
        @ApiParam(value = "Boolean flag indicting the data is in divorce format") final Boolean divorceFormat) {
        log.debug("Received request to create a draft");
        petitionService.createDraft(jwt, data, Optional.ofNullable(divorceFormat).orElse(false));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/drafts")
    @ApiOperation(value = "Deletes a divorce case draft")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "The divorce draft has been deleted successfully")})
    public ResponseEntity<Void> deleteDraft(@RequestHeader("Authorization")
                                            @ApiParam(value = "JWT authorisation token issued by IDAM",
                                                required = true) final String jwt) {
        log.debug("Received request to delete a divorce session draft");
        petitionService.deleteDraft(jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/drafts", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve All the Drafts for a given user")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns all the saved drafts for a given user")})
    public ResponseEntity<DraftList> retrieveAllDrafts(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                        @ApiParam(value = "JWT authorisation token issued by IDAM",
                                                            required = true)final String jwt) {
        return ResponseEntity.ok(petitionService.getAllDrafts(jwt));
    }
}