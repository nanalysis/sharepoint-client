# Sharepoint client

A basic Java library and command-line tool to manage files hosted on sharepoint (either directly or from teams).
Only files and folders are supported, everything else is out of scope.

Both legacy user/password authentication and OAuth2 with client id are supported.

After Sharepoint ACS deprecation and new Microsoft Entra ID, you must have the proper set of permission to read/write
files.
Depending on those permissions, you may use Sharepoint API or Graph API.

## Usage

### From command-line

> java -jar sharepoint-client.jar url site auth_method login password action [options]
 
Possible authentication methods are:

- user: use user and password credentials (not supported for Graph API)
- api: use OAuth2 with client id and secret
- msal4j: use OAuth2 through MSAL4J with client id and secret

Possible API are:

- sharepoint: Use Sharepoint API
- graph: Use Graph API (default on command-line)

Possible actions are:
- upload-folder <local-path> <remote-path> <new-folder-name>
- delete-folder <remote-path>
- list-folders <remote-path>
- list-files <remote-path>
- download <remote-folder-path> <file-name>

Some examples:
> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment user you@company.com password
> upload-folder /tmp/folder "Software" NewFolder

> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment api clientId clientSecret
> delete-folder "Software/NewFolder"

Please note that `upload-folder` isn't recursive, and expects a flat file hierarchy.

### From Java

#### Maven dependency
Add the `nanalysis-public` maven repository:

    <repositories>
        <repository>
            <id>nanalysis-public</id>
            <url>https://raw.githubusercontent.com/nanalysis/maven-repository/public/</url>
        </repository>
    </repositories>

Then declare the dependency:

    <dependency>
        <groupId>com.nanalysis</groupId>
        <artifactId>sharepoint-client</artifactId>
        <version>2.1</version>
    </dependency>

#### Code usage:

See `ManualTests` class for examples. 

## Credits

Information about OAuth2 was found there:

* https://learn.microsoft.com/en-us/entra/architecture/auth-oauth2

Information on Microsoft Graph API:

* https://learn.microsoft.com/en-us/graph/api/resources/sharepoint?view=graph-rest-1.0

## Licensing

This library is published under the GNU GPL v3, and an internal proprietary license for use in Nanalysis, RS2D and OneMoonScientific projects.

## Changelog

* 2.1: Add msal4j authentication method, also based on OAuth2.
* 2.0: Update OAuth2 authentication following ACS removal, support Graph API which becomes command-line default, move to
  JDK17.
* 1.4: Add list files/folders and download commands.
* 1.3: Chunked upload for big files.
* 1.2: OAuth2 support.
* 1.1: Add log on folder deletion, error exit one exception.
* 1: First version of Sharepoint client.
