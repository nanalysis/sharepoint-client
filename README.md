# Share point client

A basic Java library and command-line tool to manage files hosted on sharepoint (either directly or from teams).
Only files and folders are supported, everything else is out of scope.

Both legacy user/password authentication and OAuth2 with client id are supported.

## Usage

### From command-line

> java -jar sharepoint-client.jar url site auth_method login password action [options]
 
Possible authentication methods are:
- user: use user and password credentials
- api: use OAuth2 with client id and secret

Possible actions are:
- upload-folder <local-path> <remote-path> <new-folder-name>
- delete-folder <remote-path>

Some examples:
> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment user you@company.com password upload-folder /tmp/folder "Shared Documents/Software" NewFolder

> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment api clientId clientSecret delete-folder "Shared Documents/Software/NewFolder"

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
        <version>1.2</version>
    </dependency>

#### Code usage:

See `ManualTests` class for examples. 

## Credits

A lot of information regarding user authentication was found on this blog post:
* https://paulryan.com.au/2014/spo-remote-authentication-rest/

Information about OAuth2 was found there:
* https://www.techsupper.com/2019/05/access-sharepoint-online-rest-apis.html

## Licensing

This library is published under the GNU GPL v3, and an internal proprietary license for use in Nanalysis, RS2D and OneMoonScientific projects.
