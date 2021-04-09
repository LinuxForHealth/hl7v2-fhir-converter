# LinuxForHealth HL7 to FHIR Converter Development Guide

This guide outlines the general process used to implement a new HL7 event to FHIR resource mapping. Resource mapping is declarative and utilizes yaml configuration files.

Implementation Steps include:

* Create or obtain a sample HL7 v2 message/triggering event file for the use-case
* Review the official HL7 documentation for the target FHIR Resource.
* Create new FHIR resource templates if necessary.
* Create the HL7 message template to support the message/trigger event to resource mapping.
* Implement a test case to validate mapping results


## Sample HL7 v2 Message

The first step in implementing a new mapping configuration is to generate or obtain a valid HL7 v2 message/triggering event for the use-case. The message can be validated using the HAPI libraries packaged with the converter library. The example below demonstrates default HAPI validation within a JUnit5 test case context.

```java
package com.linuxforhealth.example;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HapiValidationTest {

  private HapiContext ctx = new DefaultHapiContext();

  @Test
  void testValidMessage() throws HL7Exception {

    String validMsg = "MSH|^~\\&|system1|W|system2|UHN|200105231927||ADT^A01^ADT_A01|22139243|P|2.4\r"
        + "EVN|A01|200105231927\r"
        + "PID||9999999999|2216506||Duck^Donald^^^MR.^MR.||19720227|M|||123 Foo ST.^^TORONTO^ON^M6G 3E6^CA^H~123 Foo ST.^^TORONTO^ON^M6G 3E6^CA^M|1811|(416)111-1111||E^ENGLISH|S|PATIENT DID NOT INDICATE|211004554\r"
        + "PV1|||ZFAST TRACK^WAITING^13|E^EMERGENCY||369^6^13^U EM EMERGENCY DEPARTMENT^ZFAST TRACK WAITING^FT WAIT 13^FTWAIT13^FT WAITING^FTWAIT13|^MOUSE^MICKEY^M^^DR.^MD|||SUR||||||||I|211004554||||||||||||||||||||W|||||200105231927\r"
        + "PV2||F|^R/O APPENDICIAL ABSCESS\r"
        + "IN1|1||001001|OHIP|||||||||||||||^^^^^^M|||||||||||||||||||||||||^^^^^^M";

    Message msg = ctx.getGenericParser().parse(validMsg);
    Assertions.assertNotNull(msg);

    String[] expectedNames = new String[] {"MSH", "EVN", "PID", "PV1", "PV2", "IN1"};
    Assertions.assertArrayEquals(expectedNames, msg.getNames());
  }
}
```

The test case relies on the default validation provided by the `parse` method. If an error occurs, the parser will raise an exception within the HL7Exception hierarchy.

## Review the Official FHIR (HL7 Documentation)

The Official FHIR documentation, provided by the HL7 organization, provides complete documentation for FHIR resources and the corresponding HL7 v2 mappings. The best place to start is by reviewing the [resource index](https://www.hl7.org/fhir/resourcelist.html).

Each resource page contains field definitions [(Example: Medication Resource)](https://www.hl7.org/fhir/medication.html), and HL7 v2 mappings [(Example: Medication Resource)][https://www.hl7.org/fhir/medication-mappings.html].
Review the documentation determine which FHIR resource fields to include. The FHIR specification supports numerous resources with a variety of fields. It is likely that a subset of fields are required for a given use-case.

## Create new FHIR Resource Templates

Review the existing FHIR resource templates in `src/main/resources/hl7/resource` to determine if the use-case requires a new resource template.

## Create the HL7 Message Template

Create a new HL7 Message Mapping template in `/src/main/resources/hl7/message` to support the mapping.
Add the new message type to the `supported.hl7.messages` property in [src/main/resources/config.properties](src/main/resources/config.properties).

## Create new FHIR Datatype Template

If a new FHIR datatype is required, add a yaml file to [src/main/resources/datatype](src/main/resources/datatype).

## Implement a Test Case

Implement a test case to validate the HL7 Message to FHIR Resource mapping. End-to-end test cases are found in `src/main/test/java/io/github/linuxforhealth/FhirConverterTest.java`. Create a new test class for new HL7 messages that are added.
