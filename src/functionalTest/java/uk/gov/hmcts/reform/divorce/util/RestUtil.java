package uk.gov.hmcts.reform.divorce.util;

import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;

import java.util.Collections;
import java.util.Map;

public class RestUtil {
    public static Response postToRestService(final String url,
                                             final Map<String, Object> headers,
                                             final String requestBody) {
        return postToRestService(url, headers, requestBody, Collections.emptyMap());
    }

    public static Response postToRestService(final String url,
                                             final Map<String, Object> headers,
                                             final String requestBody,
                                             final Map<String, Object> params) {
        if (requestBody != null) {
            return SerenityRest.given()
                .headers(headers)
                .queryParams(params)
                .body(requestBody)
                .when()
                .post(url)
                .andReturn();
        } else {
            return SerenityRest.given()
                .headers(headers)
                .queryParams(params)
                .when()
                .post(url)
                .andReturn();
        }
    }

    public static Response putToRestService(final String url,
                                            final Map<String, Object> headers,
                                            final String requestBody,
                                            final Map<String, Object> params) {
        if (requestBody != null) {
            return SerenityRest.given()
                .headers(headers)
                .params(params)
                .body(requestBody)
                .when()
                .put(url)
                .andReturn();
        } else {
            return SerenityRest.given()
                .headers(headers)
                .when()
                .put(url)
                .andReturn();
        }
    }

    public static Response patchToRestService(final String url,
                                              final Map<String, Object> headers,
                                              final String requestBody,
                                              final Map<String, Object> params) {
        return SerenityRest.given()
            .headers(headers)
            .params(params)
            .body(requestBody)
            .when()
            .patch(url)
            .andReturn();
    }

    public static Response deleteOnRestService(final String url, final Map<String, Object> headers) {
        return SerenityRest.given()
            .headers(headers)
            .when()
            .delete(url)
            .andReturn();
    }

    public static Response getFromRestService(final String url, final Map<String, Object> headers) {
        return SerenityRest.given()
            .headers(headers)
            .when()
            .get(url)
            .andReturn();
    }
}
