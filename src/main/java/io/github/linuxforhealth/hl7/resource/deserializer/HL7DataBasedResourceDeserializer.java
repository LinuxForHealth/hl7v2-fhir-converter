/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource.deserializer;

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
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.expression.Hl7Expression;
import io.github.linuxforhealth.hl7.expression.JELXExpression;
import io.github.linuxforhealth.hl7.expression.ReferenceExpression;
import io.github.linuxforhealth.hl7.expression.ResourceExpression;
import io.github.linuxforhealth.hl7.expression.SimpleExpression;
import io.github.linuxforhealth.hl7.expression.ValueExtractionGeneralExpression;
import io.github.linuxforhealth.hl7.resource.HL7DataBasedResourceModel;





public class HL7DataBasedResourceDeserializer extends JsonDeserializer<HL7DataBasedResourceModel> {

  private static final ObjectMapper MAPPER = ObjectMapperUtil.getYAMLInstance();

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceDeserializer.class);
 


  @Override
  public HL7DataBasedResourceModel deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    JsonNode hl7PrefixNode = node.get(TemplateFieldNames.SPEC);
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
      if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.RESOURCE)) {

        e = MAPPER.convertValue(entry.getValue(), ResourceExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.REFERENCE)) {

        e = MAPPER.convertValue(entry.getValue(), ReferenceExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.FETCH)) {
        e = MAPPER.convertValue(entry.getValue(), ValueExtractionGeneralExpression.class);
      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.EVALUATE)) {
        e = MAPPER.convertValue(entry.getValue(), JELXExpression.class);

      } else if (entry.getValue() != null && entry.getValue().has(TemplateFieldNames.SPEC)) {
        e = MAPPER.convertValue(entry.getValue(), Hl7Expression.class);
      } else {
        e = MAPPER.convertValue(entry.getValue(), SimpleExpression.class);
      }
      if (e != null) {
        expressions.put(entry.getKey(), e);
      }

      LOGGER.info("deserialized {} expression type {}", entry, e);
      
    }
    JsonNode namenode = node.get("resourceType");
    String name = "unknown";
    if (namenode != null) {
      name = namenode.textValue();
    }
    return new HL7DataBasedResourceModel(name, expressions, hl7Prefix);
    }

}
