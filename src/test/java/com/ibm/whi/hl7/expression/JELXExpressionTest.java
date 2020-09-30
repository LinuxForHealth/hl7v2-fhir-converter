/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.hl7.message.HL7MessageData;
import com.ibm.whi.hl7.parsing.HL7DataExtractor;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.datatype.CX;

public class JELXExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";
  private static final String SOME_VALUE_1 = "SOME_VALUE_1";
  private static final String SOME_VALUE_2 = "SOME_VALUE_2";
  private static final String SOME_VALUE_3 = "SOME_VALUE_3";

  @Test
  public void test_simple() throws IOException {

    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    JELXExpression exp =
        new JELXExpression("String.join(\" \",  var1,var2, var3)", new HashMap<>());



    Map<String, GenericResult> context = new HashMap<>();
    context.put("var1", new GenericResult(SOME_VALUE_1));
    context.put("var2", new GenericResult(SOME_VALUE_2));
    context.put("var3", new GenericResult(SOME_VALUE_3));

    GenericResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context));

    assertThat(value.getValue()).isEqualTo(SOME_VALUE_1 + " " + SOME_VALUE_2 + " " + SOME_VALUE_3);



  }


  @Test
  public void test_with_variables() throws IOException, DataTypeException {

    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";



    Message hl7message = getMessage(message);

    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

    CX cx = new CX(hl7message);
    cx.getCx1_IDNumber().setValue(SOME_VALUE_1);
    cx.getCx2_IdentifierCheckDigit().setValue(SOME_VALUE_2);


    Map<String, GenericResult> context = new HashMap<>();
    context.put("CX", new GenericResult(cx));


    Map<String, String> var = new HashMap<>();
    var.put("var1", "String, CX.1");
    var.put("var2", "String, CX.2");
    var.put("var3", "String, CX.2");

    JELXExpression exp = new JELXExpression("String.join(\" \",  var1,var2, var3)", var);


    GenericResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context));

    assertThat(value.getValue()).isEqualTo(SOME_VALUE_1 + " " + SOME_VALUE_2 + " " + SOME_VALUE_2);


  }


  private static Message getMessage(String message) throws IOException {
    HL7HapiParser hparser = null;

    try {
      hparser = new HL7HapiParser();
      return hparser.getParser().parse(message);
    } catch (HL7Exception e) {
      throw new IllegalArgumentException(e);
    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }

  }
}
