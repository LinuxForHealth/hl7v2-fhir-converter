/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.core.terminology.SimpleCode;
import io.github.linuxforhealth.hl7.message.HL7MessageData;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;

class ResourceExpressionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceExpressionTest.class);

    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
            + "EVN|A01|20130617154644\r"
            + "PID|1|465 306 5961|000010016^5^M11^SY1^MR^|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
            + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
            + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

    @Test
    void test1_segment() throws IOException {

        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("PID", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("PID.3")
                .withValueOf("datatype/Identifier").build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        Map<String, Object> result = (Map<String, Object>) value.getValue();
        assertThat(result.get("use")).isNull();
        assertThat(result.get("value")).isEqualTo("000010016");

    }

    @Test
    void test_component_required_missing() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961||407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("PID", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("PID.3")
                .withValueOf("datatype/Identifier").build();

        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        assertThat(value).isNull();

    }

    @Test
    void test_picks_next_value_from_rep_if_first_fails_condition_or_check()
            throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|^^^MR^SSS^^20091020^20200101~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";

        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("PID", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("PID.3")
                .withValueOf("datatype/Identifier").build();

        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        assertThat(value).isNotNull();
        Map<String, Object> result = (Map<String, Object>) value.getValue();
        assertThat(result.get("use")).isNull();
        assertThat(result.get("value")).isEqualTo("000010017");
        assertThat(result.get("type")).isNull();

    }

    @Test
    void test1_segment_rep() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|000010016^^^SY1^MR~000010017^^^SY2^SS~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("PID", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("PID.3")
                .withValueOf("datatype/Identifier").withGenerateList(true).build();

        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();
        context.put("code", new SimpleEvaluationResult(hl7DTE.getTypes((Segment) s, 3)));

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Object> results = (List<Object>) value.getValue();
        assertThat(results).hasSize(3);

        Map<String, Object> result = (Map<String, Object>) results.get(0);
        assertThat(result.get("use")).isNull();
        assertThat(result.get("value")).isEqualTo("000010016");
        assertThat(result.get("system")).isNull();
        assertThat(result.get("type")).isNotNull();

    }

    @Test
    void test1_segment_identifier_obx() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
                + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F||\r";
        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("OBX", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("OBX.3")
                .withValueOf("datatype/Identifier").build();

        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));
        Map<String, Object> result = (Map<String, Object>) value.getValue();
        assertThat(result.get("use")).isNull();
        assertThat(result.get("value")).isEqualTo("1234");
        assertThat(result.get("system")).isNull();

    }

    @Test
    void testSegmentIdentifierObxCc() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|12345678^^^MR|407623|TestPatient^John^^MR||19700101|male||||||||||\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
                + "OBX|1|TX|1234^some text^SCT||First line: ECHOCARDIOGRAPHIC REPORT||||||F||\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("OBX", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("OBX.3")
                .withValueOf("datatype/Identifier").withGenerateList(true).build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
        Map<String, Object> type = (Map<String, Object>) result.get(0).get("type");

        assertThat(type.get("text")).isEqualTo("some text");
        assertThat(type.get("coding")).isNotNull();
        List<Object> list = (List) type.get("coding");
        SimpleCode scs = (SimpleCode) list.get(0);
        assertThat(scs.getCode()).isEqualTo("1234");
        assertThat(scs.getSystem()).isEqualTo("http://snomed.info/sct");
        assertThat(scs.getDisplay()).isEqualTo("some text");

    }

    @Test
    void testSegmentIdentifierObxCcKnownSystem() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|123456^^^MR|407623|TestPatient^John^^^MR||19700101|male||||||||||\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
                + "OBX|1|TX|1234^some text^SCT||First line: ECHOCARDIOGRAPHIC REPORT||||||F||\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("OBX", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("OBX.3")
                .withValueOf("datatype/CodeableConcept").withGenerateList(true).build();

        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
        assertThat(result.get(0).get("text")).isEqualTo("some text");
        assertThat(result.get(0).get("coding")).isNotNull();
        List<Object> list = (List) result.get(0).get("coding");
        SimpleCode scs = (SimpleCode) list.get(0);
        assertThat(scs.getCode()).isEqualTo("1234");
        assertThat(scs.getSystem()).isEqualTo("http://snomed.info/sct");
        assertThat(scs.getDisplay()).isEqualTo("some text");

    }

    @Test
    void testCodeableConceptFromISTtype() throws IOException {
        String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|TestPatient^Jane|19700101|female||||||||||\r"
                + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
                + "OBX|1|TX|1234^^SCT||First line: ECHOCARDIOGRAPHIC REPORT|||AA|||F||\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getStructure("OBX", 0).getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("OBX.8")
                .withValueOf("datatype/CodeableConcept").withGenerateList(true).build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
        assertThat(result.get(0).get("text")).isNull();
        assertThat(result.get(0).get("coding")).isNotNull();
        List<SimpleCode> scs = (List<SimpleCode>) result.get(0).get("coding");
        SimpleCode sc = scs.get(0);
        assertThat(sc.getCode()).isEqualTo("AA");
        assertThat(sc.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0078");
        assertThat(sc.getDisplay()).isEqualTo("Critically abnormal");

    }

    @Test
    void test_organization_creation_with_missing_id_value() throws IOException {
        String message = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
                + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
                + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r"
                + "OBX|5|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getAllStructures("ORDER", 0, "RXA").getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("RXA.17")
                .withValueOf("resource/Organization").withGenerateList(true).build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
        assertThat(result.get(0).get("name")).isEqualTo("sanofi");
        assertThat(result.get(0).get("identifier")).isNull();

        LOGGER.debug("result=" + result);

    }

    @Test
    void test_organization_creation_with_missing_id_and_name_value() throws IOException {
        String message = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|^^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
                + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
                + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r"
                + "OBX|5|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getAllStructures("ORDER", 0, "RXA").getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("RXA.17")
                .withValueOf("resource/Organization").withGenerateList(true).build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        assertThat(value).isNull();

    }

    @Test
    void test_organization_creation_with_mo_missing_value() throws IOException {
        String message = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
                + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
                + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r"
                + "OBX|5|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r";

        Message hl7message = getMessage(message);

        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);

        Structure s = hl7DTE.getAllStructures("ORDER", 0, "RXA").getValue();
        ExpressionAttributes attr = new ExpressionAttributes.Builder().withSpecs("RXA.17")
                .withValueOf("resource/Organization").withGenerateList(true).build();
        ResourceExpression exp = new ResourceExpression(attr);
        assertThat(exp.getData()).isNotNull();

        Map<String, EvaluationResult> context = new HashMap<>();

        EvaluationResult value = exp.evaluate(new HL7MessageData(hl7DTE), ImmutableMap.copyOf(context),
                new SimpleEvaluationResult(s));

        List<Map<String, Object>> result = (List<Map<String, Object>>) value.getValue();
        assertThat(result.get(0).get("name")).isEqualTo("sanofi");
        List<Map<String, Object>> identifiers = (List<Map<String, Object>>) result.get(0).get("identifier");
        assertThat(identifiers.get(0).get("value")).isEqualTo("PMC");

        LOGGER.debug("result=" + result);

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
