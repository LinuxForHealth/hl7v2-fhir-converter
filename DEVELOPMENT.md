# LinuxForHealth HL7 to FHIR Converter Development Guide

This guide outlines the practical steps and broader process used to implement a new HL7 message event to FHIR resource mapping. Resource mapping is declarative and utilizes yaml configuration files.

Implementation Steps include:

* Create an issue so your work can be tracked and you can get feedback and input. (See [CONTRIBUTING](CONTRIBUTING.md).)
* What is it you're doing?
  - 1 _Adding new field mappings to an existing segment?_ Then:
    * Review the official HL7 documentation for the target FHIR Resource.
    * Modify the existing segment template and resource templates to map the HL7 fields to FHIR resources and data objects.
    * Modify an existing segment test with new field / resource assertions
  - 2 _Adding new segments to an existing message trigger?_ Then:
    * Review the official HL7 documentation for the target FHIR Resource.
    * Modify the existing message template and resource templates to map the HL7 segments to FHIR resources and data objects.
    * Modify an existing message test with new segment / resource assertions
  - 3 _Creating a new message / trigger?_ Then
    * Validate your proposed new message/triggering event against HL7 documentation and try parsing it.
    * Review the official HL7 documentation for the target FHIR Resource.
    * Create a new message template
    * Create new FHIR resource templates if necessary.
    * Create the HL7 message template to support the message/trigger event to resource mapping.
    * Implement tests for both the messages to resources and segment fields to resource data to validate mapping results.
* Make a PR and request review (See [CONTRIBUTING](CONTRIBUTING.md).)
* Suggestions for new developers

## 1. Adding new field mappings to an existing segment
### Review the Official FHIR (HL7 Documentation)

The Official FHIR documentation, provided by the HL7 organization, provides complete documentation for FHIR resources and the corresponding HL7 v2 mappings. The best place to start is by reviewing the [resource index](https://www.hl7.org/fhir/resourcelist.html).

Each resource page contains field definitions [(Example: Medication Resource)](https://www.hl7.org/fhir/medication.html), and HL7 v2 mappings [(Example: Medication Resource)](https://www.hl7.org/fhir/medication-mappings.html).
Review the documentation determine which FHIR resource fields to include. The FHIR specification supports numerous resources with a variety of fields. It is likely that a subset of fields are required for a given use-case.

### Modifying an HL7 Segment Template

Find the associated FHIR resource that maps from the fields in the HL7 Segment. A good example is [src/main/resources/hl7/resource/Patient.yml](src/main/resources/hl7/resource/Patient.yml) which maps for the `PID` segment.  (TEMPLATING)[TEMPLATING.md] explains how the templates work for mappings.

Reuse existing resources other resources from from [src/main/resources/hl7/resource](src/main/resources/hl7/resource), [src/main/resources/hl7/secondary](src/main/resources/hl7/secondary), and data types from [src/main/resources/hl7/datatype](src/main/resources/hl7/datatype) when possible.  Only create new templates if needed. 

### Modify an existing segment test with new field to resource assertions
 Modify an existing test in [src/test/java/io/github/linuxforhealth/hl7/segments](src/test/java/io/github/linuxforhealth/hl7/segments) to test details of fields.  As an example, see [Hl7FinancialInsuranceTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7FinancialInsuranceTest.java).  Copy and create a new segment test if needed.   Test cases are vital to help check new function, and also ensure that an added function does not break when other later function is added.  

## 2. Adding New Segments to an Existing Message Trigger 
### Review the Official FHIR (HL7 Documentation)
(See explanation above)
### Modifying an HL7 Message Template

To add new segments to an existing message, find the associated message template.  These are all in [src/main/resources/hl7/message](src/main/resources/hl7/message). A good example is [src/main/resources/hl7/message/DFT_P03.yml](src/main/resources/hl7/message/DFT_P03.yml) which processes the DFT_P03 message. (TEMPLATING)[TEMPLATING.md] explains how segments map to FHIR resources. 

Reuse existing resources other resources from from [src/main/resources/hl7/resource](src/main/resources/hl7/resource), [src/main/resources/hl7/secondary](src/main/resources/hl7/secondary), and data types from [src/main/resources/hl7/datatype](src/main/resources/hl7/datatype) when possible.  Only create new templates if needed. 

### Modify an existing segment test with new field to resource assertions
 Modify an existing test in [src/test/java/io/github/linuxforhealth/hl7/message](src/test/java/io/github/linuxforhealth/hl7/message) to test the structure of the message.  As an example see [Hl7DFTMessageTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7DFTMessageTest.java).  Copy and create a new message test if needed. 

 Add segment field tests to an existing test in [src/test/java/io/github/linuxforhealth/hl7/segments](src/test/java/io/github/linuxforhealth/hl7/segments) if one exists.  As an example, see [Hl7FinancialInsuranceTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7FinancialInsuranceTest.java).  Copy and create a new segment test if needed. 

 Test cases are vital to help check new function, and also ensure that an added function does not break when other later function is added.  

## 3. Creating a New Message Trigger

### Validate the New Message

You want to start with a valid HL7 message.  Generate or obtain a valid HL7 v2 message/triggering event. The message can be validated using the HAPI libraries packaged with the converter library. The example below demonstrates default HAPI validation within a JUnit5 test case context. _It only validates your proposed message, it does not convert the message or test its conversion._

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

### Review the Official FHIR (HL7 Documentation)
(See explanation above)
### Create new FHIR Resource Templates

Review the existing FHIR resource templates in `src/main/resources/hl7/resource` to determine if the use-case requires a new resource template. 
### Create the HL7 Message Template

Create a new HL7 Message Mapping template in `/src/main/resources/hl7/message` to support the mapping.
Add the new message type to the `supported.hl7.messages` property in [src/main/resources/config.properties](src/main/resources/config.properties).

### Create new FHIR Datatype Template

If a new FHIR datatype is required, add a yaml file to [src/main/resources/datatype](src/main/resources/datatype).

### Implement Test Cases for the Message, Segments and Fields

Implement test cases to validate the HL7 Message to FHIR Resource mapping. Create a new message testin
in [src/test/java/io/github/linuxforhealth/hl7/message](src/test/java/io/github/linuxforhealth/hl7/message) to test the structure of the message (see [Hl7DFTMessageTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7DFTMessageTest.java).  
Create a new tests in [src/test/java/io/github/linuxforhealth/hl7/segments](src/test/java/io/github/linuxforhealth/hl7/segments) to test details of fields. See [Hl7FinancialInsuranceTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7FinancialInsuranceTest.java). 

An good example of End-to-end test cases are found in [/src/test/java/io/github/linuxforhealth/MedicationFHIRConverterTest.java](/src/test/java/io/github/linuxforhealth/MedicationFHIRConverterTest.java) .

# Suggestions for the new developer

The learning curve can feel steep for a new developer.  Suggestions for "ramping up":

1.  Download the project
2. Set up your environment for Java development.  We have contributors using VS Code, Eclipse, and IntelliJ.
3. Run command `gradle clean build`.  This will confirm your libraries are set up correctly and your development environment is working. (Be prepared that testing step has 500+ tests and may take 10 minutes or more to complete.)
4. Select a unit test and run it.  This will confirm your unit test set up is working.  For example, you could try: [Hl7FinancialInsuranceTest.java](src/test/java/io/github/linuxforhealth/hl7/message/Hl7FinancialInsuranceTest.java)
5. Set `localDevEnv = true` in `gradle.properties`.  This will help you see your changes without doing a full build.
6. View the json created from the conversion in the test.  The easiest way to do this is put a breakpoint immediately after the command `.convert`, which returns json. For most tests this is in a common method: `createFHIRBundleFromHL7MessageReturnEntryList` in `ResourceUtils.java`.
7. Use the debugger to step through the asserts of the test.  View the associated data structures.
8. Look at the templates that make your test run.
9. Find the message in [src/main/resources/hl7/message](src/main/resources/hl7/message). e.g. DFT_P03.yml
10. Find the segments created by the message in [src/test/java/io/github/linuxforhealth/hl7/segments](src/test/java/io/github/linuxforhealth/hl7/segments).  For example: Coverage.yml and Organization.yml and RelatedPerson.yml.
11. Observe how the Segment.Fields, such and IN1.17 map to resources.
12. Make a small change to one of the tests.  Change the input HL7 and see how the output works.  Repeat.




