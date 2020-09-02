# HL7v2-FHIR

FHIR converter is a Java based library that enables converting [Hl7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources.<br>
FHIR converter utilized the open source  [HAPI Library](https://hapifhir.github.io/hapi-hl7v2/) for parsing Hl7 messages and it also utilizes the [HAPI library for FHIR](https://hapifhir.io/) resources to validate the generated FHIR resources.



## Features and Concepts
HL7v2-FHIR converter converts a given HL7 message to FHIR bundle resource using the message templates. These templates are [yaml](https://yaml.org/) files. Each message template defines what all FHIR resources needs to be generated from a particular message. <br>

### Structure of a message template
A message template file consists of list of resources that can be generated from that message type.
For each resource in a template following attributes needs to be defined:
```yml
      resourceName: [REQUIRED] Name of the resource example: Patient
      segment: [REQUIRED] Primary segment that this resource depends on. Example patient resource depends on PID segment
      resourcePath: [REQUIRED] path for resource file example: [Patient resource](src/main/resources/resource/Patient.yml) 
      order:[DEFAULT 0] Order of resource generation, example -- generate Patient resource followed by Encounter and so on.
      repeates: false [DEFAULT false]
      additionalSegments: [DEFAULT empty]
```





In order to convert the message the converter executes following steps:
* Detect the HL7 message type
* Based on the message type select the HL7 message template
* 


## Installation

What you’ll need
* JDK 8 or later
* Install Gradle


Steps:
```
git clone https://github.ibm.com/pbhallam/hl7v2-FHIR
cd hl7v2-FHIR
gradle build
```





