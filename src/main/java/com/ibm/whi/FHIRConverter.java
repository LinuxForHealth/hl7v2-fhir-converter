package com.ibm.whi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.python.google.common.base.Preconditions;
import com.ibm.whi.hl7.message.MessageTemplateModel;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import com.ibm.whi.hl7.resource.ObjectMapperUtil;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Unmodifiable;

public class FHIRConverter {
  private Map<String, MessageTemplateModel> messagetemplates = new HashMap<>();

  public FHIRConverter() throws IOException {
    initTemplates();
  }


  private void initTemplates() throws IOException {
    try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/message"))) {



      paths.filter(Files::isRegularFile).forEach(f -> addMessageModel(f));
    }
  }


  public String convert(String rawmessage) throws IOException {
    Preconditions.checkArgument(StringUtils.isNotBlank(rawmessage),
        "Input Hl7 message cannot be blank");
   
    HL7HapiParser hparser = null;
    try { 
    hparser = new HL7HapiParser();

      Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(rawmessage));
      String messageType = Hl7DataExtractor.getMessageType(hl7message);
      MessageTemplateModel hl7MessageTemplateModel = messagetemplates.get(messageType);
      return hl7MessageTemplateModel.convert(hl7message);


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
        MessageTemplateModel rm = ObjectMapperUtil.getInstance().readValue(templateFile, MessageTemplateModel.class);
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
