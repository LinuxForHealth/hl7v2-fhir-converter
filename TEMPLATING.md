# LinuxForHealth HL7 to FHIR Converter Templating Guide

## Overview

The converter converts a given HL7 message to a FHIR bundle resource using [yaml](https://yaml.org/) message templates. Each message template defines the FHIR resources generated from a specific HL7 meesage and triggering event.

### HL7 Message Template Structure

A HL7 message template maps one or more HL7 segments to a FHIR resource using the following structure:

```yml
      resourceName: [REQUIRED]
      segment: [REQUIRED]
      resourcePath: [REQUIRED]
      repeats:  [DEFAULT false]
      isReferenced: [DEFAULT false]
      additionalSegments: [DEFAULT empty]
```

| Template Property  | Required/Default | Description
| ------------------ | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |             
| resourceName       | Required         | Name of the FHIR resource. Example: Patient                                                                                                                                                          |
| segment            | Required         | Primary HL7 segment the FHIR resource is sourced from. Example: A Patient FHIR resource is sourced from the HL7 PID segment. If this value is not provided, the FHIR resource will not be generated. |
| resourcePath       | Required         | Relative path to the resource template. Example: resource/Patient                                                                                                                                    |
| repeats            | Default: false   | Indicates if a repeating HL7 segment will generate multiple FHIR resources.                                                                                                                          |
| isReferenced       | Default: false   | Indicates if the FHIR Resource is referenced by other FHIR resources.                                                                                                                                |
| additionalSegments | Default: empty   | List of additional HL7 segment names required to complete the FHIR resource mapping.                                                                                                                 |

FHIR resources are generated in the order listed. FHIR resources with references should follow the resources they reference.

Example of a HL7 message template:

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

### Structure of a FHIR resource template

A FHIR resource template represents a [FHIR resource](https://hl7.org/FHIR/resourcelist.html). The FHIR resource template specifies how FHIR resource field values are generated from HL7 segments or expressions. FHIR Resource templates are stored in: src/main/resources/hl7/resource.

Sample resource template:

```yml
#
# (C) Copyright IBM Corp. 2020
#
# SPDX-License-Identifier: Apache-2.0
#

# Represents data that needs to be extracted for a Patient Resource in FHIR
# reference: https://www.hl7.org/fhir/patient.html
---
resourceType: Patient
id:
  type: STRING
  valueOf: 'UUID.randomUUID()'
  expressionType: JEXL

identifier:
    valueOf: datatype/Identifier
    generateList: true
    expressionType: resource
    specs: PID.3
name:
    valueOf: datatype/HumanName
    generateList: true
    expressionType: resource
    specs: PID.5
gender:
     type: ADMINISTRATIVE_GENDER
     valueOf: PID.8
     expressionType: HL7Spec

birthDate:
     type: DATE
     valueOf: PID.7
     expressionType: HL7Spec
```


``` yml
#
# (C) Copyright IBM Corp. 2020
#
# SPDX-License-Identifier: Apache-2.0
#
---
resourceType: Condition
id:
  type: STRING
  valueOf: 'UUID.randomUUID()'
  expressionType: JEXL


category_x1:
   valueOf: datatype/CodeableConcept_var
   generateList: true
   expressionType: resource
   condition:  $source NOT_NULL
   vars:     
     code: CONDITION_CATEGORY_CODES, $type
     text: $type
     source: PRB.3
   constants:
      type: problem-list-item

category_x2:
   valueOf: datatype/CodeableConcept_var
   generateList: true
   expressionType: resource
   condition:  $source NULL
   vars:     
     code: CONDITION_CATEGORY_CODES, $type
     text: $type
     source: PRB.3
   constants:
      type: encounter-diagnosis


severity:
   valueOf: datatype/CodeableConcept
   generateList: true
   expressionType: resource
   specs: PRB.26
   vars:
     code: PRB.26

code:
   valueOf: datatype/CodeableConcept
   generateList: true
   expressionType: resource
   specs: PRB.3
   vars:
     code: PRB.3


encounter:
    valueOf: datatype/Reference
    expressionType: resource
    specs: $Encounter

subject:
    valueOf: datatype/Reference
    expressionType: resource
    specs: $Patient

onsetDateTime:
     type: DATE_TIME
     valueOf: PRB.16
     expressionType: HL7Spec

stage:
   valueOf: secondary/Stage
   generateList: true
   expressionType: resource
   specs: PRB.14
   vars:
     code: PRB.14
evidence:
   valueOf: secondary/evidence
   generateList: true
   expressionType: resource
   specs: $Observation
   useGroup: true



```

### Expressions Types
The extraction logic for each field can be defined by using expressions. This component supports 4 different type of expressions. All expressions have the following attributes:
* type: DEFAULT - Object <br>
  Represents the class type for the final return value extracted for the field.
* specs: DEFAULT - NONE<br>
  Represents the base value for a resource, if no spec is provided then parents base value would be used as base value for child resource.
  Refer to the section on supported formats for [Specification](#specification).<br>

* default: DEFAULT - NULL<br>
  If extraction of the value fails, then the default value is used.
* required : DEFAULT - false<br>
  If a field is required and cannot be extracted then the resource generation will fail even if other fields were extracted.
* vars: DEFAULT - EMPTY<br>
  List of variables and their value can be provided which may be used during the extraction process. Refer to the section on supported formats for [Variables](#variable).
* condition: DEFAULT - true<br>
  If a condition is provided, then the expression will be resolved only if the condition evaluates to true. Refer to the section on supported formats for [Condition](#condition).
* value: This represents a constant value for the expression value. This attribute is only valid for the SimpleExpression type.
* valueOf: This represents an evaluated expression value. This evaluated value depends on the expression type.
* expressionType: Based on the expression type a valueOf attribute will get evaluated.
* generateList: DEFAULT [false]
  Generates an output list output for all values of the specification. If this value is false, then first valid value of spec would be used for evaluation.
* Constants: DEFAULT - EMPTY<br>
  List of Constants (string values) which can be used during the extraction process.


```yml
      type: String
      valueOf: CX.1
      default: 'abc'
      required: true
      condition: $var1 != null
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
* Preserving white space / empty fields - Blank fields may be used to represent new lines or white space in reports. The user may want to preserve this white space to keep the integrity of the original report. To preserve this white space, the spec string should end with an &. Note that this can be combined (and often will be combined) with the multiple value extraction, either &* or *& is supported.<br> 
  Example: ``OBX.5 *&`` , ``OBX.5 &*``, ``OBX.5 & ``
  


#### Variable
Variables can be used during expression evaluation.  This engine supports defining 3 types of variables:
* SimpleVariable : These are variables where value is extracted from simple [Specification](#specification) or another variable from the context values. Example: ``var1: CWE.1 |CE.1 |CNE.1``
* ExpressionVariable : Value of a variable is extracted by evaluating a java function. Example:  `` low: OBX.7, GeneralUtils.split(low, "-", 0)``
* DataTypeVariable: Value of a variable is extracted from [Specification](#specification) and this value is converted to a particular data type. Example: `` var1: STRING, OBX.2``

Note: BASE_VALUE is reserved for base value provided to an expression during evaluation. Do not use or name variable as BASE_VALUE.

#### Condition
Conditions evaluate to true or false.<br>
Engine supports the following condition types:
* Null check,  example: ``condition:$var1 NULL``
* Not null check, example: ``condition:$var1 NOT_NULL``
* Simple condition,  example: ``condition: $obx2 EQUALS DR``
* Conditions with AND,   example: ``condition: $obx2 EQUALS SN && $obx5.3 EQUALS ':'``
* Conditions with OR, example: ``condition: $obx2 EQUALS TX || $obx2 EQUALS ST``

Conditions can be used to choose between multiple sources of data when mapping to a FHIR type. For example, see how `coding` is set in [CodeableConcept.yml](src/main/resources/hl7/datatype/CodeableConcept.yml). `coding` is set by the either coding_1, coding_2, or coding_3 based on the conditions. The last condition that evaluates to true in the list will create the value.

#### Different types of expressions
* ResourceExpression : This type of expression is used when a field is a data type defined in one of the [data type templates](../master/src/main/resources/hl7/datatype). These data type templates define different [FHIR data types](https://hl7.org/FHIR/datatypes.html).
  Example:

```yml
  identifier:   
    valueOf: datatype/IdentifierCX
    expressionType: resource
    specs: PID.3
```

* ReferenceExpression : This type of expression is used to generate a FHIR reference field/datatype for the current resource.
  Example:

```yml
 performer:
   valueOf: resource/Practitioner
   expressionType: reference
   specs: OBX.16

```

* JEXLExpression: This type of expression is used when a field value needs to be extracted by executing a Java method.

```yml
    type: STRING
    valueOf: 'GeneralUtils.generateName( prefix, given,family, suffix)'
    expressionType: JEXL
    var:
      prefix: STRING, XPN.4
      given: STRING, XPN.2
      family: STRING, XPN.1
      suffix: STRING, XPN.5
```


* Hl7Expression : This type of expression is used when a field value has to be extracted directly from the HL7 segment/field/component.

```yml
given:
     type: STRING
     valueOf: XPN.2
     expressionType: HL7Spec
```

* SimpleExpression : If the field value is constant and no extraction or conversion is required then this expression is used.
  Example 1: Constant value

```yml
text:
  value: 'ABX'

```
Example 2: Value needs to be extracted from a variable.

```yml
text:
   valueOf: $var

```

## Sample output:

```json
{
  "resourceType": "Bundle",
  "id": "32744a5d-8c36-4cd1-93bf-7da16a1bedde",
  "meta": {
    "lastUpdated": "2021-02-20T10:02:45.547+08:00"
  },
  "type": "collection",
  "entry": [ {
    "fullUrl": "urn:uuid:Patient/7425c8c2-50e7-433f-a804-79c0eab69a59",
    "resource": {
      "resourceType": "Patient",
      "id": "7425c8c2-50e7-433f-a804-79c0eab69a59",
      "identifier": [ {
        "type": {
          "coding": [ {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "MR",
            "display": "Medical record number"
          } ],
          "text": "MR"
        },
        "value": "PID1234",
        "assigner": {
          "reference": "Organization/A"
        }
      }, {
        "type": {
          "coding": [ {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "SS",
            "display": "Social Security number"
          } ],
          "text": "SS"
        },
        "value": "1234568965",
        "assigner": {
          "reference": "Organization/USA"
        }
      } ],
      "name": [ {
        "family": "DOE",
        "given": [ "JOHN" ]
      } ],
      "gender": "female",
      "birthDate": "1980-02-02"
    }
  }, {
    "fullUrl": "urn:uuid:Organization/A",
    "resource": {
      "resourceType": "Organization",
      "id": "A",
      "name": "Assigning Authority"
    }
  }, {
    "fullUrl": "urn:uuid:Organization/USA",
    "resource": {
      "resourceType": "Organization",
      "id": "USA",
      "name": "Assigning Authority"
    }
  }, {
    "fullUrl": "urn:uuid:Encounter/f333437c-9a9e-4981-855c-5d31f4f579a4",
    "resource": {
      "resourceType": "Encounter",
      "id": "f333437c-9a9e-4981-855c-5d31f4f579a4",
      "identifier": [ {
        "value": "48390"
      } ],
      "status": "finished",
      "class": {
        "code": "ff"
      },
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
        "reference": "Patient/7425c8c2-50e7-433f-a804-79c0eab69a59"
      },
      "period": {
        "start": "2014-09-12T22:00:00+08:00",
        "end": "2015-02-06T03:17:26+08:00"
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
    "fullUrl": "urn:uuid:Observation/6be3985b-ade4-47f4-b3b0-3e4e8de355c0",
    "resource": {
      "resourceType": "Observation",
      "id": "6be3985b-ade4-47f4-b3b0-3e4e8de355c0",
      "identifier": [ {
        "value": "1234_"
      } ],
      "status": "final",
      "code": {
        "coding": [ {
          "code": "1234"
        } ]
      },
      "subject": {
        "reference": "Patient/7425c8c2-50e7-433f-a804-79c0eab69a59"
      },
      "encounter": {
        "reference": "Encounter/f333437c-9a9e-4981-855c-5d31f4f579a4"
      },
      "issued": "2012-09-12T01:12:30+08:00",
      "performer": [ {
        "reference": "Practitioner/1fbe4699-7821-4176-92cf-648fe799b4f2"
      }, {
        "reference": "Practitioner/191b914f-4eb8-4190-b620-b6bde27141e8"
      }, {
        "reference": "Practitioner/dd9db2fd-ae96-4ca1-8db0-d873568b901a"
      }, {
        "reference": "Practitioner/8a630e35-52c2-43b5-aca3-1485ba986412"
      } ],
      "valueString": "ECHOCARDIOGRAPHIC REPORT"
    }
  }, {
    "fullUrl": "urn:uuid:Practitioner/1fbe4699-7821-4176-92cf-648fe799b4f2",
    "resource": {
      "resourceType": "Practitioner",
      "id": "1fbe4699-7821-4176-92cf-648fe799b4f2",
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
    "fullUrl": "urn:uuid:Practitioner/191b914f-4eb8-4190-b620-b6bde27141e8",
    "resource": {
      "resourceType": "Practitioner",
      "id": "191b914f-4eb8-4190-b620-b6bde27141e8",
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
    "fullUrl": "urn:uuid:Practitioner/dd9db2fd-ae96-4ca1-8db0-d873568b901a",
    "resource": {
      "resourceType": "Practitioner",
      "id": "dd9db2fd-ae96-4ca1-8db0-d873568b901a",
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
    "fullUrl": "urn:uuid:Practitioner/8a630e35-52c2-43b5-aca3-1485ba986412",
    "resource": {
      "resourceType": "Practitioner",
      "id": "8a630e35-52c2-43b5-aca3-1485ba986412",
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
    "fullUrl": "urn:uuid:AllergyIntolerance/44ef3e65-650b-4eff-b662-2057112f49f9",
    "resource": {
      "resourceType": "AllergyIntolerance",
      "id": "44ef3e65-650b-4eff-b662-2057112f49f9",
      "clinicalStatus": {
        "coding": [ {
          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
          "code": "active",
          "display": "Active"
        } ]
      },
      "verificationStatus": {
        "coding": [ {
          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification",
          "code": "confirmed",
          "display": "Confirmed"
        } ]
      },
      "criticality": "unable-to-assess",
      "code": {
        "coding": [ {
          "code": "00000741",
          "display": "OXYCODONE"
        } ],
        "text": "OXYCODONE"
      },
      "patient": {
        "reference": "Patient/7425c8c2-50e7-433f-a804-79c0eab69a59"
      },
      "reaction": [ {
        "manifestation": [ {
          "text": "HYPOTENSION"
        } ]
      } ]
    }
  }, {
    "fullUrl": "urn:uuid:AllergyIntolerance/38e4e23f-593c-406b-bd4d-36b51d580006",
    "resource": {
      "resourceType": "AllergyIntolerance",
      "id": "38e4e23f-593c-406b-bd4d-36b51d580006",
      "clinicalStatus": {
        "coding": [ {
          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
          "code": "active",
          "display": "Active"
        } ]
      },
      "verificationStatus": {
        "coding": [ {
          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification",
          "code": "confirmed",
          "display": "Confirmed"
        } ]
      },
      "criticality": "unable-to-assess",
      "code": {
        "coding": [ {
          "code": "00001433",
          "display": "TRAMADOL"
        } ],
        "text": "TRAMADOL"
      },
      "patient": {
        "reference": "Patient/7425c8c2-50e7-433f-a804-79c0eab69a59"
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
