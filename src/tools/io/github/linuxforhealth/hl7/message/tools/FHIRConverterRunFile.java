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

import org.hl7.fhir.r4.model.Bundle.BundleType;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

/**
 * Converts a HL7 message in a file, using the following by Java system properties:
 * - hl7.input.file - input HL7 file, qualified as necessary
 * - hl7.output.folder (directory must exist, file will be overwritten if it exist)
 * 
 * The resulting JSON is printed to System.out, and if hl7.output.folder is specified, to
 * this directory using the input filename with a .json file extension replacing the .hl7
 * input file extension.
 * 
 * This class uses a main() method; run as a Java application.
 */
public class FHIRConverterRunFile {

    public static void main(String[] args) throws IOException {
        String inputFileName = System.getProperty("hl7.input.file");
        if (inputFileName == null) {
            System.out.println("Java property hl7.input.file not found");
            return;
        }
        File inputFile = new File(inputFileName);
        if (!inputFile.exists()) {
            System.out.println("Input file " + inputFile + " not found");
            return;
        }

        String outputFolderName = System.getProperty("hl7.output.folder");
        File outputFolder = null;
        if (outputFolderName != null) {
            outputFolder = new File(outputFolderName);
            if (!outputFolder.exists()) {
                System.out.println("Output folder " + outputFolderName + " not found");
                return;
            }
            if (!outputFolder.isDirectory()) {
                System.out.println("Output folder " + outputFolderName + " not a directory");
                return;
            }
        }

        System.out.println("Converting file: " + inputFile);
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        ConverterOptions options = new Builder()
                .withBundleType(BundleType.COLLECTION)
                .withValidateResource()
                .withPrettyPrint()
                .build();

        String json = ftv.convert(inputFile, options);
        if(json == null) json = "Unable to convert the HL7 file - see logs";
        System.out.println("----------------");
        System.out.println(json);
        System.out.println("----------------");

        if (outputFolderName != null) {
            int dot = inputFileName.lastIndexOf('.');
            int pathSep = inputFileName.lastIndexOf(File.separator);
            String outfilename = outputFolderName + inputFileName.substring(pathSep, dot) + ".json";
            System.out.println("Writing results to " + outfilename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename));
            writer.write(json);
            writer.close();
        }
    }

}
