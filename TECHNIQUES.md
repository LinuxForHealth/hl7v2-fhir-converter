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
```
java
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

```
java
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
## YAML Hints

Hints about the ways syntax and references work in the YAML files

### Condition test variables, not templates

Testing the segment fields directly in conditions doesn't work. Instead you must create a var for the template field and test the var.

Not this:
```yaml
telecom_1:
    condition: PID.14 NOT_NULL    
    valueOf: datatype/Telecom
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
    valueOf: datatype/Telecom
    generateList: true
    expressionType: resource
    specs: PID.14
    vars:
       pid14: PID.14
    constants: 
       use: "work"
```