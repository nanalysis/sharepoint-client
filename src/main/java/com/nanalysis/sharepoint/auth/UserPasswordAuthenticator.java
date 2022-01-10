package com.nanalysis.sharepoint.auth;

import org.json.JSONObject;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UserPasswordAuthenticator {
    private static final String REQUEST_SECURITY_TOKEN_TEMPLATE =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                    "      xmlns:a=\"http://www.w3.org/2005/08/addressing\"\n" +
                    "      xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                    "  <s:Header>\n" +
                    "    <a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue</a:Action>\n" +
                    "    <a:ReplyTo>\n" +
                    "      <a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>\n" +
                    "    </a:ReplyTo>\n" +
                    "    <a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/extSTS.srf</a:To>\n" +
                    "    <o:Security s:mustUnderstand=\"1\"\n" +
                    "       xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                    "      <o:UsernameToken>\n" +
                    "        <o:Username>${username}</o:Username>\n" +
                    "        <o:Password>${password}</o:Password>\n" +
                    "      </o:UsernameToken>\n" +
                    "    </o:Security>\n" +
                    "  </s:Header>\n" +
                    "  <s:Body>\n" +
                    "    <t:RequestSecurityToken xmlns:t=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">\n" +
                    "      <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "        <a:EndpointReference>\n" +
                    "          <a:Address>${endpoint}</a:Address>\n" +
                    "        </a:EndpointReference>\n" +
                    "      </wsp:AppliesTo>\n" +
                    "      <t:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</t:KeyType>\n" +
                    "      <t:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</t:RequestType>\n" +
                    "      <t:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</t:TokenType>\n" +
                    "    </t:RequestSecurityToken>\n" +
                    "  </s:Body>\n" +
                    "</s:Envelope>";

    private final HttpClient httpClient;
    private final String baseUrl;

    public UserPasswordAuthenticator(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public String authenticate(String username, String password)
            throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException, SAXException {
        String token = getSecurityToken(username, password);
        signin(token);
        return fetchBearerToken();
    }

    private String getSecurityToken(String username, String password) throws IOException, InterruptedException,
            ParserConfigurationException, SAXException, XPathExpressionException {
        String authentication = REQUEST_SECURITY_TOKEN_TEMPLATE
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
}
