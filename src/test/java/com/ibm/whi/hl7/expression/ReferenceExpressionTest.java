package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Unmodifiable;

public class ReferenceExpressionTest {

  @Test
  public void test1_segment() throws IOException, HL7Exception {
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

      Map<String, Object> context = new HashMap<>();
      context.put("hde", hl7DTE);
      Structure s = hl7DTE.getStructure("PID", 0);
      context.put("PID", s);
      ReferenceExpression exp = new ReferenceExpression("Single", "datatype/Identifier", "PID.3");
      assertThat(exp.getData()).isNotNull();


      Object value = exp.execute(context);
      Map<String, Object> result = (Map<String, Object>) value;
      assertThat(result.get("use")).isEqualTo(null);
      assertThat(result.get("value")).isEqualTo("000010016");
      assertThat(result.get("system")).isEqualTo("MR");


    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }



  }



  @Test
  public void test1_segment_rep() throws IOException, HL7Exception {
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

      Map<String, Object> context = new HashMap<>();
      context.put("hde", hl7DTE);
      Structure s = hl7DTE.getStructure("PID", 0);
      context.put("PID", s);
      ReferenceExpression exp = new ReferenceExpression("Array", "datatype/Identifier", "PID.3");
      assertThat(exp.getData()).isNotNull();


      Object value = exp.execute(context);

      List<Object> results = (List<Object>) value;
      assertThat(results).hasSize(3);


      Map<String, Object> result = (Map<String, Object>) results.get(0);
      assertThat(result.get("use")).isEqualTo(null);
      assertThat(result.get("value")).isEqualTo("000010016");
      assertThat(result.get("system")).isEqualTo("MR");


    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }



  }

}
