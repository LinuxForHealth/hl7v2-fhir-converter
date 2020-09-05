package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;

public abstract class AbstractExpression implements Expression {

  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private final String type;
  private final GenericResult defaultValue;
  private final boolean required;
  private final List<String> hl7specs;
  private List<Variable> variables;

  public AbstractExpression(String type, Object defaultValue, boolean required, String hl7spec,
      Map<String, String> rawvariables) {
    if (type == null) {
      this.type = "String";
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


  protected static GenericResult getVariableValueFromVariableContextMap(String varName,
      ImmutableMap<String, GenericResult> varables) {
    if (StringUtils.isNotBlank(varName)) {
      GenericResult fetchedValue;
      fetchedValue = varables.get(varName.replace("$", ""));
      return fetchedValue;
    } else {
      return null;
    }
  }



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




}
