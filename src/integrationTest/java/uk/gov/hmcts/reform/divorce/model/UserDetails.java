package uk.gov.hmcts.reform.divorce.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDetails {
    private String id;
    private String username;
    private String emailAddress;
    private String password;
    private String authToken;
    private List<String> roles;
}
