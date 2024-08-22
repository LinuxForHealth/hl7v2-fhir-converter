/*
 * (C) Copyright IBM Corp. 2020, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import io.github.linuxforhealth.core.terminology.TerminologyLookup;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.message.HL7MessageEngine;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Converts HL7 message to FHIR bundle resource based on the customizable templates.
 *
 * @author pbhallam
 */
public class HL7ToFHIRConverter {
    private static HL7HapiParser hparser = new HL7HapiParser();
    private static final Logger LOGGER = LoggerFactory.getLogger(HL7ToFHIRConverter.class);
    private Map<String, HL7MessageModel> messagetemplates = new HashMap<>();

    /**
     * Constructor initialized all the templates used for converting the HL7 to FHIR bundle resource.
     * 
     * @throws IllegalStateException - If any issues are encountered when loading the templates.
     */
    public HL7ToFHIRConverter() {

        try {
            messagetemplates.putAll(ResourceReader.getInstance().getMessageTemplates());
            TerminologyLookup.init();
            UrlLookup.init();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failure to initialize the templates for the converter.", e);
        }
    }

    /**
     * Converts the input HL7 file (.hl7) into FHIR bundle resource.
     * 
     * @param hl7MessageFile - Single message only
     * @return JSON representation of FHIR {@link Bundle} resource. If bundle type if not specified
     *         then the default bundle type is used BundleType.COLLECTION
     * @throws IOException - if message file cannot be found
     * @throws UnsupportedOperationException - if message type is not supported
     * @throws IllegalArgumentException - if illegal arguments found
     */
    public String convert(File hl7MessageFile) throws IOException {
        Preconditions.checkArgument(hl7MessageFile != null, "Input HL7 message file cannot be null.");
        return convert(hl7MessageFile, ConverterOptions.SIMPLE_OPTIONS);

    }

    /**
     * Converts the input HL7 file (.hl7) into FHIR bundle resource.
     * 
     * @param hl7MessageFile File containing HL7 message to convert
     * @param options Options to use in conversion
     * 
     * @return JSON representation of FHIR {@link Bundle} resource.
     * @throws IOException - if message file cannot be found
     * @throws UnsupportedOperationException - if message type is not supported
     */
    public String convert(File hl7MessageFile, ConverterOptions options) throws IOException {
        Preconditions.checkArgument(hl7MessageFile != null, "Input HL7 message file cannot be null.");
        return convert(FileUtils.readFileToString(hl7MessageFile, StandardCharsets.UTF_8), options);

    }

    /**
     * Converts the input HL7 message (String data) into FHIR bundle resource.
     * 
     * @param hl7MessageData HL7 message to convert
     * @return JSON representation of FHIR {@link Bundle} resource. If bundle type if not specified
     *         then the default bundle type is used BundleType.COLLECTION
     * @throws UnsupportedOperationException - if message type is not supported
     */

    public String convert(String hl7MessageData) {
        return convert(hl7MessageData, ConverterOptions.SIMPLE_OPTIONS);

    }

    /**
     * Converts the input HL7 message (String data) into FHIR bundle resource.
     * 
     * @param hl7MessageData Message to convert
     * @param options Options for conversion
     * 
     * @return JSON representation of FHIR {@link Bundle} resource.
     * @throws UnsupportedOperationException - if message type is not supported
     */
    public String convert(String hl7MessageData, ConverterOptions options) {

        HL7MessageEngine engine = getMessageEngine(options);
        Bundle bundle = convertToBundle(hl7MessageData, options, engine);
        return engine.getFHIRContext().encodeResourceToString(bundle);
    }

    /**
     * Converts the input HL7 message (String data) into FHIR bundle resource.
     *
     * @param hl7MessageData Message to convert
     * @param options Options for conversion
     * @param engine Hl7Message engine
     * @return Bundle {@link Bundle} resource.
     * @throws UnsupportedOperationException - if message type is not supported
     */
    public Bundle convertToBundle(String hl7MessageData, ConverterOptions options, HL7MessageEngine engine) {
        Preconditions.checkArgument(StringUtils.isNotBlank(hl7MessageData),
                "Input HL7 message cannot be blank");
        if(engine == null) {
            engine = getMessageEngine(options);
        }


        Message hl7message = getHl7Message(hl7MessageData);
        if (hl7message != null) {
            String messageType = HL7DataExtractor.getMessageType(hl7message);
            HL7MessageModel hl7MessageTemplateModel = messagetemplates.get(messageType);
            if (hl7MessageTemplateModel != null) {
                return hl7MessageTemplateModel.convert(hl7message, engine);
            } else {
                throw new UnsupportedOperationException("Message type not yet supported " + messageType);
            }
        } else {
            throw new IllegalArgumentException("Parsed HL7 message was null.");
        }
    }

    private HL7MessageEngine getMessageEngine(ConverterOptions options){
        Preconditions.checkArgument(options != null, "options cannot be null.");
        FHIRContext context = new FHIRContext(options.isPrettyPrint(), options.isValidateResource(), options.getProperties(), options.getZoneIdText());

        return new HL7MessageEngine(context, options.getBundleType());
    }

    private static Message getHl7Message(String data) {
        Message hl7message = null;
        try (InputStream ins = IOUtils.toInputStream(data, StandardCharsets.UTF_8)) {
            Hl7InputStreamMessageStringIterator iterator = new Hl7InputStreamMessageStringIterator(ins);
            // only supports single message conversion.
            if (iterator.hasNext()) {

                hl7message = hparser.getParser().parse(iterator.next());
            }
        } catch (HL7Exception e) {
            throw new IllegalArgumentException("Cannot parse the message.", e);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("IOException encountered.", ioe);
        }

        try {
            if (hl7message != null) {
                String messageStructureInfo = hl7message.printStructure();
                StringBuilder output = new StringBuilder();
                String[] messageStructureInfoLines = messageStructureInfo.split(System.getProperty("line.separator"));
                for (String line : messageStructureInfoLines) {
                    if (!line.contains("|")) {
                        output.append(line);
                    } else {
                        int firstDash = line.indexOf("-");
                        // Added fail-safe check if the content after "-" is less than 5 characters
                        int lastContentIndex = Math.min(firstDash + 5, line.length() - 1);
                        output.append(line.substring(0, lastContentIndex));
                    }
                    output.append("\n");
                }
                if (output.length() > 0) {
                    LOGGER.info("HL7_MESSAGE_STRUCTURE=\n{}", output);
                }
            }
        } catch (HL7Exception e) {
            throw new IllegalArgumentException("Error printing message structure.", e);
        }
        return hl7message;
    }

    private static void close(HL7HapiParser hparser) {
        if (hparser != null) {
            try {
                hparser.getContext().close();
            } catch (IOException e) {
                throw new IllegalStateException("Failure to close HL7 parser.", e);
            }
        }
    }
}
