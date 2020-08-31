package com.ibm.whi.hl7.resource;

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
import com.ibm.whi.hl7.expression.DefaultExpression;
import com.ibm.whi.hl7.expression.Expression;
import com.ibm.whi.hl7.expression.Hl7Expression;
import com.ibm.whi.hl7.expression.ReferenceExpression;
import com.ibm.whi.hl7.expression.ResourceReferenceExpression;
import com.ibm.whi.hl7.expression.ValueReplacementExpression;



public class ResourceDeserializer extends JsonDeserializer<ResourceModel> {

  private static final ObjectMapper MAPPER = ObjectMapperUtil.getInstance();

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDeserializer.class);
 


  @Override
    public ResourceModel deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
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
        e = MAPPER.convertValue(entry.getValue(), ResourceReferenceExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.EVALUATE)) {
        e = MAPPER.convertValue(entry.getValue(), ValueReplacementExpression.class);

      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.HL7_SPEC)) {
        e = MAPPER.convertValue(entry.getValue(), Hl7Expression.class);
      } else {
        e = MAPPER.convertValue(entry.getValue(), DefaultExpression.class);
      }
      if (e != null) {
        expressions.put(entry.getKey(), e);
      }

      LOGGER.info("deserealized {} expression type {}", entry, e.getClass());
      
    }
    return new ResourceModel(expressions, hl7Prefix);
    }

}
