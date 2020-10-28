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

public class HL7ToFHIRConverter {
  private Map<String, HL7MessageModel> messagetemplates = new HashMap<>();
  private HL7MessageEngine engine;

  public HL7ToFHIRConverter(boolean isPrettyPrint, BundleType bundleType) throws IOException {

    FHIRContext context = new FHIRContext(isPrettyPrint);
    engine = new HL7MessageEngine(context, bundleType);

    messagetemplates.putAll(ResourceReader.getInstance().getMessageTemplates());
  }

  public HL7ToFHIRConverter() throws IOException {
    this(Constants.DEFAULT_PRETTY_PRINT, Constants.DEFAULT_BUNDLE_TYPE);
  }


  /**
   * 
   * @param rawmessage
   * @return FHIR {@link Bundle} resource in json format
   * @throws IOException
   * @throws UnsupportedOperationException - if message type is not supported
   * @throws IllegalArgumentException
   */
  public String convert(String rawmessage) throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(rawmessage),
        "Input HL7 message cannot be blank");

    HL7HapiParser hparser = null;
    Message hl7message = null;
    try {
      hparser = new HL7HapiParser();
      hl7message = hparser.getParser().parse(rawmessage);

    } catch (HL7Exception e) {
      throw new IllegalArgumentException("Cannot parse the message.", e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }
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



}
