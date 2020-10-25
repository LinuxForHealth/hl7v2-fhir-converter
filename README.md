# The Open Source HL7 to FHIR converter

FHIR converter is a Java based library that enables converting [HL7v2](https://www.hl7.org/implement/standards/product_section.cfm?section=13) messages to [FHIR](https://hl7.org/FHIR/) resources.<br>
FHIR converter utilized the open source  [HAPI Library](https://hapifhir.github.io/hapi-hl7v2/) for parsing Hl7 messages and it also utilizes the [HAPI library for FHIR](https://hapifhir.io/) resources to validate the generated FHIR resources.

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

### Configuration Options:
Converter supports following configuration options using the config.properties file.
* base.path.resource=
* supported.hl7.messages=ADT\_A01, ORU\_R01, PPR_PC1

If the base.path.resource is empty then the resources (templates) then default resources(templates) are used during the conversion.<br>
supported.hl7.messages lists the messages that are currently supported for conversion.

The converter looks for the config.properties file in the location specified by System property config.home. Example: -Dconfig.home=/path/to/folder/containing/config/test



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
      isReferenced: [DEFAULT false]
      additionalSegments: [DEFAULT empty]
```
Attribute description: 
* resourceName:  Name of the resource example: Patient
* segment: Primary segment that this resource depends on. Example patient resource depends on PID segment. If the primary segment is not in the message then the resource will not be generated.
* resourcePath: Path for resource file relative to the directory that has all the templates. The default path for the templates is src/main/resources/hl7. Example: Patient resource :resource/Patient (no need for file extension).
* repeats:  HL7 have certain segments that repeat and so the converter needs to generate multiple resources from those segments. Example OBX segment. If this field is set to false then only the first occurrence of that segment will be used for resource generation. If this is set to true then multiple resources will be generated from each of the occurrences of that segment.  
* additionalSegments: Any additional segments the resource needs.
* isReferenced : If this resource would be references by other resources then this field should be true. 

Note: The order of resource template is important within a message template. The resources are generated in the order in which they are listed in the message template. If a resource needs to reference another resource then that resource should ve generated before this resource. 

Example of a message template:

```yml
# FHIR Resources to extract from ADT_A01 message
resources:
    - resourceName: Patient
      segment: PID
      resourcePath: resource/Patient
      repeats: false
      isReferenced: true
      additionalSegments:

      
    - resourceName: Encounter
      segment: PV1
      resourcePath: resource/Encounter
      repeats: false
      isReferenced: true
      additionalSegments:
             - PV2
             - EVN
    - resourceName: Observation
      segment: OBX
      resourcePath: resource/Observation
      repeats: true
      isReferenced: true
      additionalSegments:
      
    - resourceName: AllergyIntolerance
      segment: AL1
      resourcePath: resource/AllergyIntolerance
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
    specs: PID.3  
name: 
    resource: datatype/HumanName *
    specs: PID.5  
gender: 
     type: ADMINISTRATIVE_GENDER
     specs: PID.8

birthDate:
     type: DATE
     specs: PID.7
```


``` yml
# Represents data that needs to be extracted for a Condition Resource in FHIR
# reference: https://www.hl7.org/fhir/condition.html
---
resourceType: Condition
id:
  type: STRING
  evaluate: 'UUID.randomUUID()'


category_x1:
   resource: datatype/CodeableConcept_var *
   condition:  $source NOT_NULL
   vars:     
     code: CONDITION_CATEGORY_CODES, $type
     text: $type
     source: PRB.3
   constants:
      type: problem-list-item

category_x2:
   resource: datatype/CodeableConcept_var *
   condition:  $source NULL
   vars:     
     code: CONDITION_CATEGORY_CODES, $type
     text: $type
     source: PRB.3
   constants:
      type: encounter-diagnosis
           

severity:
   resource: datatype/CodeableConcept *
   specs: PRB.26
   vars:
     code: PRB.26
code:
   resource: datatype/CodeableConcept *
   specs: PRB.3
   vars:
     code: PRB.3
     
     
encounter:
    resource: datatype/Reference
    specs: $Encounter
      
subject:
    resource: datatype/Reference
    specs: $Patient

onsetDateTime:
     type: DATE_TIME
     specs: PRB.16 

stage:
   resource: secondary/Stage *
   specs: PRB.14
   vars:
     code: PRB.14
evidence:
   resource: secondary/evidence *
   specs: $Observation
   useGroup: true

```

### Different expressions types 
The extraction logic for each field can be defined by using expressions. This component supports 4 different type of expressions. All expressions have following attributes:
* type: DEFAULT - Object <br>
        Class type final return value extracted for the field.
* specs: DEFAULT - NONE<br>
           Represents the base value for a resource, if no spec is provided then parents base value would be used as base value for child resource.
           The value that needs to be extracted using the HL7 spec. Refer to the section on supported formats for [Specification](Specification).<br>

* defaultValue: DEFAULT - NULL<br>
                If extraction of the value fails, then the default value can be used.
* required : DEFAULT - false<br>
            If a field is required and cannot be extracted then the resource generation will fail even if other fields were extracted.
* vars: DEFAULT - EMPTY<br>
             List of variables and there value can be provided which can be used during the extraction process. Refer to the section on supported formats for [Variables](Variable).
* condition: DEFAULT - true<br>
             If a condition is provided then the expression will be resolved only if condition evaluates to true. Refer to the section on supported formats for [Condition](Condition).
* Constants: DEFAULT - EMPTY<br>
              List of Constants (string values) which can be used during the extraction process.


```yml
      type: String
      specs: CX.1
      defaultValue: 'abc'
      required: true 
      condition: var1 != null
      vars:
        var1: CX.1
        var2: CX.2
      constants:
          code: 'some code'

 ```
#### Specification
Specification represents the base value for a expression. There are two types of Specifications - 
* SimpleSpecification  -- Represents simple specification that can be extracted from context values. Example: specs: $Patient. Note BASE_VALUE is reserved for base value provided to an expression during evaluation. Do not use or name variable as BASE_VALUE.
* HL7Specification -- Represents specification for extracting values from HL7 message.

##### HL7Specification

HL7Specification represents expression that helps to identify the value to extracted from the HL7 message.<br>
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
* SimpleVariable : These are variables where value is extracted from simple [Specification](Specification) or another variable from the context values. Example: ``var1: CWE.1 |CE.1 |CNE.1``
* ExpressionVariable : Value of a variable is extracted by evaluating a java function. Example:  `` low: OBX.7, GeneralUtils.split(low, "-", 0)``
* DataTypeVariable: Value of a variable is extracted from [Specification](Specification) and this value is converted to a particular data type. Example: `` var1: STRING, OBX.2``

Note: BASE_VALUE is reserved for base value provided to an expression during evaluation. Do not use or name variable as BASE_VALUE.

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
    specs: PID.3 
```
  
* ReferenceExpression : This type of expression is used when a field  references a FHIR resource which has to be first generated based on provided hl7spec data. Then in thhe current resource this FHIR resource is referenced using Reference data type.
Example:

```yml
 performer: 
   reference: resource/Practitioner
   specs: OBX.16

```

* JELXExpression: This type of expression is used when a field value needs to be extracted by executing a Java method.

```yml
    type: STRING
    evaluate: 'GeneralUtils.generateName( prefix, given,family, suffix)'
    var:
      prefix: STRING, XPN.4
      given: STRING, XPN.2
      family: STRING, XPN.1
      suffix: STRING, XPN.5
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
     specs: XPN.2
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

## Sample output:

```json
{
  "resourceType": "Bundle",
  "id": "cf56324b-e43a-4cc7-8768-eaba5eb61214",
  "meta": {
    "lastUpdated": "2020-10-24T15:59:07.418+08:00",
    "source": "Message: ADT_A01, Message Control Id: 102"
  },
  "type": "collection",
  "entry": [ {
    "resource": {
      "resourceType": "Patient",
      "id": "0f61c0c7-1d9e-46de-9bba-2ee908582804",
      "identifier": [ {
        "type": {
          "coding": [ {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "MR",
            "display": "Medical record number"
          } ],
          "text": "MR"
        },
        "system": "A",
        "value": "PID1234"
      }, {
        "type": {
          "coding": [ {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "SS",
            "display": "Social Security number"
          } ],
          "text": "SS"
        },
        "system": "USA",
        "value": "1234568965"
      } ],
      "name": [ {
        "family": "DOE",
        "given": [ "JOHN" ]
      } ],
      "gender": "female",
      "birthDate": "1980-02-02"
    }
  }, {
    "resource": {
      "resourceType": "Encounter",
      "id": "0c2ff935-39e5-43e9-a4c3-6df4e281cc47",
      "identifier": [ {
        "value": "48390"
      } ],
      "status": "finished",
      "type": [ {
        "text": "EL"
      } ],
      "serviceType": {
        "coding": [ {
          "system": "http://terminology.hl7.org/CodeSystem/v2-0069",
          "code": "MED",
          "display": "Medical Service"
        } ],
        "text": "MED"
      },
      "subject": {
        "reference": "Patient/0f61c0c7-1d9e-46de-9bba-2ee908582804"
      },
      "period": {
        "start": "2014-09-12T22:00:00",
        "end": "2015-02-06T03:17:26"
      },
      "length": {
        "value": 210557,
        "unit": "Minutes"
      },
      "hospitalization": {
        "preAdmissionIdentifier": {
          "value": "ABC"
        },
        "specialCourtesy": [ {
          "text": "E"
        } ],
        "specialArrangement": [ {
          "coding": [ {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0009",
            "code": "B6",
            "display": "Pregnant"
          } ],
          "text": "B6"
        } ]
      }
    }
  }, {
    "resource": {
      "resourceType": "Observation",
      "id": "e56b6d84-5418-4fc6-b172-48de133c364e",
      "identifier": [ {
        "type": {
          "coding": [ {
            "code": "1234"
          } ]
        },
        "value": "1234"
      } ],
      "status": "final",
      "code": {
        "coding": [ {
          "code": "1234"
        } ]
      },
      "subject": {
        "reference": "Patient/0f61c0c7-1d9e-46de-9bba-2ee908582804"
      },
      "encounter": {
        "reference": "Encounter/0c2ff935-39e5-43e9-a4c3-6df4e281cc47"
      },
      "issued": "2012-09-12T01:12:30",
      "performer": [ {
        "reference": "Practitioner/05dae019-4686-4140-a001-07f7f2ca9260"
      }, {
        "reference": "Practitioner/86be217b-7cdf-4044-80e6-b8469a5660fa"
      }, {
        "reference": "Practitioner/3cc53aeb-ccd1-494d-a44d-6017942b616e"
      }, {
        "reference": "Practitioner/28bb5b0d-4ab3-4157-8ca2-007cb6bda84c"
      } ],
      "valueString": "ECHOCARDIOGRAPHIC REPORT"
    }
  }, {
    "resource": {
      "resourceType": "Practitioner",
      "id": "05dae019-4686-4140-a001-07f7f2ca9260",
      "identifier": [ {
        "value": "2740"
      } ],
      "name": [ {
        "text": "Janetary TRDSE",
        "family": "TRDSE",
        "given": [ "Janetary" ]
      } ]
    }
  }, {
    "resource": {
      "resourceType": "Practitioner",
      "id": "86be217b-7cdf-4044-80e6-b8469a5660fa",
      "identifier": [ {
        "value": "2913"
      } ],
      "name": [ {
        "text": "Darren MRTTE",
        "family": "MRTTE",
        "given": [ "Darren" ]
      } ]
    }
  }, {
    "resource": {
      "resourceType": "Practitioner",
      "id": "3cc53aeb-ccd1-494d-a44d-6017942b616e",
      "identifier": [ {
        "value": "3065"
      } ],
      "name": [ {
        "text": "Paul MGHOBT",
        "family": "MGHOBT",
        "given": [ "Paul" ]
      } ]
    }
  }, {
    "resource": {
      "resourceType": "Practitioner",
      "id": "28bb5b0d-4ab3-4157-8ca2-007cb6bda84c",
      "identifier": [ {
        "value": "4723"
      } ],
      "name": [ {
        "text": "Robert LOTHDEW",
        "family": "LOTHDEW",
        "given": [ "Robert" ]
      } ]
    }
  }, {
    "resource": {
      "resourceType": "AllergyIntolerance",
      "id": "9765d5ab-a8f0-406d-8798-ea6102e9bba5",
      "code": {
        "coding": [ {
          "code": "00000741",
          "display": "OXYCODONE"
        } ],
        "text": "OXYCODONE"
      },
      "patient": {
        "reference": "Patient/0f61c0c7-1d9e-46de-9bba-2ee908582804"
      },
      "reaction": [ {
        "manifestation": [ {
          "text": "HYPOTENSION"
        } ]
      } ]
    }
  }, {
    "resource": {
      "resourceType": "AllergyIntolerance",
      "id": "5cf3b829-556f-4d25-a674-86b57a6f5a85",
      "code": {
        "coding": [ {
          "code": "00001433",
          "display": "TRAMADOL"
        } ],
        "text": "TRAMADOL"
      },
      "patient": {
        "reference": "Patient/0f61c0c7-1d9e-46de-9bba-2ee908582804"
      },
      "reaction": [ {
        "manifestation": [ {
          "text": "SEIZURES"
        }, {
          "text": "VOMITING"
        } ]
      } ]
    }
  } ]
}

```




