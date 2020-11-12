/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import com.google.common.base.Preconditions;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.message.HL7MessageEngine;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;

public class HL7ToFHIRConverter {
  private Map<String, HL7MessageModel> messagetemplates = new HashMap<>();
  private HL7MessageEngine engine;

  public HL7ToFHIRConverter(boolean isPrettyPrint, BundleType bundleType) throws IOException,
          URISyntaxException{
    initTemplates();
    FHIRContext context = new FHIRContext(isPrettyPrint);
    engine = new HL7MessageEngine(context, bundleType);
  }

  public HL7ToFHIRConverter() throws IOException, URISyntaxException{
    this(Constants.DEFAULT_PRETTY_PRINT, Constants.DEFAULT_BUNDLE_TYPE);
  }



  private void initTemplates() throws IOException, URISyntaxException {
    URL url = Thread
            .currentThread()
            .getContextClassLoader()
            .getResource(Constants.DEFAULT_HL7_MESSAGE_FOLDER);

   Path templatePath = Paths.get(url.toURI());
   try (Stream<Path> paths = Files.walk(templatePath)) {
     paths.filter(p -> p.toFile().isFile()).forEach(this::addMessageModel);
   }
  }


  public String convert(String rawmessage)
      throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(rawmessage),
        "Input HL7 message cannot be blank");
   
    HL7HapiParser hparser = null;
    try { 
    hparser = new HL7HapiParser();

      Message hl7message = hparser.getParser().parse(rawmessage);
      String messageType = HL7DataExtractor.getMessageType(hl7message);
      HL7MessageModel hl7MessageTemplateModel = messagetemplates.get(messageType);
      return hl7MessageTemplateModel.convert(hl7message, engine);


    } catch (HL7Exception e) {
      throw new IllegalArgumentException("Cannot parse the message.", e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
  }
  

  }


  public void addMessageModel(Path path) {

    File templateFile = new File(path.toString());
    if (templateFile.exists()) {
      try {

        HL7MessageModel rm =
            ObjectMapperUtil.getYAMLInstance().readValue(templateFile, HL7MessageModel.class);
        rm.setMessageName(templateFile.getName());
        messagetemplates.put(com.google.common.io.Files.getNameWithoutExtension(path.toString()),
            rm);
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Error encountered in processing the template" + templateFile, e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + templateFile);
    }

  }

}
