# LinuxForHealth HL7 to FHIR Converter

The LinuxForHealth HL7 to FHIR converter is a Java based library that enables converting [HL7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources in a declarative and configuration based manner.

Message parsing and modeling is supported using the "HAPI" libraries for [HL7](https://hapifhir.github.io/hapi-hl7v2/) and [FHIR](https://hapifhir.io/) respectively.

The converter supports the following message types/events:
* ADT_A01 - Patient Administration: Admit/Visit Notification
* OMP_O09 - Pharmacy/Treatment Order
* ORU_R01 - Observation Reporting: Observation and Result Transmission (Laboratory)
* PPR_PC1 - Patient Problem: Add Problem
* RDE_O11 - Pharmacy/Treatment Encoded Order
* RDE_O25 - Pharmacy/Treatment Refill Authorization Request
* VXU_V04 - Vaccination: Update Vaccination Record

The converter supports the following message segments:
* AL1 - Patient Allergy Information
* EVN - Event Type
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
* RXO - Pharmacy/Treatment Order
* RXR - Pharmacy/Treatment Route
* SPM - Specimen

If you need another message type/event . . .  contributions are welcome! We welcome [Pull Requests](https://github.com/LinuxForHealth/hl7v2-fhir-converter/pulls)!

## Development Quickstart

The FHIR Converter has the following dependencies:

* JDK 8 or later
* Gradle 

Note: The FHIR Converter includes a Gradle Wrapper, so a local Gradle install is not required.

Clone and build the project:
```
git clone git@github.com:LinuxForHealth/hl7v2-fhir-converter.git
cd hl7v2-fhir-converter
./gradlew build
```

## Using The Converter In A Java Application

The HL7 to FHIR converter library is available as a maven dependency. 

Library Coordinates
```
groupId = io.github.linuxforhealth
artifactId = hl7v2-fhir-converter
version = 1.0.9
```

Maven dependency
```
<dependency>
  <groupId>io.github.linuxforhealth</groupId>
  <artifactId>hl7v2-fhir-converter</artifactId>
  <version>1.0.9</version>
</dependency>
```

Gradle dependency:
```
    implementation 'io.github.linuxforhealth:hl7v2-fhir-converter:1.0.9'
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
| base.path.resource      | Path to resource templates (optional). If not specified the library's default resources under src/resources are used.                                                             | /opt/converter/resources        |
| supported.hl7.messages  | Comma delimited list of hl7 message/event types.                                                                                                                                  | ADT_A01, ORU_R01, PPR_PC1       |
| default.zoneid          | ISO 8601 timezone offset (optional). The zoneid is applied to translations when the target FHIR resource field requires a timezone, but the source HL7 field does not include it. | +08:00                          |
| additional.conceptmap   | Path to additional concept map configuration. Concept maps are used for mapping one code system to another.                                                                       | /opt/converter/concept-map.yaml |

The config.properties file location is set using the System property, `config.home`

```
-Dconfig.home=/opt/converter/config
```

## Additional Documentation
* [Templating Configuration](./TEMPLATING.md)
* [Development Guide](./DEVELOPMENT.md)
