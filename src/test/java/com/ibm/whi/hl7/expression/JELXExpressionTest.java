package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ibm.whi.hl7.expression.model.JELXExpression;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Unmodifiable;
import ca.uhn.hl7v2.model.v26.datatype.CX;

public class JELXExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";
  private static final String SOME_VALUE_1 = "SOME_VALUE_1";
  private static final String SOME_VALUE_2 = "SOME_VALUE_2";
  private static final String SOME_VALUE_3 = "SOME_VALUE_3";

  @Test
  public void test_simple() {
    JELXExpression exp =
        new JELXExpression(
        "String.join(\" \",  var1,var2, var3)", new HashMap<>());
    Map<String, Object> context = new HashMap<>();
    context.put("var1", SOME_VALUE_1);
    context.put("var2", SOME_VALUE_2);
    context.put("var3", SOME_VALUE_3);

    Object value = exp.execute(context);
    assertThat(value).isEqualTo(SOME_VALUE_1 + " " + SOME_VALUE_2 + " " + SOME_VALUE_3);
  }


  @Test
  public void test_with_variables() throws IOException, HL7Exception {

    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

    HL7HapiParser hparser = null;
    try {
      hparser = new HL7HapiParser();

      Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(message));

      Hl7DataExtractor hl7DTE = new Hl7DataExtractor(hl7message);



    Map<String, String> var = new HashMap<>();
    var.put("var1", "CX.1");
    var.put("var2", "CX.2");
      var.put("var3", "CX.2");

      CX cx = new CX(hl7message);
    cx.getCx1_IDNumber().setValue("value1");
      cx.getCx2_IdentifierCheckDigit().setValue("value2");


      JELXExpression exp = new JELXExpression("String.join(\" \",  var1,var2, var3)", var);

    Map<String, Object> context = new HashMap<>();
    context.put("CX", cx);
      context.put("hde", hl7DTE);
      context.put("String", String.class);
    Object value = exp.execute(context);
    assertThat(value).isEqualTo(SOME_VALUE_1 + " " + SOME_VALUE_2 + " " + SOME_VALUE_3);


    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }

  }

}
