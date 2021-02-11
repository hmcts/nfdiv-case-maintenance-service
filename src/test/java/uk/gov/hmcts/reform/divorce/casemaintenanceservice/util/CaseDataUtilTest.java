package uk.gov.hmcts.reform.divorce.casemaintenanceservice.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class CaseDataUtilTest {

    private final CaseDataUtil caseDataUtil = new CaseDataUtil();

    @Test
    public void shouldReturnCopyOfMapWithIdKeyRemoved() {

        final Map<String, Object> copyWithCaseIdRemoved = caseDataUtil.copyAndRemoveKeys(createCaseData(), "id");

        assertThat(copyWithCaseIdRemoved.size(), is(2));
        assertThat(copyWithCaseIdRemoved, hasEntry("field1", "value1"));
        assertThat(copyWithCaseIdRemoved, hasEntry("field2", "value2"));
    }

    @Test
    public void shouldReturnCopyOfMapWithMultipleKeysRemoved() {

        final Map<String, Object> copyWithCaseIdRemoved = caseDataUtil.copyAndRemoveKeys(createCaseData(), "field1", "field2");

        assertThat(copyWithCaseIdRemoved.size(), is(1));
        assertThat(copyWithCaseIdRemoved, hasEntry("id", 123456789L));
    }

    @Test
    public void shouldReturnCopyOfMapValuesWithNoKeysRemoved() {

        final Map<String, Object> copyWithCaseIdRemoved = caseDataUtil.copyAndRemoveKeys(createCaseData());

        assertThat(copyWithCaseIdRemoved.size(), is(3));
        assertThat(copyWithCaseIdRemoved, hasEntry("id", 123456789L));
        assertThat(copyWithCaseIdRemoved, hasEntry("field1", "value1"));
        assertThat(copyWithCaseIdRemoved, hasEntry("field2", "value2"));
    }

    private Map<String, Object> createCaseData() {
        final Map<String, Object> caseData = new HashMap<>();
        caseData.put("id", 123456789L);
        caseData.put("field1", "value1");
        caseData.put("field2", "value2");
        return caseData;
    }
}
