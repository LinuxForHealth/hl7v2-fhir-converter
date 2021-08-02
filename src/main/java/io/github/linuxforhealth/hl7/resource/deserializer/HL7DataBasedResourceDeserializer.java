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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.expression.ExpressionAttributes;
import io.github.linuxforhealth.hl7.resource.HL7DataBasedResourceModel;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class HL7DataBasedResourceDeserializer extends JsonDeserializer<HL7DataBasedResourceModel> {

  private static final String RESOURCE_TYPE_FIELD_NAME = "resourceType";
  private static final String SPEC = "specs";
  private static Map<String, Expression> commonExpressions;

  private static final ObjectMapper MAPPER = ObjectMapperUtil.getYAMLInstance();
  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceDeserializer.class);

  // Reads the resource/Common.yml and generates expressions from it.
  private static Map<String, Expression> getCommonExpressions() throws JsonProcessingException {

    // only need to create this object once and can reuse it across resoures.
    if (commonExpressions == null) {

      // generate the common expressions from the Common YAML file.
      commonExpressions = new HashMap<>();
      String path = ResourceReader.getInstance().getResource(Constants.HL7_BASE_PATH + Constants.COMMON_RESOURCE_PATH);
      JsonNode node = ObjectMapperUtil.getYAMLInstance().readTree(path);
      Map<String, Expression> expressions = generateExpressions(node);

      commonExpressions.putAll(expressions);
    }

    return commonExpressions;
  }

  @Override
  public HL7DataBasedResourceModel deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {

    ObjectNode node = jsonParser.getCodec().readTree(jsonParser);
    JsonNode hl7PrefixNode = node.get(SPEC);
    String hl7Prefix = null;

    if (hl7PrefixNode != null) {
      hl7Prefix = hl7PrefixNode.toString();
    }

    // generate expressions from the resource YAML
    Map<String, Expression> expressions = generateExpressions(node);

    JsonNode namenode = node.get(RESOURCE_TYPE_FIELD_NAME);
    String name = String.valueOf(ctxt.findInjectableValue("resourceName", null, null));

    if (namenode != null) {
      name = namenode.textValue();
    }

    // Add the common expresions to the list of expressions if this resources has expressions
    // And is base object not a datatype/? or reference/? etc.
    if (!expressions.isEmpty() && name.indexOf("/") == -1) {
      LOGGER.debug("Adding common expressions to the list of expressions for {}",name);
      expressions.putAll(getCommonExpressions());
    }


    return new HL7DataBasedResourceModel(name, expressions, hl7Prefix);
  }

  private static Map<String, Expression> generateExpressions(JsonNode node) {

    Map<String, Expression> expressions = new HashMap<>();
    Iterator<Entry<String, JsonNode>> iter = node.fields();

    while (iter.hasNext()) {

      Entry<String, JsonNode> entry = iter.next();
      Expression e;

      LOGGER.debug("deserealizing {}", entry);
      ExpressionAttributes expAttr = MAPPER.convertValue(entry.getValue(), ExpressionAttributes.class);

      if (expAttr != null && expAttr.getExpressionType() != null) {
        expAttr.setName(entry.getKey());

        try {
          e = generateExpression(expAttr);
        } catch (IllegalStateException e1) {
          LOGGER.error("deserialization failure {} expression type {}", entry, expAttr.getExpressionType(), e1);
          e = null;
        }

        if (e != null) {
          expressions.put(entry.getKey(), e);
        }
        LOGGER.debug("deserialized {} expression type {}", entry, e);
      }
    }

    return expressions;
  }

  public static Expression generateExpression(ExpressionAttributes expAttr) {

    if (expAttr != null && expAttr.getExpressionType() != null) {

      try {
        Constructor<?> ctor = expAttr.getExpressionType().getEvaluator().getConstructor(ExpressionAttributes.class);
        return (Expression) ctor.newInstance(expAttr);
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | SecurityException e1) {
        throw new IllegalStateException("Error encountered while creating expression object.", e1);
      }

    }
    return null;
  }

}
