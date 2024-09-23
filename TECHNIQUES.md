# LinuxForHealth HL7 to FHIR Converter Hints and Techniques

## Overview

Additional information, techniques, and hints about development of templates, java exits, and other enhancements to the converter.

## Getting Java control via data Types

The converter extracts information via a template and converts to a data type, such as `STRING` and `BOOLEAN`.  You can take advantage of this and create custom data types which can be called for processing of unique inputs.  Before the input is used, your custom data type processor will be invoked.

As an example, type `RELIGIOUS_AFFILIATION_CC` was added to `SimpleDataTypeMapper.java` and mapped to the data handler method `RELIGIOUS_AFFILIATION_FHIR_CC` in `SimpleDataValueResolver.java`

The template reference passes input PID.17 through custom data type `RELIGIOUS_AFFILIATION_CC`:
```yaml
extension_2:
  condition: $valCodeableConcept NOT_NULL
  valueOf: extension/Extension_CodeableConcept
  generateList: true
  expressionType: resource
  vars:
    url: String, GeneralUtils.getExtensionUrl("religion")
    valCodeableConcept: RELIGIOUS_AFFILIATION_CC, PID.17
```

The mapping.
```
java
RELIGIOUS_AFFILIATION_CC(SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC),
```

The resolver takes as input a value and returns a `CodeableConcept` object, which can be used in the template yaml.  In the example code, the input value is converted to a string and the HAPI FHIR `V3ReligiousAffiliation.class` is used to lookup the code.  With the code, the V3ReligiousAffiliation is found from the code and is used to create the CodeableConcept.
```java
    public static final ValueExtractor<Object, CodeableConcept> RELIGIOUS_AFFILIATION_FHIR_CC = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, V3ReligiousAffiliation.class);
        if (code != null) {
            V3ReligiousAffiliation rel = V3ReligiousAffiliation.fromCode(code);
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.addCoding(new Coding(rel.getSystem(), code, rel.getDisplay()));
            codeableConcept.setText(rel.getDisplay());
            return codeableConcept;
        } else {
            return null;
        }
    };
```

The lookup of the FHIR code is in file `v2ToFFhirMapping.yml` and it is important to note that the lookup section is the same as the class passed in `getFHIRCode(val, V3ReligiousAffiliation.class)`

```yaml
V3ReligiousAffiliation:
  # Agnostic -> Agnosticism
  AGN: 1004
  # Atheist -> Athiesm
  ATH: 1007
  # Baha'i -> Babi & Baha'I faiths
  BAH: 1008
  <etc>
  ```


## Getting Java control via JEXL calls to classes 

Evaluation of JEXL expressions can include the evaluation of a custom method.  You can use this to get control and evaluate an input before returning from the JEXL evaluation.

As an example, address district has specialized rules for when Parish should be used.  The Address template evaluates a JEXL expression that calls to the public `getAddressDistrict` which is in file `Hl7RelatedGeneralUtils.java`, which is mapped to `GeneralUtils`.  Variables are collected and input to method.  

`Address.yml`
```yaml
district:
     type: STRING
     valueOf: 'GeneralUtils.getAddressDistrict( patientCounty, addressCountyParish, patient)'
     expressionType: JEXL
     vars:
          patientCounty: String, PID.12
          addressCountyParish: String, XAD.9
          patient: PID
```
`Hl7RelatedGeneralUtils.getAddressDistrict` 

```java
public static String getAddressDistrict(String patientCountyPid12, String addressCountyParishPid119, Object patient) {
        LOGGER.info("getAddressCountyParish for {}", patient);

        String returnDistrict = addressCountyParishPid119;
        if (returnDistrict == null) {
            Segment pidSegment = (Segment) patient;
            try {
                Type[] addresses = pidSegment.getField(11);
                if (addresses.length == 1) {
                    returnDistrict = patientCountyPid12;
                }
            } catch (HL7Exception e) {
                // Do nothing.  Just eat the error.
                // Let the initial value stand
            }
        }
        return returnDistrict;
    }
```

## Time

Time conversion and formatting utilities are available, but they must be called in a thread-safe way.  The time ZoneId may be specified as a default in `config.properties` value `default.zoneid=+08:00` or passed in via runtime context using `.withZoneIdText("-05:00")`.  The value of the input `ZoneIdText` is available as the system variable `$ZONEID` and should be used in all time conversions where a time zone is needed. `GeneralUtils.dateTimeWithZoneId` takes an input HL7 time value field and converts it using the ZoneIdText of the current context.  The timezone should not be stored by any static method, accessing the ZoneIdText via the system variable makes the time processing threadsafe.
```yaml
time:
  type: STRING
  valueOf: "GeneralUtils.dateTimeWithZoneId(dateTimeIn,ZONEID)"
  expressionType: JEXL
  vars:
    dateTimeIn: NTE.6 | NTE.7
```
The rules for determining a time zone for a date time value are:
1. If a DateTime from HL7 contains it's own ZoneId in the DateTime, use it.
1. If it has no ZoneId, and the context ZoneIdText is set, use that.
1. If it has no ZoneId, and no context ZoneIdText, but there is a config ZoneId, use that.
1. If it has no ZoneId, and no context ZoneIdText, and no config ZoneId, use the local timezone (whichis the ZoneId of the server where the process is running).

## YAML Hints

Hints about the ways syntax and references work in the YAML files

### Condition test variables, not segment fields

Testing the segment fields directly in conditions doesn't work. Instead you must create a var for the template field and test the var.

Not this:
```yaml
telecom_1:
    condition: PID.14 NOT_NULL    
    valueOf: datatype/ContactPoint
    generateList: true
    expressionType: resource
    specs: PID.14
    constants: 
       use: "work"
```

Do this:
```yaml
telecom_1:
    condition: $pid14 NOT_NULL    
    valueOf: datatype/ContactPoint
    generateList: true
    expressionType: resource
    specs: PID.14
    vars:
       pid14: PID.14
    constants: 
       use: "work"
```

**Note**: You can now test fields, components and sub-components
from custom segments (Segments beginning with `Z`):

```yml
type_1:
  condition: $zsc322 EQUALS H1 || $zsc322 EQUALS H2
  generateList: false
  expressionType: HL7Spec
  valueOf: $code
  vars:
     zsc322: ZSC.3.2.2
  constants:
     code: allergy

type_2:
  condition: $zsc322 NOT_EQUALS H1 && $zsc322 NOT_EQUALS H2
  generateList: false
  expressionType: HL7Spec
  valueOf: $code
  vars:
     zsc322: ZSC.3.2.2
  constants:
     code: intolerance
```
### Referencing resources

Resources are referenced (linked) in one of two ways:

#### To reference an resource already created:
```yaml
subject:
   valueOf: datatype/Reference
   expressionType: resource
   specs: $Patient
```
Note the expressionType is `resource` of template `datatype/Reference`.  `specs` is the resource from the message yaml: `$Patient`.  Not the $ capital letter preceding the variable. The created `$Patient` is passed to the `Reference` template as content.

#### To create a new resource and reference a resource:
```yml
 performer:
   valueOf: resource/Practitioner
   expressionType: reference
   specs: OBX.16
```
Note the expression is a `reference`.  `Practitioner` is the template which will be used with the `specs` OBX.16 to create a Practitioner _and_ create a reference to it in this position.  

#### Passing references to children
Consider `PPR_PC1.yml`.  Note the resourceNames: `ServiceRequest` and `Encounter`.

In `DocumentReference.yml`, references to `ServiceRequest` and `Encounter` resources already created are passed as `serviceRequestRef` and `encounterRef`.
```yaml
context:
   valueOf: secondary/Context
   expressionType: resource
   vars:
      timestamp: TXA.4 | OBR.7
      providerCode: TXA.5
      serviceRequestRef: $ServiceRequest
      encounterRef: $Encounter
```

Then in `Context.yml`, the reference are used as `specs` passed into to `resource` expressions to `datatype/Reference`.  Thus the already created `ServiceRequest` and `Encounter` are used.

```yaml
related_2:
  valueOf: datatype/Reference
  expressionType: resource
  generateList: true
  specs: $serviceRequestRef

encounter:
  condition: $encounterRef NOT_NULL
  valueOf: datatype/Reference
  expressionType: resource
  specs: $encounterRef
```
#### Referencing resource values that are created from repeating segments
In cases where we need to match specific resources that are in a list from a `spec`, we can use a nested structure where the outer part identifies the element to match, and the inner part uses condition(s) to find the matching element.
A good example of this is Encounter.diagnosis in Encounter.yml.

- Outer Structure  
  - Nested expression type.
  - Cycles through each DG1 segment via `specs: DG1`.
  - Creates an identifier in `$refDG13` from DG1.3 for each time we cycle through the `specs: DG1`.
- Inner Structure (`expressionsMap`)
  - Cycles through each `specs: $Condition` resource created from each DG1 segment processed in the parent message `ADT_A03`.
  - Matches the `$refDG13` Identifier from the outer structure to the `$refconditionId` condition identifier from the inner structure.
  - `$refconditionId` is found in the `$Condition` by using `GeneralUtils.extractAttribute`, which uses a pattern matching utility to find the identifier.

```yaml
diagnosis:
   expressionType: nested
   evaluateLater: true
   generateList: true
   specs: DG1
   vars:
      refDG13: BUILD_IDENTIFIER_FROM_CWE, DG1.3
   expressionsMap:
     condition:
        valueOf: datatype/Reference
        expressionType: resource
        condition: $refconditionId EQUALS_STRING $refDG13 # Inner loop (refconditionId) matches outer loop (refDG13)
        specs: $Condition # Loops over the entire list of Condition resources
        vars:
          # refconditionId is calculated by pattern matching to find identifier that contains urn:id:extID as the system"
          refconditionId: $BASE_VALUE, GeneralUtils.extractAttribute(refconditionId,"$.identifier[?(@.system==\"urn:id:extID\")].value","String")
```

#### Referencing fields of repeating segments

As another example, in Immunization, each of the OBX segments needed processing of OBX.5 fields based on the value of OBX.3.  The following looks like it would work, but it doesn't. On the surface, it appears that `specs: OBX.5` will take each OBX record and process OBX5.  However this only works for the _first_ OBX.5, because `specs: OBX.5` is really specifying to repeat the sub-fields of OBX.5.  
```yml
# Wrong way to repeat for all OBX records
fundingSource:
  valueOf: datatype/CodeableConcept
  expressionType: resource
  condition: $obx3b EQUALS 30963-3
  specs: OBX.5
  vars:
    obx3b: String, OBX.3.1
```
The solution is to repeat on OBX using `spec: OBX`, and nest the OBX.5 processing within the repeat from `spec: OBX` 
```yml
# Right way to repeat for all OBX records
fundingSource:
   expressionType: nested
   condition: $obx3b EQUALS 30963-3
   specs: OBX
   vars:
      obx3b: String, OBX.3.1 
   expressions:
      - valueOf: datatype/CodeableConcept
        expressionType: resource
        specs: OBX.5
```

## Conditional Templates

Sometimes, when dealing with custom HL7 segments the correct FHIR resource for the segment differs
depending upon some value in the segment. For example, in our ZAL custom Alert segment field `ZAL.2.1` denotes the Alert Category,
and when the value is one of `A1`, `A3`, `H2` or `H4` then the correct FHIR resource is `AllergyIntolerance`;  for all other 
alert category values the correct FHIR resource is `Flag`

Two resources template entries with suitable `condition` expressions will direct each `ZAL` segment to its correct resource template.

```yml
resources:
    - resourceName: AllergyIntolerance
      segment: ZAL
      resourcePath: resource/AllergyIntoleranceZAL
      repeats: true
      condition: ZAL.2.1 IN [A1, A3, H2, H4]          ## Some of our custom ZAL segments are AllergyIntolerance
      additionalSegments:

    - resourceName: Flag
      segment: ZAL
      resourcePath: resource/FlagZAL
      repeats: true
      condition: ZAL.2.1 NOT_IN [A1, A3, H2, H4]      ## The rest of our custom ZAL segments are more general alert Flags
      additionalSegments:

```
The grammar for the condition field is as follows:

```
   <hl7spec>  EQUALS | NOT_EQUALS | IN | NOT_IN | NULL | NOT_NULL   <value> | [ ... ]
```
**Notes:**
  * hl7spec uses the same dot notation as expression syntax inside templates and can be of the folowing forms:
    - SEGMENT
    - SEGMENT.FIELD
    - SEGMENT.FIELD.COMPONENT
    - SEGMENT.FIELD.COMPONENT.SUBCOMPONENT
    - SEGMENT.FIELD(REPETITION)
    - SEGMENT.FIELD(REPETITION).COMPONENT
    - SEGMENT.FIELD(REPETITION).COMPONENT.SUBCOMPONENT
  
    `ZAL`, `ZAL.2`,  `ZAL.2.1`,  `ZAL.2.1.2`, `PID.14(1).2` & `PID.14(1).2.1` are all valid values for hl7spec.
  
  * The SEGMENT part of hl7spec **MUST** match the value of the `segment` field.

  * EQUALS and NOT_EQUALS expressions only accept a single value on the right-hand side of the expression.
  
  * IN and NOT_IN expressions only accept a list of values, delimited by comma, in square brackets on the right-hand side of the expression.
  
  * NULL and NOT_NULL do not accept any value.

  * The condition expression cannot be much more complex, as there are no context variables available at evaluation time.

**Examples**:
  *  `PID.5.1.2 EQUALS van`
  *  `ZAL.2.1.3 NOT_NULL`
  *  `ZAL.2(1).1 NULL`
  *  `ZAL.3.1 IN [A3, A4, H1, H3, FA, DA]`
  *  `ZAL.3.1 NOT_IN [A2, F3, DA]`
  *  `ZAL.2 EQUALS A4`
  *  `ZAL NOT_EQUALS H2`
