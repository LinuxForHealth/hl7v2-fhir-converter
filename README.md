# HL7v2-FHIR Converter

FHIR converter is a Java based library that enables converting [Hl7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources.<br>
FHIR converter utilized the open source  [HAPI Library](https://hapifhir.github.io/hapi-hl7v2/) for parsing Hl7 messages and it also utilizes the [HAPI library for FHIR](https://hapifhir.io/) resources to validate the generated FHIR resources.



## Features and Concepts
HL7v2-FHIR converter converts a given HL7 message to FHIR bundle resource using the message templates. These templates are [yaml](https://yaml.org/) files. Each message template defines what all FHIR resources needs to be generated from a particular message. <br>

### Structure of a message template
A message template file consists of list of resources that can be generated from that message type.
For each resource in a template following attributes needs to be defined:
```yml
      resourceName: [REQUIRED]
      segment: [REQUIRED]
      resourcePath: [REQUIRED] 
      order: [DEFAULT 0] 
      repeates:  [DEFAULT false]       
      additionalSegments: [DEFAULT empty] 
```
Attribute description: 
* resourceName:  Name of the resource example: Patient
* segment: Primary segment that this resource depends on. Example patient resource depends on PID segment
* resourcePath: Path for resource file example: Patient resource :src/main/resources/resource/Patient.yml
* order: Order of resource generation, example -- generate Patient resource followed by Encounter and so on.
* repeates:  HL7 have certain segments that repeat and so the convertor needs to generate multiple resources from those segments. Example OBX segment. If this field is set to false then only the first occurance of that segment will be used for resource generation. If this is set to true then multiple resources will be generated from each of the occurrences of that segment.  
* additionalSegments: Any additional segments the resource needs.
Example:
```yml
# FHIR Resources to extract from ADT_A01 message
---
resources:
    - resourceName: Patient
      segment: PID
      resourcePath: resource/Patient
      order: 1
      repeates: false
      additionalSegments:
      
    - resourceName: Encounter
      segment: PV1
      resourcePath: resource/Encounter
      order: 2
      repeates: false
      additionalSegments:
             - PV2
             - EVN
    - resourceName: Observation
      segment: OBX
      resourcePath: resource/Observation
      order: 3
      repeates: true
      additionalSegments:

```

### Structure of a resource template
Resource template represents a [FHIR resource](https://hl7.org/FHIR/resourcelist.html). In order to generate a resource, a resource template for that resource should exist in this location: master/src/main/resources/resource. The resource template defines list of fields and a way to extract values for each of these fields.


Sample resource template:
```yml
# Represents data that needs to be extracted for a Patient Resource in FHIR
# reference: https://www.hl7.org/fhir/patient.html
---
resourceType: Patient
id:
  evaluate: 'UUID.randomUUID()'
identifier:
    type: Array
    reference: datatype/IdentifierCX
    hl7spec: PID.3  
name: 
    type: Array
    reference: datatype/HumanName
    hl7spec: PID.5  
gender: 
     type: ADMINISTRATIVE_GENDER
     hl7spec: PID.8

birthDate:
     type: LOCAL_DATE
     hl7spec: PID.7
```


### Different expressions types 
The extraction logic for each field can be defined by using expressions. This component supports 4 different type of expressions. All expressions have following attributes:
* type: [DEFAULT -String] Class type of the field .
* hl7spec:[DEFAULT - NONE] The value that needs to be extracted usiing the HL7 spec.
* defaultValue: [DEFAULT - NULL]if extraction of the value fails, then the default value can be used.
* required : [DEFAULT - false] If a field is required and cannot be extracted then the resource generation will fail even if other fields were extracted.
* variables:[DEFAULT - EMPTY] List of variables and there value can be provided which can be used during the extraction process.

 Different types of expressions
* ReferenceExpression : This type of expression is used when a field is a data type defined in one of the [data type templates](master/src/main/resources/datatype). These data type templates define different [FHIR data types](https://hl7.org/FHIR/datatypes.html). 
  Example:
  ```yml
  identifier:
    type: Array
    reference: datatype/IdentifierCX
    hl7spec: PID.3 
  ```
* JELXExpression
* ValueExtractionGeneralExpression
* Hl7Expression
* DefaultExpression


### Conversion steps 
In order to convert the message the converter executes following steps:
* Detect the HL7 message type
* Based on the message type select the HL7 message template



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





