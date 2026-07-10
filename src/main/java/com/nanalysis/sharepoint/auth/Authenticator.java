package com.nanalysis.sharepoint.auth;

import com.nanalysis.sharepoint.API;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Getter
@AllArgsConstructor
public abstract class Authenticator {

    /**
     * Base host URL.
     */
    private final String baseUrl;

    /**
     * API to use.
     */
    private final API api;

    /**
     * Authenticate from given id and secret.
     *
     * @param id     Identifier to use.
     * @param secret Secret to use.
     * @return Token.
     */
    public abstract String authenticate(String id, String secret) throws IOException, TimeoutException, ExecutionException, InterruptedException;

    /**
     * @return Scope to use, depending on API choice.
     */
    public String getScope() {
        return switch (api) {
            case GRAPH -> "https://graph.microsoft.com/.default";
            case SHAREPOINT -> baseUrl + "/.default";
        };
    }

    /**
     * Utility method to find tenant id.
     *
     * @param httpClient HTTP client to use.
     * @param clientUrl  Client URL to target to find Tenant ID.
     * @return Tenant ID.
     * @throws IOException          Communication issues.
     * @throws InterruptedException Interrupted while communication was ongoing.
     */
    public static String guessTenantId(HttpClient httpClient, String clientUrl) throws IOException, InterruptedException {
        String header = getAuthenticationHeader(httpClient, clientUrl);
        return extractAuthHeaderAttribute(header, "realm");
    }

    private static String getAuthenticationHeader(HttpClient httpClient, String clientUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(clientUrl))
                .header("Authorization", "Bearer")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.headers().firstValue("WWW-Authenticate")
                .orElseThrow(() -> new IOException("Unable to authenticate, no WWW-Authenticate header in response!"));
    }

    private static String extractAuthHeaderAttribute(String authHeader, String attribute) throws IOException {
        String realmMarker = attribute + "=\"";
        int start = authHeader.indexOf(realmMarker);
        if (start < 0) {
            throw new IOException("Unable to extract " + attribute + ": " + authHeader);
        }
        start += realmMarker.length();
        int end = authHeader.indexOf('"', start);
        if (end < 0) {
            throw new IOException("Unable to extract " + attribute + ": " + authHeader);
        }

        return authHeader.substring(start, end);
    }
}
