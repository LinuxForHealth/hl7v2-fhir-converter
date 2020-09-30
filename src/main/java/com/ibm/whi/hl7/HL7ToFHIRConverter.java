/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.whi.hl7;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.ObjectMapperUtil;
import com.ibm.whi.hl7.message.HL7MessageEngine;
import com.ibm.whi.hl7.message.HL7MessageModel;
import com.ibm.whi.hl7.parsing.HL7DataExtractor;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

public class HL7ToFHIRConverter {
  private Map<String, HL7MessageModel> messagetemplates = new HashMap<>();
  private HL7MessageEngine engine;

  public HL7ToFHIRConverter() throws IOException {
    initTemplates();
    engine = new HL7MessageEngine();
  }




  private void initTemplates() throws IOException {
    try (Stream<Path> paths =
        Files.walk(Paths.get(Constants.DEAFULT_HL7_MESSAGE_FOLDER.getAbsolutePath()))) {
      paths.filter(p -> p.toFile().isFile()).forEach(this::addMessageModel);
    }
  }


  public String convert(String rawmessage) throws IOException {
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
