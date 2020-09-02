package com.ibm.whi.hl7.resource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.Expression;
import com.ibm.whi.hl7.expression.GenericResult;
import com.ibm.whi.hl7.expression.model.DefaultExpression;
import com.ibm.whi.hl7.expression.model.Hl7Expression;
import com.ibm.whi.hl7.expression.model.JELXExpression;
import com.ibm.whi.hl7.expression.model.ReferenceExpression;
import com.ibm.whi.hl7.expression.model.ValueExtractionGeneralExpression;


@JsonDeserialize(using = ResourceDeserializer.class)
public class ResourceModel {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceModel.class);

  private static final File RESOURCES = new File("src/main/resources");
  private Map<String, Expression> expressions;
  private String hl7spec;
  private int order;
  private String name;

  public ResourceModel(String name, Map<String, Expression> expressions, String hl7spec) {
    this(name, expressions, hl7spec, 0);


  }

  public ResourceModel(String name, Map<String, Expression> expressions, String hl7spec,
      int order) {
    this.expressions = new HashMap<>();
    this.expressions.putAll(expressions);
    this.hl7spec = hl7spec;
    this.order = order;
    this.name = name;

  }



  public ResourceModel(String name, Map<String, Expression> expressions) {
    this(name, expressions, null);
  }

  public Map<String, Expression> getExpressions() {
    return expressions;
  }



  public Object evaluate(ImmutableMap<String, ?> executable,
      ImmutableMap<String, GenericResult> variables) {

    LOGGER.info("Started Evaluating resource {}", this.name);
    Map<String, GenericResult> localContext = new HashMap<>(variables);
    Map<String, Object> resolveValues = new HashMap<>();
    Map<String, Expression> expressionMap = this.getExpressions();


    Map<String, Expression> defaultExp =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof DefaultExpression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> refExp =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof ReferenceExpression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> hl7Exps =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof Hl7Expression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<String, Expression> resourceRef = expressionMap.entrySet().stream()
        .filter(e -> (e.getValue() instanceof ValueExtractionGeneralExpression))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> valueref = expressionMap.entrySet().stream()
        .filter(e -> (e.getValue() instanceof JELXExpression))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



    for (Entry<String, Expression> entry : refExp.entrySet()) {

      ReferenceExpression exp = (ReferenceExpression) entry.getValue();
      LOGGER.info("----Evaluating {} {}", exp.getType(), entry.getKey());
      LOGGER.info("----Extracted reference resource  {} {} reference {}", exp.getType(),
          entry.getKey(), exp.getReference());
      if (exp.getData() != null) {

        GenericResult obj = exp.execute(executable, ImmutableMap.copyOf(localContext));
        LOGGER.info("----Extracted object from reference resource  {} {} reference {}  value {}",
            exp.getType(), entry.getKey(), exp.getReference(), obj);
        if (obj != null) {

          resolveValues.put(entry.getKey(), obj.getValue());
        }
      }
    }

    executeExpression(executable, ImmutableMap.copyOf(localContext), resolveValues, hl7Exps);
    executeExpression(executable, ImmutableMap.copyOf(localContext), resolveValues, resourceRef);

    executeExpression(executable, ImmutableMap.copyOf(localContext), resolveValues, defaultExp);

    executeExpression(executable, ImmutableMap.copyOf(localContext), resolveValues, valueref);


    resolveValues.values().removeIf(Objects::isNull);
    if (resolveValues.isEmpty()) {
      return null;
    } else {
      return resolveValues;
    }

  }

  private static void executeExpression(ImmutableMap<String, ?> executable,
      ImmutableMap<String, GenericResult> localContext,
      Map<String, Object> resolveValues, Map<String, Expression> hl7Exps) {
    for (Entry<String, Expression> entry : hl7Exps.entrySet()) {
      Expression exp = entry.getValue();
      LOGGER.info("Evaluating {} {}", entry.getKey(), entry.getValue());
      GenericResult obj = exp.execute(executable, localContext);
      LOGGER.info("Evaluated {} {} value returned {} ", entry.getKey(), entry.getValue(), obj);

      if (obj != null) {

        resolveValues.put(entry.getKey(), obj.getValue());
      } else if (exp.getDefaultValue() != null) {
        resolveValues.put(entry.getKey(), exp.getDefaultValue());
      }
    }
  }

  public int getOrder() {
    return order;
  }

  public String getHl7spec() {
    return hl7spec;
  }


  public static ResourceModel generateResourceModel(String path) {

    File templateFile = new File(RESOURCES, path + ".yml");

    if (templateFile.exists()) {
      try {
        ResourceModel rm =
            ObjectMapperUtil.getInstance().readValue(templateFile, ResourceModel.class);
        rm.setName(templateFile.getName());
        return rm;
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Error encountered in processing the template" + templateFile, e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + templateFile);
    }

  }

  private void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }



}
