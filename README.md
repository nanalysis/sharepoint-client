# Share point client

A basic Java library and command-line tool to manage files hosted on sharepoint (either directly or from teams).
Only files and folders are supported, everything else is out of scope.

## Usage

### From command-line

> java -jar sharepoint-client.jar url site login password action [options]
 
Possible actions are:
- upload-folder <local-path> <remote-path> <new-folder-name>
- delete-folder <remote-path>

Some examples:
> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment you@company.com password upload-folder /tmp/folder "Shared Documents/Software/" NewFolder

> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment you@company.com password delete-folder "Shared Documents/Software/NewFolder"

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
        <version>1.0</version>
    </dependency>

#### Code usage:

See `ManualTests` class for examples. 

## Credits

A lot of information regarding authentication was found on this blog post:
* https://paulryan.com.au/2014/spo-remote-authentication-rest/

## Licensing

This library is published under the GNU GPL v3, and an internal proprietary license for use in Nanalysis, RS2D and OneMoonScientific projects.
