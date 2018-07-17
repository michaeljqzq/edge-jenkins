/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.iotedge;


import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.model.AzureCredentialCache;
import com.microsoft.jenkins.iotedge.model.AzureCredentialsValidationException;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ShellExecuter {

    public PrintStream logger;
    public File workspace;

    public ShellExecuter(PrintStream logger, File workspace) {
        this.logger = logger;
        this.workspace = workspace;
    }

    public ShellExecuter(PrintStream logger) {
        this.logger = logger;
    }

    public ShellExecuter() {

    }

    public void login(AzureCredentialCache credentialsCache) throws AzureCredentialsValidationException {
        String command = "az login --service-principal -u " + credentialsCache.clientId + " -p " + credentialsCache.clientSecret + " --tenant " + credentialsCache.tenantId;
        try {
            executeAZ(command, false);
            command = "az account set -s " + credentialsCache.subscriptionId;
            executeAZ(command, false);
        } catch (AzureCloudException e) {
            throw new AzureCredentialsValidationException(e.getMessage());
        }
    }

    public String getVersion() throws AzureCloudException {
        String command = "az --version";
        ExitResult result = executeCommand(command);
        if (result.code == 0) {
            return result.output;
        }
        throw AzureCloudException.create("Azure CLI not found");
    }

    public String executeAZ(String command, Boolean printCommand) throws AzureCloudException {
        if (printCommand) {
            if(logger != null) logger.println("Running: " + command);
        }
        ExitResult result = executeCommand(command);
        if (result.code == 0) {
            if(logger != null) logger.println(result.output);
            return result.output;
        }
        throw AzureCloudException.create(result.output);
    }

    private static class ExitResult {
        public String output;
        public int code;

        ExitResult(String output, int code) {
            this.output = output;
            this.code = code;
        }
    }

    private ExitResult executeCommand(String command) {


        StringBuffer output = new StringBuffer();

        Process p;
        int exitCode = -1;
        try {
            if (File.pathSeparatorChar == ':') {
                p = Runtime.getRuntime().exec("/bin/sh -c \"" + command + "\"", null, workspace);
            } else {
                p = Runtime.getRuntime().exec("cmd.exe /c \"" + command + "\"", null, workspace);
            }
            p.waitFor();

            InputStream stream;

            if (p.exitValue() != 0) {
                stream = p.getErrorStream();
            } else {
                stream = p.getInputStream();
            }
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream, "utf-8"));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            exitCode = p.exitValue();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());

        }
        return new ExitResult(output.toString(), exitCode);
    }
}
