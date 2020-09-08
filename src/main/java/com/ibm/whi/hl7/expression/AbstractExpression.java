package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.message.InputData;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;

public abstract class AbstractExpression implements Expression {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpression.class);
  public static final String OBJECT_TYPE = Object.class.getSimpleName();
  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private final String type;
  private final GenericResult defaultValue;
  private final boolean required;
  private final List<String> hl7specs;
  private List<Variable> variables;
  private Condition condition;
  private boolean isMultiple;

  public AbstractExpression(String type, Object defaultValue, boolean required, String hl7spec,
      Map<String, String> rawvariables, String condition) {
    if (type == null) {
      this.type = OBJECT_TYPE;
    } else {
      this.type = type;
    }
    if (defaultValue != null) {
      this.defaultValue = new GenericResult(defaultValue);
    } else {
      this.defaultValue = null;
    }

    this.required = required;
    this.hl7specs = getTokens(hl7spec);
    if (StringUtils.isNotBlank(condition)) {
      this.condition = new Condition(condition);
    }

    initVariables(rawvariables);

  }

  private void initVariables(Map<String, String> rawvariables) {
    this.variables = new ArrayList<>();
    if (rawvariables != null) {
      for (Entry<String, String> e : rawvariables.entrySet()) {
        StringTokenizer stk = new StringTokenizer(e.getValue(), " ").setIgnoreEmptyTokens(true);
        if (stk.getTokenList().size() == 2) {
          String vartype = stk.nextToken();

          String specs = stk.nextToken();

          this.variables.add(new Variable(e.getKey(), getTokens(specs), vartype));
        } else {


          this.variables.add(new Variable(e.getKey(), getTokens(e.getValue())));
        }
      }

    }
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public GenericResult getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  @Override
  public List<String> getspecs() {
    return new ArrayList<>(this.hl7specs);
  }

  public List<Variable> getVariables() {
    return new ArrayList<>(variables);
  }


  private static List<String> getTokens(String value) {
    if (StringUtils.isNotBlank(value)) {
      StringTokenizer st = new StringTokenizer(value, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }

    return new ArrayList<>();
  }

  /**
   * Evaluates Hl7spec value and resolves variables
   * 
   * @see com.ibm.whi.core.expression.Expression#evaluate(com.ibm.whi.core.message.InputData,
   *      java.util.Map)
   */
  @Override
  public GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");

    // resolve hl7spec
    LOGGER.info("Evaluating expression type {} , hl7spec {}", this.getType(), this.getspecs());
    GenericResult hl7Values = dataSource.extractMultipleValuesForSpec(this.getspecs(),
        ImmutableMap.copyOf(contextValues));
    LOGGER.info("Evaluating expression type {} , hl7spec {} returned hl7 value {} ", this.getType(),
        this.getspecs(), hl7Values);
    // resolve variables
    Map<String, GenericResult> resolvedVariables = new HashMap<>(contextValues);

    if (!this.isMultiple) {
    resolvedVariables.putAll(
          dataSource.resolveVariables(this.getVariables(), ImmutableMap.copyOf(contextValues)));
    }
    if (this.isConditionSatisfied(resolvedVariables)) {
      return evaluateExpression(dataSource, resolvedVariables, hl7Values);
    } else {
      return null;
    }

  }



  protected abstract GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> resolvedVariables, GenericResult hl7Values);





  protected static boolean isVar(String spec) {
    return StringUtils.isNotBlank(spec) && spec.startsWith("$") && spec.length() > 1;
  }

  protected static String getDataType(Object data) {

    if (data instanceof Structure) {
      return ((Structure) data).getName();
    } else if (data instanceof Type) {
      return ((Type) data).getName();
    } else {
      return data.getClass().getCanonicalName();
    }
  }

  @Override
  public boolean isConditionSatisfied(Map<String, GenericResult> contextValues) {
    if (this.condition != null) {
      return this.condition.evaluateCondition(contextValues);
    } else {
      return true;
    }
  }

  public boolean isMultiple() {
    return isMultiple;
  }

  public void setMultiple(boolean isMultiple) {
    this.isMultiple = isMultiple;
  }

  static Object getSingleValue(GenericResult hl7SpecValues) {
    Object hl7Value = null;
    if (hl7SpecValues != null && !hl7SpecValues.isEmpty()) {
      Object valList = hl7SpecValues.getValue();
      if (valList instanceof List && !((List) valList).isEmpty()) {
        hl7Value = ((List) valList).get(0);
      } else {
        hl7Value = hl7SpecValues.getValue();
      }
    }
    return hl7Value;
  }

}
