/*
 * (C) Copyright IBM Corp. 2021, 2022
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

import org.hl7.fhir.r4.model.Bundle.BundleType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

/**
 * Converts all HL7 messages found in a folder, writes the resulting JSONs to a specified location.
 * 
 * Uses the following Java system properties:
 * - hl7.input.folder : directory containing the HL7 files to convert
 * - hl7.output.folder : directory to write output to; directory must exist, files will be overwritten if they exist
 * - hl7.output.folder.flat : Indicates whether to create subdirectories to match input directory structure when
 * hl7.tools.recurse.subfolders=true. Defaults to false.
 * - hl7.tools.debug : Set to "true" to output input HL7 and output JSON to the console. When "false" output JSONs
 * will only be written to files. Defaults to false.
 * - hl7.tools.recurse.subfolders : When "true" will recurse input folder sub-directories. Output will also be placed in
 * sub-directories. Defaults to "true".
 * 
 * This class uses a main() method; run as a Java application.
 */
public class FHIRConverterRunFolder {

    String inputFolderName;
    String outputFolderName;
    boolean flattenOutputFolders;
    boolean traverseSubFolders;
    boolean debug;
    int numConvertedFiles = 0;
    int numConvertedFolders = 0;
    HL7ToFHIRConverter hl7Converter;
    ConverterOptions hl7ConverterOptions;
    Gson gson;

    public FHIRConverterRunFolder(String inputFolderName, String outputFolderName, boolean traverseSubFolders,
            boolean flattenOutputFolders, boolean debug) {
        this.inputFolderName = inputFolderName;
        this.outputFolderName = outputFolderName;
        this.traverseSubFolders = traverseSubFolders;
        this.flattenOutputFolders = flattenOutputFolders;
        this.debug = debug;
        hl7Converter = new HL7ToFHIRConverter();
        hl7ConverterOptions = new Builder()
                .withBundleType(BundleType.COLLECTION)
                .withProperty("TENANT", "tenantid")
                .withValidateResource()
                .withPrettyPrint()
                .build();
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public static void main(String[] args) throws IOException {
        String inputFolderName = System.getProperty("hl7.input.folder");
        if (inputFolderName == null) {
            System.out.println("Java property hl7.input.folder not found");
            return;
        }
        File inputFolder = new File(inputFolderName);
        if (!inputFolder.exists()) {
            System.out.println("Input folder " + inputFolderName + " not found");
            return;
        }

        String outputFolderName = System.getProperty("hl7.output.folder");
        if (outputFolderName == null) {
            System.out.println("Java property hl7.output.folder not found");
            return;
        }
        File outputFolder = new File(outputFolderName);
        if (!outputFolder.exists()) {
            System.out.println("Output folder " + outputFolderName + " not found");
            return;
        }

        String flattenOutput = System.getProperty("hl7.output.folder.flat");
        boolean flattenOutputFolders = false;
        if (flattenOutput != null && flattenOutput.equalsIgnoreCase("true")) {
            flattenOutputFolders = true;
        }

        String debugStr = System.getProperty("hl7.tools.debug");
        boolean debug = false;
        if (debugStr != null && debugStr.equalsIgnoreCase("true")) {
            debug = true;
        }

        String traverseSubdirectoriesStr = System.getProperty("hl7.tools.recurse.subfolders");
        boolean traverseSubdirectories = true;
        if (traverseSubdirectoriesStr != null && traverseSubdirectoriesStr.equalsIgnoreCase("false")) {
            traverseSubdirectories = false;
        }

        FHIRConverterRunFolder converter = new FHIRConverterRunFolder(inputFolderName, outputFolderName,
                traverseSubdirectories, flattenOutputFolders, debug);

        converter.processFolder(inputFolder, outputFolderName);

        System.out.println("Done! Converted " + converter.numberOfConvertedFiles() + " files in "
                + converter.numberOfConvertedFolders() + " folders, output written to " + outputFolderName);
    }

    public int numberOfConvertedFiles() {
        return numConvertedFiles;
    }

    public int numberOfConvertedFolders() {
        return numConvertedFolders;
    }

    public void processFolder(File inputFolder, String outputFolderName) {
        try {
            // get the list of Files in the Folder
            File[] fileList = inputFolder.listFiles(File::isFile);
            int numFiles = fileList.length;
            int currentFileNum = 1;

            System.out.println("Processing directory: " + inputFolder);
            if (numFiles == 0) {
                return;
            }
            File outputFolder = null;

            // for each file with extension .hl7 in the folder, run the HL7 to FHIR conversion
            for (File file : fileList) {
                String currentFileName = file.getName();
                if (file.isFile() && getFileExtension(currentFileName).equals("hl7")) {
                    String hl7MessageString = new String(Files.readAllBytes(Paths.get(file.toString())),
                            StandardCharsets.UTF_8);
                    if (debug) {
                        System.out.println("    Input HL7 message:\n" + hl7MessageString);
                    }

                    System.out.println("    Processing file " + currentFileNum + " of " + numFiles +
                            ": " + currentFileName);

                    String json;
                    try {
                        // Convert from HL7 to JSON
                        json = hl7Converter.convert(hl7MessageString, hl7ConverterOptions);
                    } catch (UnsupportedOperationException e1) {
                        String msg = "        Unsupported message type";
                        System.out.println(msg);
                        json = "{\"Error\":\"" + msg.trim() + "\"}";
                    } catch (IllegalArgumentException e2) {
                        String msg = e2.getMessage();
                        System.out.println("        " + msg);
                        json = "{\"Error\":\"" + msg + "\"}";
                    }

                    // Create a JSON object to be able to pretty print 
                    JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
                    String prettyJson = gson.toJson(jsonObj);
                    if (debug) {
                        System.out.println("Output JSON message:\n" + prettyJson);
                    }

                    // Write result JSON files to file system
                    if (outputFolder == null) {
                        ++numConvertedFolders;
                        outputFolder = new File(outputFolderName);
                        if (!outputFolder.exists()) {
                            outputFolder.mkdir();
                        }
                    }
                    int dotLoc = file.getName().indexOf(".");
                    String fileName = file.getName().substring(0, dotLoc);
                    String fileOut = outputFolder + File.separator + fileName + ".json";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(fileOut));
                    writer.write(prettyJson);
                    writer.close();

                    ++numConvertedFiles;

                } else { // file does not have hl7 extension
                    System.out.println("    Skipping non-hl7 file: " + currentFileName);
                }
                ++currentFileNum;
            }

            // Recurse through the folders in the directory
            File[] folderList = inputFolder.listFiles(File::isDirectory);
            for (File folder : folderList) {
                String outputSubfolderName = outputFolderName;
                if (!flattenOutputFolders) {
                    outputSubfolderName = outputFolderName + File.separator + folder.getName();
                }
                processFolder(folder, outputSubfolderName);
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
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
