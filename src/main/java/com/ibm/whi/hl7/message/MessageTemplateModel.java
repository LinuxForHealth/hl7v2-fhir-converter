package com.ibm.whi.hl7.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.python.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Unmodifiable;

public class MessageTemplateModel {

  private List<FHIRResource> resources;
  private String messageName;


  public MessageTemplateModel(@JsonProperty("messageName") String messageName,
      @JsonProperty("resources") List<FHIRResource> resources) {

    this.messageName = messageName;
    this.resources = resources;

  }


  public String convert(String message) throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(message),"Input Hl7 message cannot be blank");
    HL7HapiParser hparser = null;
    try { 
    hparser = new HL7HapiParser();

    Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(message));
      return convert(hl7message);


    } catch (HL7Exception e) {
      throw new IllegalArgumentException("Cannot parse the message.", e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
  }

  }



  public String convert(Message message) throws IOException {
    Preconditions.checkArgument(message != null, "Input Hl7 message cannot be blank");

    Hl7DataExtractor hl7DTE = new Hl7DataExtractor(message);
    Map<String, Object> executable = new HashMap<>();
    executable.put("hde", hl7DTE);
    Bundle bundle =
        MessageUtil.convertMessageToFHIRResource(hl7DTE, resources, executable, new HashMap<>());

    return FHIRContext.getIParserInstance().setPrettyPrint(true).encodeResourceToString(bundle);



  }


  public String getMessageName() {
    return messageName;
  }

  public void setMessageName(String messageName) {
    this.messageName = messageName;
  }



}
