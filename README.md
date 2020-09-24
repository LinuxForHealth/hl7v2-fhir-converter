# FHIR Converter

FHIR converter is a Java based library that enables converting [Hl7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources.<br>
FHIR converter utilized the open source  [HAPI Library](https://hapifhir.github.io/hapi-hl7v2/) for parsing Hl7 messages and it also utilizes the [HAPI library for FHIR](https://hapifhir.io/) resources to validate the generated FHIR resources.



## Features and Concepts of HL7 to FHIR conversion
HL7v2-FHIR converter converts a given HL7 message to FHIR bundle resource using the message templates. These templates are [yaml](https://yaml.org/) files. Each message template defines what all FHIR resources needs to be generated from a particular message. <br>

### Structure of a message template
A message template file consists of list of resources that can be generated from that message type.
For each resource in a template following attributes needs to be defined:

```yml
      resourceName: [REQUIRED]
      segment: [REQUIRED]
      resourcePath: [REQUIRED]
      repeats:  [DEFAULT false]
      additionalSegments: [DEFAULT empty]
```
Attribute description: 
* resourceName:  Name of the resource example: Patient
* segment: Primary segment that this resource depends on. Example patient resource depends on PID segment. If the primary segment is not in the message then the resource will not be generated.
* resourcePath: Path for resource file relative to the directory that has all the templates. The default path for the templates is src/main/resources/hl7. Example: Patient resource :resource/Patient (no need for file extension).
* repeats:  HL7 have certain segments that repeat and so the converter needs to generate multiple resources from those segments. Example OBX segment. If this field is set to false then only the first occurrence of that segment will be used for resource generation. If this is set to true then multiple resources will be generated from each of the occurrences of that segment.  
* additionalSegments: Any additional segments the resource needs.

Example of a message template:

```yml
# FHIR Resources to extract from ADT_A01 message

resources:

    - resourceName: Patient
      segment: PID
      resourcePath: resource/Patient
      repeats: false
      additionalSegments:
      
    - resourceName: Encounter
      segment: PV1
      resourcePath: resource/Encounter
      repeats: false
      additionalSegments:
             - PV2
             - EVN
    - resourceName: Observation
      segment: OBX
      resourcePath: resource/Observation
      repeats: true
      additionalSegments:

```

### Structure of a resource template
Resource template represents a [FHIR resource](https://hl7.org/FHIR/resourcelist.html). In order to generate a resource, a resource template for that resource should exist in this location: src/main/resources/hl7/resource. The resource template defines list of fields and a way to extract values for each of these fields.


Sample resource template:

```yml
# Represents data that needs to be extracted for a Patient Resource in FHIR
# reference: https://www.hl7.org/fhir/patient.html
---
resourceType: Patient
id:
  type: STRING
  evaluate: 'UUID.randomUUID()'
  
identifier:
    resource: datatype/Identifier *
    hl7spec: PID.3  
name: 
    resource: datatype/HumanName *
    hl7spec: PID.5  
gender: 
     type: ADMINISTRATIVE_GENDER
     hl7spec: PID.8

birthDate:
     type: DATE
     hl7spec: PID.7
```


### Different expressions types 
The extraction logic for each field can be defined by using expressions. This component supports 4 different type of expressions. All expressions have following attributes:
* type: DEFAULT - Object <br>
        Class type final return value extracted for the field.
* hl7spec: DEFAULT - NONE<br>
           The value that needs to be extracted using the HL7 spec. Refer to the section on supported formats for [Specification](Specification).
* defaultValue: DEFAULT - NULL<br>
                If extraction of the value fails, then the default value can be used.
* required : DEFAULT - false<br>
            If a field is required and cannot be extracted then the resource generation will fail even if other fields were extracted.
* variables: DEFAULT - EMPTY<br>
             List of variables and there value can be provided which can be used during the extraction process. Refer to the section on supported formats for [Variables](Variable).
* condition: DEFAULT - true<br>
             If a condition is provided then the expression will be resolved only if condition evaluates to true. Refer to the section on supported formats for [Condition](Condition).


```yml
      type: String
      hl7spec: CX.1
      defaultValue: 'abc'
      required: true 
      condition: var1 != null
      variables:
        var1: CX.1
        var2: CX.2

 ```
#### Specification
Specification represents expression that helps to identify the value to extracted from the HL7 message.<br>
The specification expression has the following format :
* Single SPEC - where spec can be <br>
    - SEGMENT 
    - SEGMENT.FIELD
    - SEGMENT.FIELD.COMPONENT
    - FIELD
    - FIELD.COMPONENT
    - FIELD.COMPONENT.SUBCOMPONENT<br>
Example:  `` OBX, OBX.1, CWE, CWE.1, OBX.3.1``
* Multiple SPEC - Where each single spec is separated by |, where | represents "or'. If the value from first spec is extracted, then the remaining specs are ignored.<br>
Example: ``OBX.1 |OBX.2|OBX.3`` , if OBX.1 is null then only OBX.2 will be extracted.
* Multiple value extraction - In HL7 several fields can have repeated values, so extract all repetition for that field the spec string should end with *.<br>
 Example: ``PID.3 *`` , ``OBX.1 |OBX.2 |OBX.3 *``


#### Variable
Variables can be used during expression evaluation.  This engine supports defining 3 types of variables:
* SimpleVariable : These are variables where value is extracted from simple [HL7Spec](Specification) or another variable from the context values. Example: ``var1: CWE.1 |CE.1 |CNE.1``
* ExpressionVariable : Value of a variable is extracted by evaluating a java function. Example:  `` low: OBX.7, GeneralUtils.split(low, "-", 0)``
* DataTypeVariable: Value of a variable is extracted from [HL7Spec](Specification) and this value is converted to a particular data type. Example: `` var1: STRING, OBX.2``

#### Condition
Conditions evaluate to true or false.<br>
Engine supports the following condition types:
* Null check,  example: ``condition:$var1 NULL``
* Not null check, example: ``condition:$var1 NOT_NULL``
* Simple condition,  example: ``condition: $obx2 EQUALS DR``
* Conditions with AND,   example: ``condition: $obx2 EQUALS SN && $obx5.3 EQUALS ':'``
* Conditions with OR, example: ``condition: $obx2 EQUALS TX || $obx2 EQUALS ST``

#### Different types of expressions
* ResourceExpression : This type of expression is used when a field is a data type defined in one of the [data type templates](master/src/main/resources/hl7/datatype). These data type templates define different [FHIR data types](https://hl7.org/FHIR/datatypes.html). 
Example:
  
```yml
  identifier:
    type: Array
    resource: datatype/IdentifierCX
    hl7spec: PID.3 
```
  
* ReferenceExpression : This type of expression is used when a field  references a FHIR resource which has to be first generated based on provided hl7spec data. Then in thhe current resource this FHIR resource is referenced using Reference data type.
Example:

```yml
 performer: 
   reference: resource/Practitioner
   hl7spec: OBX.16

```

* JELXExpression: This type of expression is used when a field value needs to be extracted by executing a Java method.

```yml
    type: STRING
    evaluate: 'GeneralUtils.generateName( prefix, given,family, suffix)'
    var:
      prefix: STRING XPN.4
      given: STRING XPN.2
      family: STRING XPN.1
      suffix: STRING XPN.5
```

* ValueExtractionGeneralExpression : This type of expression is used when a field value can be extracted from a field of another resource or variable.

```yml
identifier:
 fetch: '$ref-type:identifier'
```
* Hl7Expression : This type of expression is used when a field value has to be extracted directly from the HL7 segment/field/component.

```yml
given: 
     type: STRING
     hl7spec: XPN.2
```

* SimpleExpression : If the field value is constant and no extraction or conversion is required then this expression is used.
Example 1: Constant value

```yml
code: 'ABX'

```
Example 2: Value needs to be extracted from a variable.

```yml
code: $var

```
## Usage


### Installation

What you’ll need
* JDK 8 or later
* Install Gradle


Steps:

```
git clone git@github.com:LinuxForHealth/hl7v2-fhir-converter.git
cd hl7v2-fhir-converter
gradle build

```

### Converting HL7v2 message to FHIR resources

In order to convert a Hl7 message to FHIR resource, create a new instance of the class FHIRConverter and invoke the function  convert and pass the hl7message data (file contents).

```
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String output= ftv.convert(hl7message); // generated a FHIR output
```
Sample output:

```json
{"resourceType": "Bundle",
  "meta": {
    "tag": [ {
      "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
      "code": "SUBSETTED",
      "display": "Resource encoded in summary mode"
    } ]
  },
  "type": "collection",
  "entry": [ {
    "resource": {
      "resourceType": "Patient",
      "id": "ee746e04-80b1-4822-9c63-9e3bffe8cee3",
      "meta": {
        "tag": [ {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
          "code": "SUBSETTED",
          "display": "Resource encoded in summary mode"
        } ]
      },
      "identifier": [ {
        "system": "1231",
        "value": "ADTNew"
      } ],
      "name": [ {
        "text": "familyname",
        "family": "familyname"
      } ],
      "gender": "female",
      "birthDate": "1983-02-02"
    }
  }, {
    "resource": {
      "resourceType": "Encounter",
      "id": "a0a44360-0543-4a01-abbe-8cc16d9aa5ee",
      "meta": {
        "tag": [ {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
          "code": "SUBSETTED",
          "display": "Resource encoded in summary mode"
        } ]
      },
      "identifier": [ {
        "value": "48390"
      } ],
      "status": "finished",
      "serviceType": {
        "coding": [ {
          "code": "MED"
        } ],
        "text": "MED"
      },
      "period": {
        "start": "2014-09-12T22:00:00",
        "end": "2000-02-06T03:17:26"
      },
      "length": {
        "value": 7679202,
        "unit": "Minutes"
      },
      "hospitalization": {
        "preAdmissionIdentifier": {
          "value": "ABC"
        }
      }
    }
  }, {
    "resource": {
      "resourceType": "Observation",
      "id": "5834de56-ad14-472a-9302-0b8f7458beb8",
      "meta": {
        "tag": [ {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
          "code": "SUBSETTED",
          "display": "Resource encoded in summary mode"
        } ]
      },
      "identifier": [ {
        "value": "1234"
      } ],
      "status": "final",
      "code": {
        "coding": [ {
          "code": "1234"
        } ],
        "text": "1234"
      },
      "issued": "2012-09-12T01:12:30",
      "performer": [ {
        "reference": "438ab808-0c21-4969-90e0-b0748fe098da",
        "type": "Practitioner"
      } ],
      "valueString": "First line: ECHOCARDIOGRAPHIC REPORT"
    }
  }, {
    "resource": {
      "resourceType": "Practitioner",
      "id": "438ab808-0c21-4969-90e0-b0748fe098da",
      "meta": {
        "tag": [ {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
          "code": "SUBSETTED",
          "display": "Resource encoded in summary mode"
        } ]
      },
      "identifier": [ {
        "value": "2740"
      } ],
      "name": [ {
        "text": "Jane FamilyName",
        "family": "FamilyName",
        "given": [ "Jane" ]
      } ]
    }
  } ]
}
```




