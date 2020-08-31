package com.ibm.whi.hl7.expression.eval;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.data.GeneralUtils;
import com.ibm.whi.hl7.exception.NoMoreRepititionException;
import antlr.StringUtils;

public class WHIAJexlEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(WHIAJexlEngine.class);
  private JexlEngine jexl;
  private Map<String, Object> defaultContext = new HashMap<>();



  public WHIAJexlEngine() {
    jexl = new JexlBuilder().silent(false).debug(true).strict(true).create();
    LOGGER.info("silent:{} , strict :{} ", jexl.isSilent(), jexl.isStrict());
    defaultContext.put("StringUtils", StringUtils.class);
    defaultContext.put("NumberUtils", NumberUtils.class);
    defaultContext.put("String", String.class);
    defaultContext.put("UUID", UUID.class);
    defaultContext.put("GeneralUtils", GeneralUtils.class);



  }

  public Object evaluate(String jexlExp, Map<String, Object> context) {

    LOGGER.info("Evaluating expression : {}", jexlExp);
    Map<String, Object> localContext = new HashMap<>(defaultContext);
    localContext.putAll(context);

    JexlExpression exp = jexl.createExpression(jexlExp);
    JexlContext jc = new MapContext();
    localContext.entrySet().forEach(e -> jc.set(e.getKey(), e.getValue()));
    // Now evaluate the expression, getting the result
    try {
      Object obj = exp.evaluate(jc);
      LOGGER.info("Evaluated expression : {}, returning object {}", jexlExp, obj);
      return obj;
    } catch (JexlException e) {
      if (e.getCause() instanceof NoMoreRepititionException) {
        throw new NoMoreRepititionException(e.getCause().getMessage() + ": expression" + jexlExp,
            e.getCause());
      } else {
        throw e;
      }
    }

  }
}
