/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationParser;
import io.github.linuxforhealth.hl7.expression.variable.VariableGenerator;

@JsonDeserialize(builder = ExpressionAttributes.Builder.class)
public class ExpressionAttributes {
  private static final String OBJECT_TYPE = Object.class.getSimpleName();

  // Basic properties of an expression
  private String name;
  private final String type;
  private final String defaultValue;
  private final boolean isRequired;
  private final List<Specification> specs;
  private final List<Variable> variables;
  private final Condition condition; // filter is applies to the specs, spec values that pass the
                               // condition
  // are used for evaluating the expression.
  private final Map<String, String> constants;
  private final String value;
  private final String valueOf;
  private final boolean useGroup;
  private ExpressionType expressionType;
  private String toString;
  private final List<ExpressionAttributes> expressions;
  private final Map<String, ExpressionAttributes> expressionsMap;
  private final boolean isEvaluateLater;


  // if valueof attribute ends with * then list of values will be generated
  private boolean generateMultiple;


  private ExpressionAttributes(Builder exBuilder) {
    if (exBuilder.type == null) {
      this.type = OBJECT_TYPE;
    } else {
      this.type = exBuilder.type;
    }

    this.defaultValue = exBuilder.defaultValue;
    this.name = exBuilder.name;

    this.isRequired = exBuilder.isRequired;
    this.isEvaluateLater = exBuilder.isEvaluateLater;
   
    this.generateMultiple = exBuilder.generateList;


    this.specs = getSpecList(exBuilder.rawSpecs, exBuilder.useGroup, this.generateMultiple);
    if (StringUtils.isNotBlank(exBuilder.rawCondition)) {
      this.condition = ConditionUtil.createCondition(exBuilder.rawCondition, exBuilder.useGroup);
    } else {
      this.condition = null;
    }

    this.constants = new HashMap<>();
    if (exBuilder.constants != null && !exBuilder.constants.isEmpty()) {
      this.constants.putAll(exBuilder.constants);
    }

    this.variables = new ArrayList<>();
    if (exBuilder.rawVariables != null) {
      for (Entry<String, String> e : exBuilder.rawVariables.entrySet()) {
        this.variables.add(VariableGenerator.parse(e.getKey(), e.getValue()));
      }
    }

    this.value = exBuilder.value;
    this.valueOf = exBuilder.valueOf;


    this.expressionType = exBuilder.expressionType;
    this.useGroup = exBuilder.useGroup;

    if (this.expressionType == null && CollectionUtils.isNotEmpty(this.specs)) {
      this.expressionType = ExpressionType.HL7SPEC;
    }

    if (exBuilder.expressions != null) {
      this.expressions = exBuilder.expressions;
    } else {
      this.expressions = null;
    }

    if (exBuilder.expressionsMap != null) {
      this.expressionsMap = exBuilder.expressionsMap;
    } else {
      this.expressionsMap = null;
    }

  }

  public Map<String, ExpressionAttributes> getExpressionsMap() {
    return expressionsMap;
  }

  public boolean isUseGroup() {
    return useGroup;
  }

  public String getType() {
    return type;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public List<Specification> getSpecs() {
    return ImmutableList.copyOf(specs);
  }

  public List<Variable> getVariables() {
    return ImmutableList.copyOf(variables);
  }

  public Condition getFilter() {
    return condition;
  }

  public Map<String, String> getConstants() {
    return ImmutableMap.copyOf(constants);
  }

  public boolean isGenerateMultiple() {
    return this.generateMultiple;
  }

  public String getValue() {
    return this.value;
  }


  public ExpressionType getExpressionType() {
    return expressionType;
  }

  public String getValueOf() {
    return valueOf;
  }



  public String getName() {
    return name;
  }



  public List<ExpressionAttributes> getExpressions() {
    return expressions;
  }


  /**
   * Extract special chars: * indicates to extract fields from multiple entries & indicates to
   * retain empty (null) fields
   * 
   * @param inputString
   * @param generateMultiple
   * @return ExpressionModifiers object with booleans indicating which modifiers were used and the
   *         expression after modifiers have been removed
   */
  public static final ExpressionModifiers extractExpressionModifiers(String inputString,
      boolean generateMultiple) {

    boolean extractMultiple = generateMultiple;
    boolean retainEmpty = false;
    String expression = inputString;

    if (StringUtils.endsWith(expression, "*") && !StringUtils.endsWith(expression, ".*")) {
      expression = StringUtils.removeEnd(expression, "*");
      extractMultiple = true;
    }
    if (StringUtils.endsWith(expression, "&")) {
      expression = StringUtils.removeEnd(expression, "&");
      retainEmpty = true;
    }
    // Repeat check for asterisk to allow for different order of special chars
    if (StringUtils.endsWith(expression, "*") && !StringUtils.endsWith(expression, ".*")) {
      expression = StringUtils.removeEnd(expression, "*");
      extractMultiple = true;
    }
    expression = StringUtils.strip(expression);

    return new ExpressionModifiers(extractMultiple, retainEmpty, expression);
  }

  public static List<Specification> getSpecList(String inputString, boolean useGroup,
      boolean generateMultiple) {

    ExpressionModifiers exp = extractExpressionModifiers(inputString, generateMultiple);

    List<Specification> specs = new ArrayList<>();
    if (StringUtils.isNotBlank(exp.expression)) {
      StringTokenizer st = new StringTokenizer(exp.expression, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      st.getTokenList().forEach(s -> specs
          .add(SpecificationParser.parse(s, exp.extractMultiple, useGroup, exp.retainEmpty)));
    }

    return specs;
  }

  @Override
  public String toString() {
    if (this.toString == null) {
      this.toString = ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE,
          false, false, true, null);
    }
    return this.toString;
  }



  public boolean isEvaluateLater() {
    return isEvaluateLater;
  }




  public static class Builder {



    private String name;
    private String type;
    private String defaultValue;
    private boolean isRequired;
    private String rawSpecs;
    private String rawCondition;
    private Map<String, String> rawVariables;
    private Map<String, String> constants;
    private boolean useGroup;
    private ExpressionType expressionType;

    private String valueOf;
    private String value;
    private boolean generateList;
    private List<ExpressionAttributes> expressions;

    private Map<String, ExpressionAttributes> expressionsMap;
    private boolean isEvaluateLater;
  


    public Builder() {}

    public Builder(String singleValue) {
      value = singleValue;
      this.expressionType = ExpressionType.SIMPLE;
    }

    public boolean isUseGroup() {
      return useGroup;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withDefault(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder withRequired(boolean isRequired) {
      this.isRequired = isRequired;
      return this;
    }

    public Builder withEvaluateLater(boolean isEvaluateLater) {
      this.isEvaluateLater = isEvaluateLater;
      return this;
    }



    public Builder withSpecs(String rawSpecs) {
      this.rawSpecs = rawSpecs;
      return this;
    }

    public Builder withUseGroup(boolean useGroup) {
      this.useGroup = useGroup;
      return this;
    }

    public Builder withCondition(String rawCondition) {
      this.rawCondition = rawCondition;
      return this;
    }

    public Builder withVars(Map<String, String> rawVariables) {
      this.rawVariables = rawVariables;
      return this;
    }

    public Builder withConstants(Map<String, String> constants) {
      this.constants = constants;
      return this;
    }

    public Builder withExpressions(List<ExpressionAttributes> expressions) {
      this.expressions = expressions;
      return this;
    }

    public Builder withExpressionsMap(Map<String, ExpressionAttributes> expressionsMap) {
      this.expressionsMap = expressionsMap;
      return this;
    }


    public Builder withValueOf(String valueOf) {
      this.valueOf = StringUtils.trim(valueOf);
      if (this.expressionType == null) {
        this.expressionType = ExpressionType.SIMPLE;
      }
      return this;
    }

    public Builder withExpressionType(String expressionType) {
      this.expressionType = EnumUtils.getEnumIgnoreCase(ExpressionType.class, expressionType);
      return this;
    }

    public Builder withValue(String value) {
      this.value = value;
      this.expressionType = ExpressionType.SIMPLE;
      return this;
    }

    public Builder withGenerateList(boolean generateList) {
      this.generateList = generateList;
      return this;
    }

    public ExpressionAttributes build() {
      return new ExpressionAttributes(this);
    }

  }

  // Class used when extracting modifiers from the expression, contains the expression after
  // modifiers have been removed and
  // booleans indicating which modifiers were in the expression.
  public static class ExpressionModifiers {
    private boolean extractMultiple = false; // true when * is used in the expression
    private boolean retainEmpty = false; // true when & is used in the expression
    private String expression = ""; // resulting expression after the modifiers have been removed

    ExpressionModifiers(boolean theExtractMultiple, boolean theRetainEmpty, String theExpression) {
      extractMultiple = theExtractMultiple;
      retainEmpty = theRetainEmpty;
      expression = theExpression;
    }
    
    public boolean getExtractMultiple() {
        return extractMultiple;
    }
    
    public boolean getRetainEmpty() {
        return retainEmpty;
    }
    
    public String getExpression() {
        return expression;
    }
  }

  public void setName(String key) {
    this.name = key;

  }
}