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
    private static final String UPLOAD_FOLDER = "upload-folder";
    private static final String DELETE_FOLDER = "delete-folder";
    private static final String LIST_FOLDERS = "list-folders";
    private static final String LIST_FILES = "list-files";
    private static final String DOWNLOAD = "download";

    // use chucked upload if file is bigger than that
    private static final int FILE_SIZE_THRESHOLD = 30 * 1024 * 1024; // 30MB

    private static void uploadFolder(SharepointClient sharepoint, String[] options) throws Exception {
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

    private static void deleteFolder(SharepointClient sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(DELETE_FOLDER + " options are: <remote-path>");
        }

        String path = options[0];
        System.out.println("Deleting folder: " + path);
        sharepoint.deleteFolder(path);
    }

    private static void listFolders(SharepointClient sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(LIST_FOLDERS + " options are: <remote-path>");
        }

        String path = options[0];
        List<String> files = sharepoint.listFolders(path);
        files.forEach(System.out::println);
    }

    private static void listFiles(SharepointClient sharepoint, String[] options) throws Exception {
        if (options.length != 1) {
            throw new IllegalArgumentException(LIST_FILES + " options are: <remote-path>");
        }

        String path = options[0];
        List<String> files = sharepoint.listFiles(path);
        files.forEach(System.out::println);
    }

    private static void download(SharepointClient sharepoint, String[] options) throws Exception {
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

    private static void executeAction(SharepointClient sharepoint, String action, String[] options) throws Exception {
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

    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("usage: java -jar sharepoint-client.jar <url> <site> <auth_method> <login|client_id> <password|client_secret> <action> [options].");
            System.err.println("Authentication methods are: ");
            System.err.println(" - user: uses login and password access");
            System.err.println(" - api: uses OAuth2 with client id and client secret");
            System.err.println("Possible actions are: ");
            System.err.println("- " + UPLOAD_FOLDER + " <local-path> <remote-path> <new-folder-name>");
            System.err.println("- " + DELETE_FOLDER + " <remote-path>");
            System.err.println("- " + LIST_FOLDERS + " <remote-path>");
            System.err.println("- " + LIST_FILES + " <remote-path>");
            System.err.println("- " + DOWNLOAD + " <remote-folder-path> <file-name>");
            System.err.println();
            System.err.println("examples:");
            System.err.println("> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment user you@company.com password " + UPLOAD_FOLDER + " /tmp/folder \"Shared Documents/Software/Temporary\" \"NewFolder\"");
            System.err.println("> java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment api someid somesecret " + DELETE_FOLDER + " \"Shared Documents/Software/Temporary/NewFolder\"");
            System.exit(1);
        }

        String baseUrl = args[0];
        String site = args[1];
        String authMethod = args[2];
        String login = args[3];
        String password = args[4];
        String action = args[5];
        String[] options = Arrays.copyOfRange(args, 6, args.length);

        try {
            SharepointClient sharepoint = new SharepointClient(baseUrl, site);
            if(authMethod.equalsIgnoreCase("user")) {
                sharepoint.authenticateWithUserCredentials(login, password);
            } else if(authMethod.equalsIgnoreCase("api")) {
                sharepoint.authenticateWithOAuth2(login, password);
            } else {
                throw new IllegalArgumentException("Unknown authentication method: " + authMethod);
            }
            executeAction(sharepoint, action, options);
        } catch (Exception e) {
            System.err.println("Failure: " + e.getMessage());
            System.exit(2);
        }
    }
}
