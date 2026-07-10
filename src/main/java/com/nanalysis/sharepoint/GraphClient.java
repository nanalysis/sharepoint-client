/*
 * sharepoint-client: access files hosted on sharepoint from Java.
 * Copyright (C) 2026 - Nanalysis Scientific Corp.
 * -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nanalysis.sharepoint;


import com.nanalysis.sharepoint.auth.OAuth2Authenticator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * A graph client to manage authentication, files and folders. Use Microsoft Graph API.
 */
public class GraphClient implements Client {
    private static final String GRAPH_ERROR = "error";
    private static final String GRAPH_ERROR_CODE = "code";
    private static final String GRAPH_ERROR_MESSAGE = "message";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final String host;
    private final String site;
    private final String baseUrl;

    private String token = "";
    private String siteId = null;

    public GraphClient(String baseUrl, String site) {
        this.baseUrl = baseUrl;
        this.host = hostFromUrl();
        this.site = site;
    }

    private String hostFromUrl() {
        return URI.create(baseUrl).getHost();
    }

    @Override
    public void authenticateWithUserCredentials(String username, String password) {
        throw new UnsupportedOperationException("User authentication not supported yet for Microsoft Graph API.");
    }

    @Override
    public void authenticateWithOAuth2(String clientId, String clientSecret) throws IOException, InterruptedException {
        this.token = new OAuth2Authenticator(httpClient, baseUrl, site, API.GRAPH).authenticate(clientId, clientSecret);
    }

    private String getSiteId() throws IOException, InterruptedException {
        if (siteId == null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + host + ":/sites/" + site))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkForError(response);
            JSONObject json = new JSONObject(response.body());
            siteId = json.getString("id");
        }
        return siteId;
    }

    /**
     * @param siteId Site ID to consider.
     * @param path   Path to consider.
     * @return ID of file of folder.
     * @throws IOException          Communication issues.
     * @throws InterruptedException Interrupted while communication was ongoing.
     */
    private String getResourceId(String siteId, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/root:/" + encodePath(path)))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
        JSONObject json = new JSONObject(response.body());
        return json.getString("id");
    }

    private JSONArray listChildren(String path) throws IOException, InterruptedException {
        String siteId = getSiteId();

        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/root:/"
                        + encodePath(path) + ":/children"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
        checkForError(response);

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("value");
    }

    @Override
    public List<String> listFolders(String path) throws IOException, InterruptedException {
        JSONArray results = listChildren(path);
        List<String> folders = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            if (results.getJSONObject(i).has("folder")) {
                folders.add(results.getJSONObject(i).getString("name"));
            }
        }
        return folders;
    }

    @Override
    public List<String> listFiles(String path) throws IOException, InterruptedException {
        JSONArray results = listChildren(path);
        List<String> files = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            if (results.getJSONObject(i).has("file")) {
                files.add(results.getJSONObject(i).getString("name"));
            }
        }
        return files;
    }

    @Override
    public void createFolder(String parent, String folderName) throws IOException, InterruptedException {
        String siteId = getSiteId();

        String createFolderJson = """
                {
                  "name": "%s",
                  "folder": {},
                  "@microsoft.graph.conflictBehavior": "fail"
                }
                """.formatted(folderName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/root:/" + encodePath(parent) + ":/children"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createFolderJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    private void deleteResource(String path) throws IOException, InterruptedException {
        String siteId = getSiteId();
        String folderId = getResourceId(siteId, path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/items/" + folderId))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    @Override
    public void deleteFolder(String path) throws IOException, InterruptedException {
        deleteResource(path);
    }

    @Override
    public void deleteFile(String folder, String filename) throws IOException, InterruptedException {
        deleteResource(folder + "/" + filename);
    }

    @Override
    public void uploadFile(String folder, String filename, File file) throws IOException, InterruptedException {
        String siteId = getSiteId();
        String folderId = getResourceId(siteId, folder);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/items/" + folderId + ":/" + encodePath(filename) + ":/content"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    @Override
    public void uploadFile(String folder, String filename, byte[] data) throws IOException, InterruptedException {
        String siteId = getSiteId();
        String folderId = getResourceId(siteId, folder);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/items/" + folderId + ":/" + encodePath(filename) + ":/content"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    private String createUploadSession(String folder, String filename)
            throws IOException, InterruptedException {
        String siteId = getSiteId();
        String folderId = getResourceId(siteId, folder);
        String body = """
                {
                    "item": {
                        "@microsoft.graph.conflictBehavior": "replace"
                    }
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/items/" + folderId + ":/"
                        + encodePath(filename) + ":/createUploadSession"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);

        JSONObject json = new JSONObject(response.body());
        return json.getString("uploadUrl");
    }

    private int readChunk(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int count = in.read(
                    buffer,
                    total,
                    buffer.length - total);

            if (count < 0) {
                break;
            }
            total += count;
        }
        return total;
    }

    @Override
    public void uploadBigFile(String folder, String filename, long size, InputStream input, Consumer<Double> progressCallback)
            throws IOException, InterruptedException {
        // create an upload session, pre-authenticated.
        String uploadUrl = createUploadSession(folder, filename);

        int chunkSize = 10 * 1024 * 1024;
        byte[] buffer = new byte[chunkSize];
        long offset = 0;

        int nread;
        while ((nread = readChunk(input, buffer)) > 0) {
            long end = offset + nread - 1;
            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Range", String.format("bytes %d-%d/%d", offset, end, size))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(buffer, 0, nread))
                    .build();

            HttpResponse<String> response = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
            checkForError(response);

            offset += nread;
            progressCallback.accept(100. * offset / size);
        }
    }

    @Override
    public InputStream download(String folder, String filename) throws IOException, InterruptedException {
        String siteId = getSiteId();
        String fileId = getResourceId(siteId, folder + "/" + filename);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/sites/" + siteId + "/drive/items/" + fileId + "/content"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Unable to download file: HTTP error " + response.statusCode());
        }
        return response.body();
    }

    private void checkForError(HttpResponse<String> response) throws IOException {
        int status = response.statusCode();
        // Success codes.
        if (status >= 200 && status < 300) {
            return;
        }

        String body = response.body();
        try {
            JSONObject root = new JSONObject(body);
            if (root.has(GRAPH_ERROR)) {
                JSONObject error = root.getJSONObject(GRAPH_ERROR);
                String code = error.optString(GRAPH_ERROR_CODE, "Unknown");
                String message = error.optString(GRAPH_ERROR_MESSAGE, "No error message provided");
                String sb = "Microsoft Graph request failed" + " (HTTP " + status + ")" + ": " + code + " - " + message;
                throw new IOException(sb);
            }
            throw new IOException("Microsoft Graph request failed (HTTP " + status + "): " + body);
        } catch (org.json.JSONException e) {
            throw new IOException("Microsoft Graph request failed (HTTP " + status + ") with an invalid JSON response: " + body, e);
        }
    }

    private String encodePath(String path) {
        // no percent URI encoding without a third party library, use form encoding instead and fix spaces
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
