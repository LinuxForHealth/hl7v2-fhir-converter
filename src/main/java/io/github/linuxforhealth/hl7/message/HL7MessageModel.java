/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import io.github.linuxforhealth.api.FHIRResourceTemplate;
import io.github.linuxforhealth.api.MessageEngine;
import io.github.linuxforhealth.api.MessageTemplate;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageModel implements MessageTemplate<Message> {

    private List<FHIRResourceTemplate> resources;
    private String messageName;
    private static final Logger LOGGER = LoggerFactory.getLogger(HL7MessageModel.class);

    @JsonCreator
    public HL7MessageModel(@JsonProperty("messageName") String messageName,
                           @JsonProperty("resources") List<HL7FHIRResourceTemplate> resources) {
        this.messageName = messageName;
        this.resources = new ArrayList<>();
        if (resources != null && !resources.isEmpty()) {
            this.resources.addAll(resources);
        }

    }

    private void handleException(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        StringBuilder classAndStack = new StringBuilder();
        classAndStack.append(e.getClass() + "\n");
        for (int i = 0; i < stackTrace.length; i++) {
            classAndStack.append(stackTrace[i] + "\n");
        }
        LOGGER.error("Error transforming HL7 message. {}", classAndStack);
    }


    public String convert(String message, MessageEngine engine) throws IOException {
        Preconditions.checkArgument(StringUtils.isNotBlank(message),
                "Input Hl7 message cannot be blank");
        HL7HapiParser hparser = null;
        try {
            hparser = new HL7HapiParser();

            Message hl7message = hparser.getParser().parse(message);
            Bundle bundle = convert(hl7message, engine);
            return engine.getFHIRContext().encodeResourceToString(bundle);

        } catch (HL7Exception e) {
            throw new IllegalArgumentException("Cannot parse the message.", e);
        } finally {
            if (hparser != null) {
                hparser.getContext().close();
            }
        }

    }


    @Override
    public Bundle convert(Message message, MessageEngine engine) {
        Preconditions.checkArgument(message != null, "Input Hl7 message cannot be null");
        Preconditions.checkArgument(engine != null, "MessageEngine cannot be null");

        HL7DataExtractor hl7DTE = new HL7DataExtractor(message);
        HL7MessageData dataSource = new HL7MessageData(hl7DTE);

        Bundle bundle = null;

        // Catch any exceptions and log them without the message.
        // NOTE: We have seen PHI in these exception messages.
        try {
            bundle = engine.transform(dataSource, this.getResources(), new HashMap<>());
            engine.getFHIRContext().validate(bundle);

        } catch (Exception e) {
            // Print stack class and trace without the error message.
            handleException(e);
        }

        return bundle;

    }


    @Override
    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    @Override
    public List<FHIRResourceTemplate> getResources() {
        return new ArrayList<>(resources);
    }


}
