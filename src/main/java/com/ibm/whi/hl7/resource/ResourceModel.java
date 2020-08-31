package com.ibm.whi.hl7.resource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.whi.hl7.expression.DefaultExpression;
import com.ibm.whi.hl7.expression.Expression;
import com.ibm.whi.hl7.expression.Hl7Expression;
import com.ibm.whi.hl7.expression.ReferenceExpression;
import com.ibm.whi.hl7.expression.ResourceReferenceExpression;
import com.ibm.whi.hl7.expression.ValueReplacementExpression;


@JsonDeserialize(using = ResourceDeserializer.class)
public class ResourceModel {
  private static final File RESOURCES = new File("src/main/resources");
  private Map<String, Expression> expressions;
  private String hl7spec;
  private int order;

  public ResourceModel(Map<String, Expression> expressions, String hl7spec) {
    this(expressions, hl7spec, 0);


  }

  public ResourceModel(Map<String, Expression> expressions, String hl7spec, int order) {
    this.expressions = new HashMap<>();
    this.expressions.putAll(expressions);
    this.hl7spec = hl7spec;
    this.order = order;

  }



  public ResourceModel(Map<String, Expression> expressions) {
    this(expressions, null);
  }

  public Map<String, Expression> getExpressions() {
    return expressions;
  }



  public Object evaluate(Map<String, Object> context) {
    Map<String, Object> localContext = new HashMap<>(context);
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
        .filter(e -> (e.getValue() instanceof ResourceReferenceExpression))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> valueref = expressionMap.entrySet().stream()
        .filter(e -> (e.getValue() instanceof ValueReplacementExpression))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



    for (Entry<String, Expression> entry : refExp.entrySet()) {

      ReferenceExpression exp = (ReferenceExpression) entry.getValue();
      exp.getReferencesResources().forEach(ref -> localContext.put(ref, resolveValues.get(ref)));

      if (exp.getData() != null) {

        Object obj = exp.execute(localContext);
        if (obj != null) {

          resolveValues.put(entry.getKey(), obj);
        }
      }
    }

    executeExpression(localContext, resolveValues, hl7Exps);
    executeExpression(localContext, resolveValues, resourceRef);

    executeExpression(localContext, resolveValues, defaultExp);

    executeExpression(localContext, resolveValues, valueref);


    resolveValues.values().removeIf(Objects::isNull);
    if (resolveValues.isEmpty()) {
      return null;
    } else {
      return resolveValues;
    }

  }

  private static void executeExpression(Map<String, Object> localContext,
      Map<String, Object> resolveValues, Map<String, Expression> hl7Exps) {
    for (Entry<String, Expression> entry : hl7Exps.entrySet()) {
      Expression exp = entry.getValue();

      Object obj = exp.execute(localContext);


      if (obj != null) {

        resolveValues.put(entry.getKey(), obj);
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
        return ObjectMapperUtil.getInstance().readValue(templateFile, ResourceModel.class);

      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Error encountered in processing the template" + templateFile, e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + templateFile);
    }

  }




}
