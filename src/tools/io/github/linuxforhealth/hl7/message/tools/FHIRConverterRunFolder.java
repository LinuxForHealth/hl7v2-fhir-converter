/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.message.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

/**
 * Converts all HL7 messages found in a folder, writes the resulting JSONs to a specified location.
 * Uses the following Java system properties:
 * - hl7.input.folder
 * - hl7.output.folder (directory must exist, files will be overwritten if they exist)
 * - hl7.tools.debug (set to "true" to output input HL7 and output JSON to the console as well as to files)
 * This class uses a main() method; run as a Java application.
 */
public class FHIRConverterRunFolder {

    public static void main(String[] args) throws IOException {
        String hl7MessageString;

        String inputFolderName = System.getProperty("hl7.input.folder");
        if (inputFolderName == null) {
            System.out.println("Java property hl7.input.folder not found");
            return;
        }
        String outputFolderName = System.getProperty("hl7.output.folder");
        if (outputFolderName == null) {
            System.out.println("Java property hl7.output.folder not found");
            return;
        }
        String debugStr = System.getProperty("hl7.tools.debug");
        boolean debug = false;
        if (debugStr != null && debugStr.equalsIgnoreCase("true")) {
            debug = true;
        }

        int numConvertedFiles = 0;
        try {
            //get the list of Files in the Folder
            File inputFolder = new File(inputFolderName);
            if (!inputFolder.exists()) {
                System.out.println("Intput folder " + inputFolderName + " not found");
                return;
            }
            File outputFolder = new File(outputFolderName);
            if (!outputFolder.exists()) {
                System.out.println("Output folder " + outputFolderName + " not found");
                return;
            }
            File[] fileList = inputFolder.listFiles();
            int numFiles = fileList.length;
            int currentFileNum = 1;

            // for each file with extension .hl7 in the folder, run the HL7 to FHIR conversion
            for (File file : fileList) {
                String currentFileName = file.getName();
                ++currentFileNum;
                if (file.isFile() && getFileExtension(currentFileName).equals("hl7")) {
                    hl7MessageString = new String(Files.readAllBytes(Paths.get(file.toString())),
                            StandardCharsets.UTF_8);
                    if (debug) {
                        System.out.println("Input HL7 message:\n" + hl7MessageString);
                    }

                    if (debug) {
                        System.out.println(
                                "Processing file " + currentFileNum + " of " + numFiles + ": " + currentFileName);
                    } else {
                        System.out.println("Processing file:" + currentFileName);
                    }

                    // Convert from HL7 to JSON
                    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
                    String json = ftv.convert(hl7MessageString);

                    // Create a JSON object to be able to pretty print 
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObj = parser.parse(json).getAsJsonObject();
                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                    String prettyJson = gson.toJson(jsonObj);
                    if (debug) {
                        System.out.println("Output JSON message:\n" + prettyJson);
                    }

                    // Write result JSON files to file system
                    int dotLoc = file.getName().indexOf(".");
                    String fileName = file.getName().substring(0, dotLoc);
                    String fileOut = outputFolder + File.separator + fileName + ".json";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(fileOut));
                    writer.write(prettyJson);
                    writer.close();
                    ++numConvertedFiles;
                } else if (debug) {
                    System.out.println("Skipping file:" + currentFileName);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        System.out.println("Done! Converted " + numConvertedFiles + " files");
    }

    private static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "";
        }

    }

}
