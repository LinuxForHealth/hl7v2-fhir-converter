/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import com.google.common.base.Preconditions;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.message.HL7MessageEngine;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Converts HL7 message to FHIR bundle resource based on the customizable templates.
 * 
 *
 * @author pbhallam
 */
public class HL7ToFHIRConverter {
  private Map<String, HL7MessageModel> messagetemplates = new HashMap<>();

  /**
   * Constructor initialized all the templates used for converting the HL7 to FHIR bundle resource.
   * 
   * @throws IllegalStateException - If any issues are encountered when loading the templates.
   */
  public HL7ToFHIRConverter() {

    try {
      messagetemplates.putAll(ResourceReader.getInstance().getMessageTemplates());
    } catch (IOException | IllegalArgumentException e) {
      throw new IllegalStateException("Failure to initialize the templated for the converter.", e);
    }
  }

  /**
   * Converts the input HL7 message (String data) into FHIR bundle resource.
   * 
   * @param hl7MessageData
   * @return JSON representation of FHIR {@link Bundle} resource. If bundle type if not specified
   *         then the default bundle type is used BundleType.COLLECTION
   * 
   */

  public String convert(String hl7MessageData) {
    return convert(hl7MessageData, Constants.DEFAULT_PRETTY_PRINT, Constants.DEFAULT_BUNDLE_TYPE);

  }

  /**
   * Converts the input HL7 message (String data) into FHIR bundle resource.
   * 
   * @param hl7MessageData
   * @param isPrettyPrint
   * @param bundleType
   * @return JSON representation of FHIR {@link Bundle} resource.
   */
  public String convert(String hl7MessageData, boolean isPrettyPrint, BundleType bundleType) {
    Preconditions.checkArgument(StringUtils.isNotBlank(hl7MessageData),
        "Input HL7 message cannot be blank");
    Preconditions.checkArgument(bundleType != null, "bundleType cannot be null.");
    FHIRContext context = new FHIRContext(isPrettyPrint);
    HL7MessageEngine engine = new HL7MessageEngine(context, bundleType);

    Message hl7message = getMessage(hl7MessageData);
    String messageType = HL7DataExtractor.getMessageType(hl7message);
    HL7MessageModel hl7MessageTemplateModel = messagetemplates.get(messageType);
    if (hl7MessageTemplateModel != null) {
      return hl7MessageTemplateModel.convert(hl7message, engine);
    } else {
      throw new IllegalArgumentException("Message type not yet supported " + messageType);
    }
  }



  private static Message getMessage(String message) {
    HL7HapiParser hparser = null;

    try {
      hparser = new HL7HapiParser();
      return hparser.getParser().parse(message);
    } catch (HL7Exception e) {
      throw new IllegalArgumentException(e);
    } finally {
      close(hparser);
    }

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
