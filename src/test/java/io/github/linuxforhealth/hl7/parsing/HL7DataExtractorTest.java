/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Unmodifiable;
import ca.uhn.hl7v2.model.primitive.IS;
import ca.uhn.hl7v2.model.v26.datatype.CX;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.segment.AL1;
public class HL7DataExtractorTest {

  @Test
  public void returns_segment_if_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("PID");

  }


  @Test
  public void returns_segment_if_exists_group() throws IOException {
    String message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PROBLEM", 0, "PRB", 0).getValue();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("PRB");

  }


  @Test
  public void returns_segment_if_exists_group_no_segment() throws IOException {
    String message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PROBLEM", 0, "NTE", 0).getValue();
    assertThat(s).isNull();

  }



  @Test
  public void get_group_then_segment_for_non_existing_segment() throws IOException {
    String message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PROBLEM", 0).getValue();
    assertThat(s).isNotNull();

    Structure sub = hl7DTE.getAllStructures(s, "NTE").getValue();
    assertThat(sub).isNull();


  }



  @Test
  public void returns_null_if_segment_does_not_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PV1", 0).getValue();
    assertThat(s).isNull();

  }


  @Test
  public void returns_null_if_segment_does_not_exists_when_querying_all_repititions()
      throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";


    Message hl7message = Unmodifiable.unmodifiableMessage(getMessage(message));
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    List<Structure> s = hl7DTE.getAllStructures("OBX").getValues();
    assertThat(s).isEmpty();

  }


  @Test
  public void returns_null_if_segment_name_is_invalid() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";


    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PV67", 0).getValue();
    assertThat(s).isNull();

  }



  @Test
  public void returns_list_of_segment_for_repititions() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"

        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "AL1|3|DRUG|00004700^INFLUENZA VIRUS VACCINE||\r"
        + "AL1|4|BRANDNAME|00008604^LEVAQUIN||RASH ITCHING\r"
        + "AL1|5|BRANDNAME|00010302^PNEUMOVAX 23||\r";



    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    List<Structure> structures = hl7DTE.getAllStructures("AL1").getValues();
    assertThat(structures).isNotNull();
    assertThat(structures).hasSize(5);
    structures.forEach(s -> assertThat(s.getName()).isEqualTo("AL1"));

  }



  @Test
  public void returns_requested_repetition_of_segment() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"

        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "AL1|3|DRUG|00004700^INFLUENZA VIRUS VACCINE||\r"
        + "AL1|4|BRANDNAME4|00008604^LEVAQUIN||RASH ITCHING\r"
        + "AL1|5|BRANDNAME5|00010302^PNEUMOVAX 23||\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    List<Structure> structures = hl7DTE.getStructure("AL1", 4).getValues();
    assertThat(structures).isNotNull();
    assertThat(structures).hasSize(1);
    AL1 al1 = (AL1) structures.get(0);
    assertThat(al1.getAl12_AllergenTypeCode().getCwe1_Identifier().getValue())
        .isEqualTo("BRANDNAME5");

  }


  @Test
  public void returns_null_if_requested_repetition_of_segment_does_not_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"

        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "AL1|3|DRUG|00004700^INFLUENZA VIRUS VACCINE||\r"
        + "AL1|4|BRANDNAME4|00008604^LEVAQUIN||RASH ITCHING\r"
        + "AL1|5|BRANDNAME5|00010302^PNEUMOVAX 23||\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    List<Structure> structures = hl7DTE.getStructure("AL1", 6).getValues();
    assertThat(structures).isNotNull();
    assertThat(structures).isEmpty();


  }



  @Test
  public void returns_field_value_for_specific_rep_if_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("PID");

    Type t = hl7DTE.getType((Segment) s, 3, 1).getValue();
    assertThat(t).isNotNull();
    assertThat(t.getName()).isEqualTo("CX");
    CX cx = (CX) t;
    assertThat(cx.getCx1_IDNumber().getValue()).isEqualTo("000010017");
    assertThat(cx.getAssigningAuthority().getHd1_NamespaceID().getValue()).isEqualTo("MR");


  }

  @Test
  public void returns_all_field_values_if_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("PID");

    List<Type> types = hl7DTE.getTypes((Segment) s, 3).getValues();
    assertThat(types).isNotNull();
    assertThat(types).hasSize(3);
    assertThat(types.get(0).getName()).isEqualTo("CX");
    CX cx = (CX) types.get(0);
    assertThat(cx.getCx1_IDNumber().getValue()).isEqualTo("000010016");
    assertThat(cx.getAssigningAuthority().getHd1_NamespaceID().getValue()).isEqualTo("MR");


  }



  @Test
  public void returns_null_if_field_value_for_repitition_does_not_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    assertThat(s).isNotNull();
    assertThat(s.getName()).isEqualTo("PID");


    Type t = hl7DTE.getType((Segment) s, 3, 5).getValue();
    assertThat(t).isNull();

    List<Type> types = hl7DTE.getType((Segment) s, 3, 5).getValues();
    assertThat(types).isEmpty();


  }



  @Test
  public void returns_component_value_for_specific_rep_if_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 3, 1).getValue();
    Type comp = hl7DTE.getComponent(t, 1).getValue();

    assertThat(comp).isNotNull();
    assertThat(comp.getName()).isEqualTo("ST");
    ST id = (ST) comp;
    assertThat(id.getValue()).isEqualTo("000010017");

  }



  @Test
  public void returns_null_if_component_value_for_does_not_exists() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 3, 1).getValue();
    Type comp = hl7DTE.getComponent(t, 6).getValue();
    assertThat(comp).isNull();

    List<Type> comps = hl7DTE.getComponent(t, 6).getValues();
    assertThat(comps).isEmpty();

  }


  @Test
  public void returns_component_and_subcomponent_value_for_specific_rep_if_exists()
      throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 3, 1).getValue();
    Type comp = hl7DTE.getComponent(t, 4, 1).getValue();

    assertThat(comp).isNotNull();
    assertThat(comp.getName()).isEqualTo("IS");
    IS id = (IS) comp;
    assertThat(id.getValue()).isEqualTo("MR");

  }



  @Test
  public void returns_null_if_component_subcomponent_value_for_does_not_exists()
      throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("PID", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 3, 1).getValue();
    Type comp = hl7DTE.getComponent(t, 4, 2).getValue();
    assertThat(comp).isNull();

    List<Type> comps = hl7DTE.getComponent(t, 4, 2).getValues();
    assertThat(comps).isEmpty();

  }



  @Test
  public void extracts_component_from_variable_type_primitive() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r";

    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("OBX", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 5, 0).getValue();
    assertThat(t).isNotNull();
    Type comp = hl7DTE.getComponent(t, 1).getValue();
    assertThat(comp).isNotNull();
    Primitive p = (Primitive) comp;
    assertThat(p.getValue()).isEqualTo("ECHOCARDIOGRAPHIC REPORT");


  }


  @Test
  public void extracts_component_from_variable_type_compositive() throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"
        + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^MEIECG";
    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("OBX", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 5, 0).getValue();
    assertThat(t).isNotNull();
    Type comp = hl7DTE.getComponent(t, 2).getValue();
    assertThat(comp).isNotNull();
    Primitive p = (Primitive) comp;
    assertThat(p.getValue()).isEqualTo("No significant change was found");


  }


  @Test
  public void extracts_component_from_variable_type_compositive_with_subcomponent()
      throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"
        + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^MEIECG";
    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("OBX", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 5, 0).getValue();
    assertThat(t).isNotNull();
    Type comp = hl7DTE.getComponent(t, 2, 1).getValue();
    assertThat(comp).isNotNull();
    Primitive p = (Primitive) comp;
    assertThat(p.getValue()).isEqualTo("No significant change was found");


  }


  @Test
  public void returns_null_from_variable_type_compositive_with_subcomponent_if_subcomponent_does_not_exists()
      throws IOException {
    String message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r"
        + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^MEIECG";
    Message hl7message = getMessage(message);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);


    Structure s = hl7DTE.getStructure("OBX", 0).getValue();
    Type t = hl7DTE.getType((Segment) s, 5, 0).getValue();
    assertThat(t).isNotNull();
    Type comp = hl7DTE.getComponent(t, 2, 2).getValue();
    assertThat(comp).isNull();


  }


  //

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
