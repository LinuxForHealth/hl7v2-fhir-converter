/*
 * (C) Copyright IBM Corp. 2020
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
import org.hl7.fhir.r4.model.Bundle.BundleType;
import com.google.common.base.Preconditions;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
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
   * @param hl7Message - Single message only
   * @return FHIR {@link Bundle} resource in json format
   * @throws IOException
   * @throws UnsupportedOperationException - if message type is not supported
   * @throws IllegalArgumentException
   */
  public String convert(String hl7Message) throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(hl7Message),
        "Input HL7 message cannot be blank");

    Message hl7message = getHl7Message(hl7Message);
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


  /**
   * 
   * @param hl7MessageFile - Single message only
   * @return FHIR {@link Bundle} resource in json format
   * @throws IOException
   * @throws UnsupportedOperationException - if message type is not supported
   * @throws IllegalArgumentException
   */
  public String convert(File hl7MessageFile) throws IOException {
    Preconditions.checkArgument(hl7MessageFile != null, "Input HL7 message file cannot be null.");
    return convert(FileUtils.readFileToString(hl7MessageFile, StandardCharsets.UTF_8));

  }

  private static Message getHl7Message(String data) throws IOException {

    HL7HapiParser hparser = null;
    Message hl7message = null;
    try (InputStream ins = IOUtils.toInputStream(data, StandardCharsets.UTF_8)) {
      Hl7InputStreamMessageStringIterator iterator =
          new Hl7InputStreamMessageStringIterator(ins);
      // only supports single message conversion.
      if (iterator.hasNext()) {
        hparser = new HL7HapiParser();
        hl7message = hparser.getParser().parse(iterator.next());
      }
    } catch (HL7Exception e) {
      throw new IllegalArgumentException("Cannot parse the message.", e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }

    return hl7message;
  }

}
