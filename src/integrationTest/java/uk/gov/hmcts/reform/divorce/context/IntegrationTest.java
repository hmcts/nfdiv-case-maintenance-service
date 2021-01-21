package uk.gov.hmcts.reform.divorce.context;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.junit.spring.integration.SpringIntegrationMethodRule;
import org.assertj.core.util.Strings;
import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.IdamTestSupport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

@Slf4j
@RunWith(SerenityRunner.class)
@ContextConfiguration(classes = {ServiceContextConfiguration.class})
public abstract class IntegrationTest {

    @Value("${case.maintenance.service.base.uri}")
    protected String serverUrl;

    @Autowired
    protected IdamTestSupport idamTestSupport;

    @Rule
    public SpringIntegrationMethodRule springMethodIntegration;

    private final List<UserDetails> createdUsers = new ArrayList<>();

    protected IntegrationTest() {
        this.springMethodIntegration = new SpringIntegrationMethodRule();
    }

    @After
    public void onDestroy() {
        log.info("Destroying created users");
        for (UserDetails details : createdUsers) {
            try {
                idamTestSupport.deleteUser(details.getEmailAddress());
            } catch (Exception e) {
                log.error("User deletion failed " + details.getEmailAddress(), e);
            }
        }
    }

    protected UserDetails getUserDetails() {
        UserDetails details = idamTestSupport.createAnonymousCitizenUser();
        createdUsers.add(details);

        return details;
    }

    protected UserDetails getSolicitorUser() {
        return idamTestSupport.getSolicitorUser();
    }

    protected String getUserToken() {
        return getUserDetails().getAuthToken();
    }

    protected UserDetails getCaseWorkerUser() {
        return idamTestSupport.getCaseworkerUser();
    }

    protected String getCaseWorkerToken() {
        return getCaseWorkerUser().getAuthToken();
    }
}
