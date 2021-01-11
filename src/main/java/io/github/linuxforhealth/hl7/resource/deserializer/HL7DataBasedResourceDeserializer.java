/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource.deserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.expression.ExpressionAttributes;
import io.github.linuxforhealth.hl7.resource.HL7DataBasedResourceModel;



public class HL7DataBasedResourceDeserializer extends JsonDeserializer<HL7DataBasedResourceModel> {

  private static final String RESOURCE_TYPE_FIELD_NAME = "resourceType";

  private static final ObjectMapper MAPPER = ObjectMapperUtil.getYAMLInstance();

  private static final Logger LOGGER =
      LoggerFactory.getLogger(HL7DataBasedResourceDeserializer.class);



  @Override
  public HL7DataBasedResourceModel deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {
    ObjectNode node = jsonParser.getCodec().readTree(jsonParser);
    JsonNode hl7PrefixNode = node.get(TemplateFieldNames.SPEC);
    String hl7Prefix = null;
    if (hl7PrefixNode != null) {
      hl7Prefix = hl7PrefixNode.toString();
    }
    Map<String, Expression> expressions = new HashMap<>();


    Iterator<Entry<String, JsonNode>> iter = node.fields();


    while (iter.hasNext()) {

      Entry<String, JsonNode> entry = iter.next();

      Expression e;
      LOGGER.info("deserealizing {}", entry);
      ExpressionAttributes expAttr =
          MAPPER.convertValue(entry.getValue(), ExpressionAttributes.class);

      if (expAttr != null && expAttr.getExpressionType() != null) {

        try {
          Constructor<?> ctor =
              expAttr.getExpressionType().getEvaluator().getConstructor(ExpressionAttributes.class);
          e = (Expression) ctor.newInstance(expAttr);

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException | SecurityException e1) {
          LOGGER.error("deserialization failure {} expression type {}", entry,
              expAttr.getExpressionType(), e1);
          e = null;
        }
        if (e != null) {
          expressions.put(entry.getKey(), e);
        }

        LOGGER.info("deserialized {} expression type {}", entry, e);

      }

    }
    JsonNode namenode = node.get(RESOURCE_TYPE_FIELD_NAME);
    String name = "unknown";
    if (namenode != null) {
      name = namenode.textValue();
    }
    return new HL7DataBasedResourceModel(name, expressions, hl7Prefix);
  }

}
