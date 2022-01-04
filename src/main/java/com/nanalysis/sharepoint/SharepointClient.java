/*
 * sharepoint-client: access files hosted on sharepoint from Java.
 * Copyright (C) 2022 - Nanalysis Scientific Corp.
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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
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


/**
 * A sharepoint client to manage authentication, files and folders.
 */
public class SharepointClient {
    private final HttpClient httpClient = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final String baseUrl;
    private final String siteUrl;

    private String token = "";

    public SharepointClient(String baseUrl, String site) {
        this.baseUrl = baseUrl;
        this.siteUrl = baseUrl + "/sites/" + site;
    }

    public void authenticate(String username, String password)
            throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException, SAXException {
        String token = getSecurityToken(username, password);
        signin(token);
        this.token = fetchBearerToken();
    }

    private String getSecurityToken(String username, String password) throws IOException, InterruptedException,
            ParserConfigurationException, SAXException, XPathExpressionException {
        String authentication = Templates.REQUEST_SECURITY_TOKEN
                .replace("${username}", username)
                .replace("${password}", password)
                .replace("${endpoint}", baseUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/extSTS.srf"))
                .POST(HttpRequest.BodyPublishers.ofString(authentication))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return extractXmlTag(response.body(), "BinarySecurityToken");
    }

    private void signin(String securityToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_forms/default.aspx?wa=wsignin1.0"))
                .POST(HttpRequest.BodyPublishers.ofString(securityToken))
                .build();

        // we don't care about the response here, only the side effect: cookies are stored in http client
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String fetchBearerToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_api/contextinfo"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(" "))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        return json.getString("FormDigestValue");
    }

    public List<String> listFolders(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(siteUrl + "/_api/web/GetFolderByServerRelativeUrl('" + encodePath(path) + "')/Folders"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        JSONArray results = json.getJSONArray("value");
        List<String> folders = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            folders.add(results.getJSONObject(i).getString("Name"));
        }
        return folders;
    }

    public List<String> listFiles(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(siteUrl + "/_api/web/GetFolderByServerRelativeUrl('" + encodePath(path) + "')/Files"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        JSONArray results = json.getJSONArray("value");
        List<String> folders = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            folders.add(results.getJSONObject(i).getString("Name"));
        }
        return folders;
    }


    public void createFolder(String parent, String folderName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/_api/web/folders", siteUrl)))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("{\"ServerRelativeUrl\": \"%s/%s\"}", parent, folderName)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    public void deleteFolder(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(siteUrl + "/_api/web/GetFolderByServerRelativeUrl('" + encodePath(path) + "')"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("If-Match", "*")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }


    public void uploadFile(String folder, String filename, File file) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/_api/web/GetFolderByServerRelativeUrl('%s')/Files/Add(url='%s',overwrite=true)",
                        siteUrl, encodePath(folder), encodePath(filename))))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    public void uploadFile(String folder, String filename, byte[] data) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/_api/web/GetFolderByServerRelativeUrl('%s')/Files/Add(url='%s',overwrite=true)",
                        siteUrl, encodePath(folder), encodePath(filename))))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    public void deleteFile(String folder, String filename) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(siteUrl + "/_api/web/GetFolderByServerRelativeUrl('" + encodePath(folder) + "')/Files('" + encodePath(filename) + "')"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("If-Match", "*")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkForError(response);
    }

    public InputStream download(String folder, String filename) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(siteUrl + "/_api/web/GetFolderByServerRelativeUrl('" + encodePath(folder) + "')/Files('" + encodePath(filename) + "')/$value"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return response.body();
    }

    private void checkForError(HttpResponse<String> response) throws IOException {
        String body = response.body();
        if (!body.isBlank()) {
            try {
                JSONObject json = new JSONObject(body);
                if (json.has("odata.error")) {
                    throw new IOException(json.getJSONObject("odata.error").getJSONObject("message").getString("value"));
                }
            } catch (JSONException e) {
                throw new IOException("Unknown error: " + body);
            }
        }
    }

    private String extractXmlTag(String body, String tagName)
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder xmlDocumentBuilder = factory.newDocumentBuilder();
        Element root = xmlDocumentBuilder.parse(new ByteArrayInputStream(body.getBytes())).getDocumentElement();

        // Unable to ask for "//wsse:BinarySecurityToken" without defining a namespace context, use local-name workaround instead
        return XPathFactory.newInstance().newXPath()
                .evaluate("//*[local-name()='" + tagName + "']/text()", root);
    }

    private String encodePath(String path) {
        // no percent URI encoding without a third party library, use form encoding instead and fix spaces
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
