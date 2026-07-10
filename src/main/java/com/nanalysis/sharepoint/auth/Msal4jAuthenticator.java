package com.nanalysis.sharepoint.auth;

import com.microsoft.aad.msal4j.*;
import com.nanalysis.sharepoint.API;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Msal4jAuthenticator extends Authenticator {
    /**
     * Timeout for token request in min.
     */
    private static final int TIMEOUT = 1;

    /**
     * Authority URL to access the token.
     */
    private final String authority;

    public Msal4jAuthenticator(String baseUrl, String tenantId, API api) {
        super(baseUrl, api);
        this.authority = "https://login.microsoftonline.com/" + tenantId;
    }

    @Override
    public String authenticate(String clientId, String clientSecret) throws IOException, TimeoutException, ExecutionException, InterruptedException {
        IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder(clientId, credential)
                .authority(authority)
                .build();

        // Scope sharepoint: baseUrl + "/.default"
        // Scope microsoft graph: "https://graph.microsoft.com/.default"
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton(getScope())).build();

        CompletableFuture<IAuthenticationResult> acquireToken = app.acquireToken(parameters);
        IAuthenticationResult result = acquireToken.get(TIMEOUT, TimeUnit.MINUTES);
        return result.accessToken();
    }
}
