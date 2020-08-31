package com.ibm.whi.hl7.expression.eval;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;


public class WHIAJexlEngineTest {

  @Test
  public void test() {
    WHIAJexlEngine engine= new WHIAJexlEngine();
    Map<String, Object> context = new HashMap<>();
    context.put("var1", "s");
    context.put("var2", "t");
    context.put("var3", "u");

    String value = (String) engine.evaluate("String.join(\" \",  var1,var2, var3)", context);
    assertThat(value).isEqualTo("s t u");
  }

}
