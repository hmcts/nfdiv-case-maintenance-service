package uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CitizenCaseState {
    AWAITING_PAYMENT("AwaitingPayment"),
    AWAITING_HWF_DECISION("AwaitingHWFDecision"),
    SUBMITTED("Submitted"),
    ISSUED("Issued"),
    PENDING_REJECTION("PendingRejection"),
    AWAITING_DOCUMENTS("AwaitingDocuments"),
    AWAITING_DECREE_NISI("AwaitingDecreeNisi");

    private final String value;
}
