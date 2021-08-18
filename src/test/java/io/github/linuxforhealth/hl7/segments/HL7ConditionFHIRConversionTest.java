/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class HL7ConditionFHIRConversionTest {

  private static final ConverterOptions OPTIONS = new Builder().withPrettyPrint().build();

  @Test
  public void validate_encounter() {

    String hl7message = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\r"
        + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
        + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
        + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|DOE^JOHN^A^|C|Y|20210322154326|V45|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    System.out.println(json);
    assertThat(json).isNotBlank();

    FHIRContext context = new FHIRContext(true, false);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Find the condition from the FHIR bundle.
    List<Resource> conditionResource = e.stream()
        .filter(v -> ResourceType.Condition == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(conditionResource).hasSize(1);

    Resource condition = conditionResource.get(0);
  }

  @Test
  public void validate_problem() {

    String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
        + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
        + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
        + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20170310074000||||C^Confirmed^Confirmation Status List|resolved|20170310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    System.out.println(json);
    assertThat(json).isNotBlank();

    FHIRContext context = new FHIRContext(true, false);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Find the condition from the FHIR bundle.
    List<Resource> conditionResource = e.stream()
        .filter(v -> ResourceType.Condition == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(conditionResource).hasSize(1);

    Resource condition = conditionResource.get(0);
  }


  @Test
  public void validate_naughty_prb() {

    String hl7message = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
        + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
        + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
        + "PRB|AD|20210101000000|G47.31^Primary central sleep apnea^ICD-10-CM|28827016|||20210101000000|20210101000000|20210101000000||||ACTIVE||20210101000000|20210101000000\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    System.out.println(json);
    assertThat(json).isNotBlank();

    FHIRContext context = new FHIRContext(true, false);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Find the condition from the FHIR bundle.
    List<Resource> conditionResource = e.stream()
        .filter(v -> ResourceType.Condition == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(conditionResource).hasSize(1);

    Resource condition = conditionResource.get(0);
    System.out.println("Cond=" + condition.toString());
  }

  @Test
  public void validate_reasonReference() {

    String hl7message = 
    "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"+
    "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"+
    "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"+
    "PV1|1|O|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|FR|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"+
    "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|A^Ambulance^HL70430\n"+
    "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\n"+
    "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\n"+
    "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\n"+
    "DG1|4|D4|Q99.9^Chromosomal abnormality, unspecified^ICD-10^^^|Chromosomal abnormality, unspecified||A\n"+
    "DG1|5|D5|G66^Mitral Valve Heart Arteriosclerosis With Calcification^ICD-10^^^|Mitral Valve Heart Arteriosclerosis With Calcification||A\n"+
    "DG1|6|D6|H78^Mitral valve regurgitation^ICD-10^^^|Mitral valve regurgitation||A\n"+
    "DG1|6|D7|J78^Mitral valve disorder in childbirth^ICD-10^^^|Mitral valve disorder in childbirth||A\n"+
    "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    System.out.println(json);
    assertThat(json).isNotBlank();

    FHIRContext context = new FHIRContext(true, false);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Find the condition from the FHIR bundle.
    List<Resource> conditionResource = e.stream()
        .filter(v -> ResourceType.Condition == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(conditionResource).hasSize(1);

    Resource condition = conditionResource.get(0);
    System.out.println("Cond=" + condition.toString());
  }


}
