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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

/**
 * Used to test the features from an IDE. Not meant to be called automatically.
 * Addresses and credentials must be set before use.
 */
public class ManualTests {
    private static final String BASE_URL = "https://xxx.sharepoint.com";
    private static final String SITE = "EXT-Test";
    private static final String TEST_PATH = "Shared Documents/General/Test";
    private static final String BIG_FILE_LOCAL_PATH = "xxx";
    private static final String USERNAME_OR_CLIENT_ID = "xxx";
    private static final String PASSWORD_OR_CLIENT_SECRET = "xxx";
    private static final boolean USE_OAUTH2 = true;

    private SharepointClient client;

    @Before
    public void setup() throws Exception {
        client = new SharepointClient(BASE_URL, SITE);
        if(USE_OAUTH2) {
            client.authenticateWithOAuth2(USERNAME_OR_CLIENT_ID, PASSWORD_OR_CLIENT_SECRET);
        } else {
            client.authenticateWithUserCredentials(USERNAME_OR_CLIENT_ID, PASSWORD_OR_CLIENT_SECRET);
        }
    }


    @Ignore("Manual test")
    @Test
    public void listFolders() throws Exception {
        var folders = client.listFolders(TEST_PATH);
        System.out.println(folders);
    }

    @Ignore("Manual test")
    @Test
    public void listFiles() throws Exception {
        var files = client.listFiles(TEST_PATH);
        System.out.println(files);
    }

    @Ignore("Manual test")
    @Test
    public void createFolder() throws Exception {
        client.createFolder(TEST_PATH, "NewFolder");
        System.out.println("Folder created");
    }

    @Ignore("Manual test")
    @Test
    public void uploadFile() throws Exception {
        client.uploadFile(TEST_PATH, "test.txt", "Hello world".getBytes());
        System.out.println("File created");
    }

    @Ignore("Manual test")
    @Test
    public void uploadBigFile() throws Exception {
        File f = new File(BIG_FILE_LOCAL_PATH);
        Consumer<Double> progress = percent -> System.out.printf("Uploading: %.2f%%%n", percent);
        client.uploadBigFile(TEST_PATH, f.getName(), f.length(), new FileInputStream(f), progress);
        System.out.println("File created");
    }

    @Ignore("Manual test")
    @Test
    public void deleteFile() throws Exception {
        client.deleteFile(TEST_PATH, "test.txt");
        System.out.println("File deleted");
    }


    @Ignore("Manual test")
    @Test
    public void deleteFolder() throws Exception {
        client.deleteFolder(TEST_PATH + "/NewFolder");
        System.out.println("Folder deleted");
    }
}
