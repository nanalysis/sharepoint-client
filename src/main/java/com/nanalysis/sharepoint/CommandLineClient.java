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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class CommandLineClient {
    private static final String AUTH_USER = "user";
    private static final String AUTH_OAUTH2 = "api";
    private static final String API_GRAPH = "graph";
    private static final String API_SHAREPOINT = "sharepoint";
    private static final String UPLOAD_FOLDER = "upload-folder";
    private static final String DELETE_FOLDER = "delete-folder";
    private static final String LIST_FOLDERS = "list-folders";
    private static final String LIST_FILES = "list-files";
    private static final String DOWNLOAD = "download";

    // use chucked upload if file is bigger than that
    private static final int FILE_SIZE_THRESHOLD = 30 * 1024 * 1024; // 30MB

    private static void uploadFolder(Client sharepoint, String[] options) throws Exception {
        if (options.length != 3) {
            throw new IllegalArgumentException(UPLOAD_FOLDER + " options are: <remote-parent> <new-folder-name> <local-path>");
        }

        String localPath = options[0];
        String remotePath = options[1];
        String folderName = options[2];

        File local = new File(localPath);
        if (!local.isDirectory()) {
            throw new IOException("Not a valid local directory: " + local.getAbsolutePath());
        }

        File[] files = local.listFiles(File::isFile);
        if (files == null) {
            throw new IOException("Unable to list files in " + local.getAbsolutePath());
        }

        String path = remotePath + "/" + folderName;
        System.out.println("Creating folder: " + path);
        sharepoint.createFolder(remotePath, folderName);

        for (File f : files) {
            System.out.println("Uploading: " + f.getName());
            if(f.length() < FILE_SIZE_THRESHOLD) {
                sharepoint.uploadFile(path, f.getName(), f);
            } else {
                try(FileInputStream input = new FileInputStream(f)) {
                    sharepoint.uploadBigFile(path, f.getName(), f.length(), input,
                            percent -> System.out.printf("\r... %.2f%%%n", percent));
                }
            }
        }
    }

    private static void deleteFolder(Client sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(DELETE_FOLDER + " options are: <remote-path>");
        }

        String path = options[0];
        System.out.println("Deleting folder: " + path);
        sharepoint.deleteFolder(path);
    }

    private static void listFolders(Client sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(LIST_FOLDERS + " options are: <remote-path>");
        }

        String path = options[0];
        List<String> files = sharepoint.listFolders(path);
        files.forEach(System.out::println);
    }

    private static void listFiles(Client sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(LIST_FILES + " options are: <remote-path>");
        }

        String path = options[0];
        List<String> files = sharepoint.listFiles(path);
        files.forEach(System.out::println);
    }

    private static void download(Client sharepoint, String[] options) throws Exception {
        if (options.length != 2) {
            throw new IllegalArgumentException(LIST_FILES + " options are: <remote-parent> <file-name>");
        }

        String folder = options[0];
        String fileName = options[1];
        System.out.println("Downloading file: " + fileName);
        try(InputStream input = sharepoint.download(folder, fileName)) {
            Files.copy(input, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void executeAction(Client sharepoint, String action, String[] options) throws Exception {
        switch (action) {
            case UPLOAD_FOLDER:
                uploadFolder(sharepoint, options);
                break;
            case DELETE_FOLDER:
                deleteFolder(sharepoint, options);
                break;
            case LIST_FOLDERS:
                listFolders(sharepoint, options);
                break;
            case LIST_FILES:
                listFiles(sharepoint, options);
                break;
            case DOWNLOAD:
                download(sharepoint, options);
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    /**
     * @param str String to check.
     * @return True if it corresponds to one of the allowed API names.
     */
    private static boolean isAPI(String str) {
        return API_SHAREPOINT.equalsIgnoreCase(str) || API_GRAPH.equalsIgnoreCase(str);
    }

    /**
     * @param baseUrl    Base URL for host.
     * @param site       Specific site to target.
     * @param authMethod Authentication method.
     * @param login      Login/Id to use.
     * @param password   Password/Token to use.
     * @param api        API to use.
     * @param action     Action to perform.
     * @param options    Action options.
     */
    private static void command(String baseUrl, String site, String authMethod, String login, String password, API api, String action, String[] options) {
        try {
            Client client = api == API.Sharepoint ? new SharepointClient(baseUrl, site) : new GraphClient(baseUrl, site);
            if (authMethod.equalsIgnoreCase(AUTH_USER)) {
                client.authenticateWithUserCredentials(login, password);
            } else if (authMethod.equalsIgnoreCase(AUTH_OAUTH2)) {
                client.authenticateWithOAuth2(login, password);
            } else {
                throw new IllegalArgumentException("Unknown authentication method: " + authMethod);
            }
            executeAction(client, action, options);
        } catch (Exception e) {
            System.err.println("Failure: " + e.getMessage());
            System.exit(2);
        }
    }

    public static void main(String[] args) {
        if (args.length < 6 || (isAPI(args[5]) && args.length < 7)) {
            System.err.println("usage: java -jar sharepoint-client.jar <url> <site> <auth_method> <login|client_id> <password|client_secret> <?api> <action> [options].");
            System.err.println("Authentication methods are: ");
            System.err.printf(" - %s: uses login and password access%n", AUTH_USER);
            System.err.printf(" - %s: uses OAuth2 with client id and client secret%n", AUTH_OAUTH2);
            System.err.println("Possible actions are: ");
            System.err.printf(" - %s <local-path> <remote-path> <new-folder-name>%n", UPLOAD_FOLDER);
            System.err.printf(" - %s <remote-path>%n", DELETE_FOLDER);
            System.err.printf(" - %s <remote-path>%n", LIST_FOLDERS);
            System.err.printf(" - %s <remote-path>%n", LIST_FILES);
            System.err.printf(" - %s <remote-folder-path> <file-name>%n", DOWNLOAD);
            System.err.println("Possible APIs are: ");
            System.err.printf(" - %s: uses Sharepoint API to access resources%n", API_SHAREPOINT);
            System.err.printf(" - %s: uses Microsoft Graph API to access resources (default)%n", API_GRAPH);
            System.err.println();
            System.err.println("examples:");
            System.err.printf("> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment user you@company.com password %s /tmp/folder \"Software/Temporary\" \"NewFolder\"%n", UPLOAD_FOLDER);
            System.err.printf("> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment api someid somesecret %s \"Software/Temporary/NewFolder\"%n", DELETE_FOLDER);
            System.err.printf("> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment api someid somesecret %s %s \"Shared Documents/Software/Temporary/NewFolder\"%n", API_SHAREPOINT, DELETE_FOLDER);
            System.exit(1);
        }

        String baseUrl = args[0];
        String site = args[1];
        String authMethod = args[2];
        String login = args[3];
        String password = args[4];
        // If 5th argument is used for API, then action is on 6th and options after that. If not, 5th argument is action.
        if (API_SHAREPOINT.equalsIgnoreCase(args[5])) {
            command(baseUrl, site, authMethod, login, password, API.Sharepoint, args[6], Arrays.copyOfRange(args, 7, args.length));
        } else if (API_GRAPH.equalsIgnoreCase(args[5])) {
            command(baseUrl, site, authMethod, login, password, API.Graph, args[6], Arrays.copyOfRange(args, 7, args.length));
        } else {
            command(baseUrl, site, authMethod, login, password, API.Graph, args[5], Arrays.copyOfRange(args, 6, args.length));
        }
    }
}
