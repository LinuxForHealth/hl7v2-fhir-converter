# LinuxForHealth HL7 to FHIR Converter

The LinuxForHealth HL7 to FHIR converter is a Java based library that enables converting [HL7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources in a declarative and configuration based manner.

Message parsing and modeling is supported using the "HAPI" libraries for [HL7](https://hapifhir.github.io/hapi-hl7v2/) and [FHIR](https://hapifhir.io/) respectively.

The converter supports the following message types/events:
* ADT_A01 - Patient Administration: Admit/Visit Notification
* ADT_A03 - Patient Administration: Discharge/End Visit  
* ADT_A08 - Patient Administration: Update Patient Information
* ADT_A34 - Patient Administration: Merge Patient Information - Patient ID Only
* ADT_A40 - Patient Administration: Merge Patient - Patient Identifier List
* DFT_P03 - Post Detail Financial Transaction (does not convert FT1)
* MDM_T02 - Original Document Notifcication and Content
* MDM_T06 - Document Addendum Notification and Content
* OMP_O09 - Pharmacy/Treatment Order
* ORM_O01 - General Order Message
* ORU_R01 - Observation Reporting: Observation and Result Transmission (Laboratory)
* PPR_PC1 - Patient Problem: Add Problem
* RDE_O11 - Pharmacy/Treatment Encoded Order
* RDE_O25 - Pharmacy/Treatment Refill Authorization Request
* VXU_V04 - Vaccination: Update Vaccination Record

The converter supports the following message segments:
* AL1 - Patient Allergy Information
* DG1 - Diagnosis
* EVN - Event Type
* IN1 - Insurance
* MRG - Merge Patient Information
* MSH - Message Header
* NTE - Notes and Comments
* OBR - Observation Request
* OBX - Observation/Result
* ORC - Common Order
* PD1 - Patient Additional Demographic
* PID - Patient Identification
* PRB - Problem Details
* PV1 - Patient Visit
* PV2 - Patient Visit - Additional Information
* RXA - Pharmacy/Treatment Administration
* RXC - Pharmacy/Treatment Component Order
* RXE - Pharmacy/Treatment Encoded Order
* RXO - Pharmacy/Treatment Order (Ignored for RDE messages; RXE used instead)
* RXR - Pharmacy/Treatment Route
* SPM - Specimen
* TXA - Transcription Document Header

If you need another message type/event . . .  contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md)!

## Additional Documentation
* [Templating Configuration](./TEMPLATING.md)
* [Development Guide](./DEVELOPMENT.md)
* [HL7 to FHIR Conversion Design](./HL7FHIR.md)
* [Techniques](./TECHNIQUES.md)
* [Coding and Testing Best Practices](./BEST_PRACTICES.md)

## Development Quickstart

The FHIR Converter has the following dependencies:

* JDK 11 or later (upgraded on 10/28/21, version 1.0.12 and earlier supported Java 8)
* Gradle 

Note: The FHIR Converter includes a Gradle Wrapper, so a local Gradle install is not required.

Clone and build the project:
```
git clone git@github.com:LinuxForHealth/hl7v2-fhir-converter.git
cd hl7v2-fhir-converter
./gradlew build
```

## Using the Converter in a Java Application

The HL7 to FHIR converter library is available as a maven dependency. 

Library Coordinates
```
groupId = io.github.linuxforhealth
artifactId = hl7v2-fhir-converter
version = 1.0.10
```

Maven dependency
```
<dependency>
  <groupId>io.github.linuxforhealth</groupId>
  <artifactId>hl7v2-fhir-converter</artifactId>
  <version>1.0.10</version>
</dependency>
```

Gradle dependency:
```
    implementation 'io.github.linuxforhealth:hl7v2-fhir-converter:1.0.10'
```     

Instantiate and execute the converter
```
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String output= ftv.convert(hl7message); // generated a FHIR output
```

## Converter Configuration:

The converter configuration file, config.properties, supports the following settings
 
| Property Name           | Description                                                                                                                                                                       | Example Value                   |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| base.path.resource      | Path to resource templates (optional). If not specified the library's default resources under src/resources are used.                                                            | /opt/converter/resources        |
| supported.hl7.messages  | Comma delimited list of hl7 message/event types. An asterisk `*` may be used to indicate all messages found in sub-directory `/hl7/messages` under the `base.path.resource` and sub-directory `/hl7/messages` under `additional.resources.location` are supported. If not specified, defaults to `*`.                                                                                                                             | ADT_A01, ORU_R01, PPR_PC1       |
| default.zoneid          | ISO 8601 timezone offset (optional). The zoneid is converted to java.time.ZoneId and applied to translations when the target FHIR resource field requires a timezone, but the source HL7 field does not include it.  Requires a valid string value for java.time.ZoneId. | +08:00                          |
| additional.conceptmap   | Path to additional concept map configuration. Concept maps are used for mapping one code system to another.                                                                       | /opt/converter/concept-map.yaml |
| additional.resources.location  | Path to additional resources. These supplement those `base.path.resource`.                                                                         | /opt/supplemental/resources|

### HL7 Converter Configuration Property Location

The config.properties file location is searched in the following order:

* HL7CONVERTER_CONFIG_HOME environment variable is checked first
* hl7converter.config.home system property is checked next 
   
   ``` -Dhl7converter.config.home=/opt/converter/config_home_folder/```

* Lastly, the local classpath resource folder will be searched for config.properties

## Converter Runtime Parameters

The converter allows passing of certain parameters at run time through the options.
 
| Parameter Name           | Description                                                                                                                                                                       | Example Call on Options Creation                    |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| ZoneIdText     | ZoneId override for the ISO 8601 timezone offset. Overrides default.zoneid in config.properties. Requires a valid ZoneId text value, which is converted to a java.time.ZoneId.            | options.withZoneIdText("+07:00")      |
| Property (Key/Value)  | A string property expressed as a key / value pair.  Properties become available as variables to the templates.  A property `TENANT` with value `myTenantId` is utilized in templates as `$TENANT`.             | options.withProperty("TENANT","myTenantId")      |


### PHI (Protected Health Information)

Since this converter is used in production environments using real patient data it can not log or print out anything that may contain PHI data. We will be stripping out all debug log statements as part of the build. This allows developers to use these debug statements to debug issues with santized unit test data.

* Please do not add any print outs (ie system.out.println etc). Use the logger.
* When adding / editing current or future error, warn, and info log statements please be careful to not add any data structures that might contain PHI. If you aren't sure then make the log statement debug so it's stripped out.

### Local development gradle build switch (localDevEnv)

Before the gradle build step where the compile happens the gradle build copies the _src_ directory to the _target_ directoy and sets the gradle build sourcesets to the target directory. Then it strips out all LOGGER.debug statements. This means the compile/build runs off the target directory where the debug statements have been stripped. However if you are running locally you still want the build to run off the _src_ directory or otherwise you must build after each change to get it copied to the _target_ directory. There is a flag in the gradle.properties named _localDevEnv_ which should always be set to false in GIT but a developer can override this flag to true and the local build will keep the sourcesets tied to the _src_ directory.
