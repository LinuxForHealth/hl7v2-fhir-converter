package com.ibm.whi.core.expression.eval;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.ibm.whi.hl7.data.HL7GeneralUtils;
import com.ibm.whi.hl7.exception.NoMoreRepititionException;


public final class WHIAJexlEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(WHIAJexlEngine.class);
  private JexlEngine jexl;
  private Map<String, Object> functions = new HashMap<>();



  public WHIAJexlEngine() {
    jexl = new JexlBuilder().silent(false).debug(true).strict(true).create();
    LOGGER.info("silent:{} , strict :{} ", jexl.isSilent(), jexl.isStrict());
    functions.put(StringUtils.class.getSimpleName(), StringUtils.class);
    functions.put(NumberUtils.class.getSimpleName(), NumberUtils.class);
    functions.put(String.class.getSimpleName(), String.class);
    functions.put(UUID.class.getSimpleName(), UUID.class);
    functions.put("GeneralUtils", HL7GeneralUtils.class);



  }

  public Object evaluate(String jexlExp, Map<String, Object> context) {
    Preconditions.checkArgument(StringUtils.isNotBlank(jexlExp), "jexlExp cannot be blank");
    Preconditions.checkArgument(context != null, "context cannot be null");
    String trimedJexlExp = StringUtils.trim(jexlExp);
    // ensure that expression
    validateExpression(trimedJexlExp);

    LOGGER.info("Evaluating expression : {}", trimedJexlExp);
    Map<String, Object> localContext = new HashMap<>(functions);
    localContext.putAll(context);

    JexlExpression exp = jexl.createExpression(trimedJexlExp);
    JexlContext jc = new MapContext();
    localContext.entrySet().forEach(e -> jc.set(e.getKey(), e.getValue()));
    // Now evaluate the expression, getting the result
    try {
      Object obj = exp.evaluate(jc);
      LOGGER.info("Evaluated expression : {}, returning object {}", trimedJexlExp, obj);
      return obj;
    } catch (JexlException e) {
      if (e.getCause() instanceof NoMoreRepititionException) {
        throw new NoMoreRepititionException(
            e.getCause().getMessage() + ": expression" + trimedJexlExp,
            e.getCause());
      } else {
        throw e;
      }
    }

  }

  private void validateExpression(String jexlExp) {

    StringTokenizer stk = new StringTokenizer(jexlExp, ".").setIgnoreEmptyTokens(true);
    String tok = stk.nextToken();
    // format of expressions should be Function.method
    // if only one part is specified like Function then this is not a valid expression.
    if (stk.getTokenList().size() < 2 || functions.get(tok) == null) {
      throw new IllegalArgumentException("Expression has unsupported function: " + tok);
    }
    if (jexlExp.contains(";")) {
      throw new IllegalArgumentException(
          "Expression cannot contain character ; Expression: " + jexlExp);
    }

  }
}
