package com.nanalysis.sharepoint.auth;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OAuth2Authenticator {
    private static final String OAUTH_URL_TEMPLATE = "https://accounts.accesscontrol.windows.net/${tenantId}/tokens/OAuth/2";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String clientUrl;

    public OAuth2Authenticator(HttpClient httpClient, String baseUrl, String siteUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.clientUrl = siteUrl + "/_vti_bin/client.svc/";
    }

    public String authenticate(String clientId, String clientSecret)
            throws IOException, InterruptedException {
        String host = new URL(baseUrl).getHost();

        String header = getAuthenticationHeader();
        String tenantId = extractAuthHeaderAttribute(header, "realm");
        String resourceId = extractAuthHeaderAttribute(header, "client_id");

        String clientAtTenant = clientId + "@" + tenantId;
        String resource = resourceId + "/" + host+ "@" + tenantId;
        String body = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(clientAtTenant, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);

        String oauthUrl = OAUTH_URL_TEMPLATE.replace("${tenantId}", tenantId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(oauthUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        return json.getString("access_token");
    }

    private String getAuthenticationHeader() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(clientUrl))
                .header("Authorization", "Bearer")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.headers().firstValue("WWW-Authenticate")
                .orElseThrow(() -> new IOException("Unable to authenticate, no WWW-Authenticate header in response!"));
    }

    private String extractAuthHeaderAttribute(String authHeader, String attribute) throws IOException {
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
