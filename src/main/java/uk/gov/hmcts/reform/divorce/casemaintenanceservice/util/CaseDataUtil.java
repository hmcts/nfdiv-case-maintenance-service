package uk.gov.hmcts.reform.divorce.casemaintenanceservice.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CaseDataUtil {

    public Map<String, Object> copyAndRemoveKeys(final Map<String, Object> caseData, final String... keys) {

        final HashMap<String, Object> caseDataCopy = new HashMap<>(caseData);

        for (String key : keys) {
            caseDataCopy.remove(key);
        }

        return caseDataCopy;
    }
}
