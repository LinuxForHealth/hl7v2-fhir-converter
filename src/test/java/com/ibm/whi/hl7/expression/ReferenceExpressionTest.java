package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.model.ReferenceExpression;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
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

      Structure s = hl7DTE.getStructure("PID", 0).getValue();

      ReferenceExpression exp = new ReferenceExpression("Single", "datatype/IdentifierCX", "PID.3");
      assertThat(exp.getData()).isNotNull();


      Map<String, GenericResult> context = new HashMap<>();
      context.put("PID", new GenericResult(s));

      Map<String, Object> executable = new HashMap<>();
      executable.put("hde", hl7DTE);


      GenericResult value =
          exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
      Map<String, Object> result = (Map<String, Object>) value.getValue();
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


      Structure s = hl7DTE.getStructure("PID", 0).getValue();

      ReferenceExpression exp = new ReferenceExpression("Array", "datatype/IdentifierCX", "PID.3");
      assertThat(exp.getData()).isNotNull();


      Map<String, GenericResult> context = new HashMap<>();
      context.put("PID", new GenericResult(s));
      context.put("code", new GenericResult(hl7DTE.getTypes((Segment) s, 3)));
      Map<String, Object> executable = new HashMap<>();
      executable.put("hde", hl7DTE);


      GenericResult value =
          exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));

      List<Object> results = (List<Object>) value.getValue();
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



  @Test
  public void test1_segment_identifier_obx() throws IOException, HL7Exception {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F||\r";

    HL7HapiParser hparser = null;
    try {
      hparser = new HL7HapiParser();

      Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(message));

      Hl7DataExtractor hl7DTE = new Hl7DataExtractor(hl7message);


      Structure s = hl7DTE.getStructure("OBX", 0).getValue();

      ReferenceExpression exp =
          new ReferenceExpression("Single", "datatype/IdentifierCWE", "OBX.3");
      assertThat(exp.getData()).isNotNull();


      Map<String, GenericResult> context = new HashMap<>();
      context.put("OBX", new GenericResult(s));

      Map<String, Object> executable = new HashMap<>();
      executable.put("hde", hl7DTE);


      GenericResult value =
          exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
      Map<String, Object> result = (Map<String, Object>) value.getValue();
      assertThat(result.get("use")).isEqualTo(null);
      assertThat(result.get("value")).isEqualTo("1234");
      assertThat(result.get("system")).isEqualTo(null);


    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }



  }

  @Test
  public void test1_segment_identifier_obx_cc() throws IOException, HL7Exception {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F||\r";

    HL7HapiParser hparser = null;
    try {
      hparser = new HL7HapiParser();

      Message hl7message = Unmodifiable.unmodifiableMessage(hparser.getParser().parse(message));

      Hl7DataExtractor hl7DTE = new Hl7DataExtractor(hl7message);


      Structure s = hl7DTE.getStructure("OBX", 0).getValue();

      ReferenceExpression exp =
          new ReferenceExpression("Array", "datatype/CodeableConcept", "OBX.3");
      assertThat(exp.getData()).isNotNull();

      Map<String, GenericResult> context = new HashMap<>();
      context.put("OBX", new GenericResult(s));
      context.put("code", new GenericResult(hl7DTE.getTypes((Segment) s, 3).getValue()));
      Map<String, Object> executable = new HashMap<>();
      executable.put("hde", hl7DTE);


      GenericResult value =
          exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));

      List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
      assertThat(result.get(0).get("text")).isEqualTo("1234");



    } finally {
      if (hparser != null) {
        hparser.getContext().close();
      }
    }



  }

}
