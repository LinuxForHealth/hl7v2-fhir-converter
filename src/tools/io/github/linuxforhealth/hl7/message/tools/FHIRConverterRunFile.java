/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.hl7.fhir.r4.model.Bundle.BundleType;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

/**
 * Converts a HL7 message in a file specified by Java system property:
 * hl7.filename
 * The resulting JSON is printed to System.out.
 * This class uses a main() method; run as a Java application.
 */
public class FHIRConverterRunFile {

    public static void main(String[] args) throws IOException {
        String filename = System.getProperty("hl7.filename");
        System.out.println("Converting file: " + filename);
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String everything = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            everything = sb.toString();
        } finally {
            br.close();
        }

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        ConverterOptions options = new Builder()
                .withBundleType(BundleType.COLLECTION)
                .withValidateResource()
                .withPrettyPrint()
                .build();

        String json = ftv.convert(new File(filename), options);
        System.out.println("----------------");
        System.out.println(json);
        System.out.println("----------------");

        int dot = filename.lastIndexOf('.');
        int pathSep = filename.lastIndexOf(File.separator);
        String path = "C:\\aaawork\\code\\temp files\\";
        String outfilename = path + filename.substring(pathSep, dot) + "-conversion.json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename));
        writer.write(json);
        writer.close();
    }

}
