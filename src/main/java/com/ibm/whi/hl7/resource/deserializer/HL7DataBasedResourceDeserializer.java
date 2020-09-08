package com.ibm.whi.hl7.resource.deserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.hl7.expression.SimpleExpression;
import com.ibm.whi.hl7.expression.Hl7Expression;
import com.ibm.whi.hl7.expression.JELXExpression;
import com.ibm.whi.hl7.expression.ReferenceExpression;
import com.ibm.whi.hl7.expression.ValueExtractionGeneralExpression;
import com.ibm.whi.hl7.resource.HL7DataBasedResourceModel;
import com.ibm.whi.hl7.resource.ObjectMapperUtil;




public class HL7DataBasedResourceDeserializer extends JsonDeserializer<HL7DataBasedResourceModel> {

  private static final ObjectMapper MAPPER = ObjectMapperUtil.getYAMLInstance();

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceDeserializer.class);
 


  @Override
  public HL7DataBasedResourceModel deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    JsonNode hl7PrefixNode = node.get(TemplateFieldNames.HL7_SPEC);
    String hl7Prefix = null;
    if (hl7PrefixNode != null) {
      hl7Prefix = hl7PrefixNode.toString();
    }
    Map<String, Expression> expressions = new HashMap<>();
    
    Iterator<Entry<String, JsonNode>> iter= node.fields();

    while(iter.hasNext()) {
      Entry<String, JsonNode> entry=iter.next();

      Expression e;
      LOGGER.info("deserealizing {}", entry);
      if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.REFERENCE)) {

        e = MAPPER.convertValue(entry.getValue(), ReferenceExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.FETCH)) {
        e = MAPPER.convertValue(entry.getValue(), ValueExtractionGeneralExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.EVALUATE)) {
        e = MAPPER.convertValue(entry.getValue(), JELXExpression.class);

      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.HL7_SPEC)) {
        e = MAPPER.convertValue(entry.getValue(), Hl7Expression.class);
      } else {
        e = MAPPER.convertValue(entry.getValue(), SimpleExpression.class);
      }
      if (e != null) {
        expressions.put(entry.getKey(), e);
      }

      LOGGER.info("deserealized {} expression type {}", entry, e.getClass());
      
    }
    JsonNode namenode = node.get("resourceType");
    String name = "unknown";
    if (namenode != null) {
      name = namenode.textValue();
    }
    return new HL7DataBasedResourceModel(name, expressions, hl7Prefix);
    }

}
