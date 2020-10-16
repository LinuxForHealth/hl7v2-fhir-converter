/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.hl7.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.linuxforhealth.api.FHIRResourceTemplate;
import com.ibm.linuxforhealth.api.MessageEngine;
import com.ibm.linuxforhealth.api.MessageTemplate;
import com.ibm.linuxforhealth.hl7.parsing.HL7DataExtractor;
import com.ibm.linuxforhealth.hl7.parsing.HL7HapiParser;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

public class HL7MessageModel implements MessageTemplate<Message> {

  private List<FHIRResourceTemplate> resources;
  private String messageName;


  @JsonCreator
  public HL7MessageModel(@JsonProperty("messageName") String messageName,
      @JsonProperty("resources") List<HL7FHIRResourceTemplate> resources) {
    this.messageName = messageName;
    this.resources = new ArrayList<>();
    if (resources != null && !resources.isEmpty()) {
      this.resources.addAll(resources);
    }

  }



  public String convert(String message, MessageEngine engine) throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(message),
        "Input Hl7 message cannot be blank");
    HL7HapiParser hparser = null;
    try {
      hparser = new HL7HapiParser();

      Message hl7message = hparser.getParser().parse(message);
      return convert(hl7message, engine);


    } catch (HL7Exception e) {
      throw new IllegalArgumentException("Cannot parse the message.", e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }

  }


  @Override
  public String convert(Message message, MessageEngine engine) {
    Preconditions.checkArgument(message != null, "Input Hl7 message cannot be null");
    Preconditions.checkArgument(engine != null, "MessageEngine cannot be null");

    HL7DataExtractor hl7DTE = new HL7DataExtractor(message);
    HL7MessageData dataSource = new HL7MessageData(hl7DTE);

    Bundle bundle =
        engine.transform(dataSource, this.getResources(), new HashMap<>());
    return engine.getFHIRContext()
        .encodeResourceToString(bundle);

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
