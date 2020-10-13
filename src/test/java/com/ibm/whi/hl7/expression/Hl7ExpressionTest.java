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
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.core.expression.SimpleEvaluationResult;
import com.ibm.whi.core.terminology.SimpleCode;
import com.ibm.whi.hl7.message.HL7MessageData;
import com.ibm.whi.hl7.parsing.HL7DataExtractor;
import com.ibm.whi.hl7.parsing.HL7HapiParser;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;

public class Hl7ExpressionTest {

  @Test
  public void test1_segment() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();

    Hl7Expression exp = new Hl7Expression("String", "PID.3");



    Map<String, EvaluationResult> context = new HashMap<>();
    // context.put("PID", new SimpleEvaluationResult(s));


    EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
        new SimpleEvaluationResult(s));
    assertThat((String) value.getValue()).isEqualTo("000010016");


  }



  @Test
  public void test3_field() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();

    Type type = hl7DTE.getType((Segment) s, 3, 0).getValue();


    Hl7Expression exp = new Hl7Expression("String", "CX.1");



    Map<String, EvaluationResult> context = new HashMap<>();
    // context.put("PID", new SimpleEvaluationResult(s));
    // context.put(type.getName(), new SimpleEvaluationResult(type));


    EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
        new SimpleEvaluationResult(type));


    assertThat((String) value.getValue()).isEqualTo("000010016");



  }

  @Test
  public void test3_field_subcomponent() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

    Message hl7message = getMessage(message);

    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();


    Hl7Expression exp = new Hl7Expression("String", "PID.3.1");



    Map<String, EvaluationResult> context = new HashMap<>();
    // context.put("PID", new SimpleEvaluationResult(s));


    EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
        new SimpleEvaluationResult(s));


    assertThat((String) value.getValue()).isEqualTo("000010016");



  }



  @Test
  public void test3_field_options() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location|RE|||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

    Structure s = hl7DTE.getStructure("PV1", 0).getValue();
    Hl7Expression exp = new Hl7Expression("String", "PV1.3 | PV1.4 |PV1.59");

    Map<String, EvaluationResult> context = new HashMap<>();
    // context.put("PV1", new SimpleEvaluationResult(s));


    EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
        new SimpleEvaluationResult(s));



    assertThat((String) value.getValue()).isEqualTo("Location");



  }


  @Test
  public void test3_CODING_SYSTEM_V2_field() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|CE|93000CMP^LIN^CLIP|11|1305^No significant change was found^MEIECG|||AA";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("OBX", 0).getValue();
    Hl7Expression exp = new Hl7Expression("CODING_SYSTEM_V2", "OBX.8");



    Map<String, EvaluationResult> context = new HashMap<>();
    // context.put("OBX", new SimpleEvaluationResult(s));


    EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
        new SimpleEvaluationResult(s));


    assertThat((SimpleCode) value.getValue()).isNotNull();
    SimpleCode sc = (SimpleCode) value.getValue();
    assertThat(sc.getCode()).isEqualTo("AA");
    assertThat(sc.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0078");
    assertThat(sc.getDisplay()).isEqualTo("Critically abnormal");



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
