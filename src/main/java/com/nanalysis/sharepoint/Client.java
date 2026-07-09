package com.nanalysis.sharepoint;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public interface Client {

    void authenticateWithUserCredentials(String username, String password)
            throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException, SAXException;

    void authenticateWithOAuth2(String clientId, String clientSecret) throws IOException, InterruptedException;

    List<String> listFolders(String path) throws IOException, InterruptedException;

    List<String> listFiles(String path) throws IOException, InterruptedException;

    void createFolder(String parent, String folderName) throws IOException, InterruptedException;

    void deleteFolder(String path) throws IOException, InterruptedException;

    void deleteFile(String folder, String filename) throws IOException, InterruptedException;

    void uploadFile(String folder, String filename, File file) throws IOException, InterruptedException;

    void uploadFile(String folder, String filename, byte[] data) throws IOException, InterruptedException;

    void uploadBigFile(String folder, String filename, long size, InputStream input, Consumer<Double> progressCallback)
            throws IOException, InterruptedException;

    InputStream download(String folder, String filename) throws IOException, InterruptedException;
}
