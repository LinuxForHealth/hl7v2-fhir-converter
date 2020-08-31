package com.ibm.whi.hl7.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.python.google.common.base.Preconditions;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Unmodifiable;

public class Hl7Message {

  private List<FHIRResource> resources;
  private String messageName;

  public Hl7Message(String messageName, List<FHIRResource> resources) {

    this.messageName = messageName;
    this.resources = resources;

  }


  public String convertMessageToFHIRResource(String message) throws HL7Exception, IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(message),"Input Hl7 message cannot be blank");
    HL7HapiParser hparser = null;
    try { 
    hparser = new HL7HapiParser();
    Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(message));
    Hl7DataExtractor hl7DTE = new Hl7DataExtractor(hl7message);
    Map<String, Object> context = new HashMap<>();
    context.put("hde", hl7DTE);
      Bundle bundle = MessageUtil.convertMessageToFHIRResource(hl7DTE, resources, context);

      return FHIRContext.getIParserInstance().setPrettyPrint(true).encodeResourceToString(bundle);


    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
  }

  }


}
