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

/**
 * XML or json templates used by sharepoint client.
 */
public class Templates {
    public static final String REQUEST_SECURITY_TOKEN =
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
}
