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
import java.io.IOException;

public class CommandLineClient {
    private static void upload(SharepointClient sharepoint, String[] args) throws Exception {
        if (args.length != 8) {
            System.err.println("upload options are: <remote-parent> <new-folder-name> <local-path>");
            System.exit(1);
        }

        String localPath = args[5];
        String remotePath = args[6];
        String folderName = args[7];

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
            sharepoint.uploadFile(path, f.getName(), f);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("usage: java -jar sharepoint-client.jar <url> <site> <login> <password> <action> [options], with possible actions being:");
            System.err.println("- upload <local-path> <remote-parent> <new-folder-name>");
            System.err.println();
            System.err.println("example: java -jar sharepoint-client.jar https://xxx.sharepoint.com ProductDevelopment \"you@company.com\" \"password\" upload /tmp/folder \"Shared Documents/Software/Temporary\" \"NewFolder\"");
            System.exit(1);
        }


        SharepointClient sharepoint = new SharepointClient(args[0], args[1]);
        sharepoint.authenticate(args[2], args[3]);
        String action = args[4];

        if (action.equals("upload")) {
            upload(sharepoint, args);
        } else {
            System.err.println("Unknown action: " + action);
            System.exit(1);
        }
    }
}
