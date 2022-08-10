# Coding and Testing Best Practices
## General
In addition to general Java best practices, these are additional requirements and recommendations 
for keeping the increasingly large converter code maintable.  

1. Format code before checkin. This includes Java and YML files.
1. Remove all println's.
1. NO Logging of PHI.  Anything that could potentially contain PHI must be put into a debug statement.
1. Use a linter.  The linter is your friend, and will help you avoid _code smell_.  We recommend `SonarLint` on VS Code or IntelliJ, a SonarQube based linter.
1. Solve all linter warnings. Watch for:
   1. Remove unused variable / imports (the formatter in eclipse will handle this automatically)
   1. Remove `Public` on test methods.  
   1. Ensure assertThat calls are complete, eg `assertThat(x).isTrue()` not `assertThat(x.isTrue())`
1. Use linter overrides sparingly. You can tell your linter to ignore transgressions with an override. Please approve new overrides with team leads.  
7. Use exact imports (not wild cards)
1. Clean up unused imports
1. Fix spelling errors
1. Be generous with comments.  Your code reviewers, the next developer, and your future self will thank you.
1. Answer all code review comments.  It is OK to simply say "done" or "fixed".  Set all items addressed to `Resolved`.  For deferred items, it is often,
though not always, appropriate to add a TODO comment in the code.

## YAML Templates
### Namespace and scope
When a message is processed, the segments are passed in as `additionalSegments`.  These segments are _in scope_ for all templates that are processed in the recursive parsing of HL7 and construction of the FHIR resourses. As each resource template is opened the `segments` and `vars` which are passed in remain in scope for all children templates opened to complete full message parsing. 
1. Utilize scope when possible. Avoid creating unnecessary vars when segments and fields are already available. If it is not obvious, add comments on dependencies and assumptions.  Example from `Coverage.yml`:
```yaml
# If the subscriber is not SEL (self), then create the related person
subscriber_1:
   condition: $relatedRelationshipStr NOT_NULL && $relatedRelationshipStr NOT_EQUALS SEL
   valueOf: resource/RelatedPerson
   expressionType: reference
   vars: 
      relatedRelationshipStr: String, IN1.17
      # Related person gets many values from scope, so they do not need to be passed in
      #  IN1 and sub-fields
      #  $Patient  
```
2. When vars are created, use names that are sufficiently unique to avoid any potential name collisions with parent and child templates.  
### Formatting
An improperly formatted yml template may cause deserialization failures.  Usually caused by unmatched indentations, or missing end of line indicator at end of file.   Use of a formatter on the yml templates should solve this problem.

## HL7-FHIR Converter testcases

1.  Choose a meaningful test name; use `camelCase`.
1.  Above the test, describe the test(s).  This makes it easier to understand how the various tests differ.

### Minimize sample data
Improve readability and reduce confusion, reduce unnecessary computation!
1.  Create HL7 Messages having the minimal segments needed for the test
1.  Create HL7 Segments with the minimal fields needed for the test

Example: HL7ConditionFHIRConversionTest.validateProblemWithInvalidClinicalStatusAndResolutionDate

### Add thorough segment / field specific documentation to improve understanding 
1.  Near the Segments, document which fields must be populated or left empty for the test.  This ensures no one changes critical pieces of the message. This is especially important when testing precendence, as we rely on certain fields being specified to properly verify they are not being used.  See example, below.
1.  Break up long messages and add field documentation. See example below. Detailed example [Hl7FinancialInsuranceTest. testBasicInsuranceCoverageFields](/src/test/java/io/github/linuxforhealth/hl7/segments/Hl7FinancialInsuranceTest.java): 
```java
                // IN1 Segment is split and concatenated for easier understanding. (| precedes numbered field.)
                // IN1.2.1, IN1.2.3 to Identifier 1
                // IN1.2.4, IN1.2.6 to Identifier 2
                + "IN1|1|Value1^^System3^Value4^^System6"
                // IN1.3 to Organization Identifier 
                //    IN1.3.1 to Organization Identifier.value
                //    IN1.3.4 to Organization Identifier.system
                //    IN1.3.5 to Organization Identifier.type.code
                //    IN1.3.7 to Organization Identifier.period.start
                //    IN1.3.8 to Organization Identifier.period.end
                + "|IdValue1^^^IdSystem4^IdType5^^20201231145045^20211231145045"
                // IN1.4 to Organization Name
                + "|Large Blue Organization"
                // IN1.5 to Organization Address (All XAD standard fields)
                + "|456 Ultramarine Lane^^Faketown^CA^ZIP5"
...
                + "|UA34567|Blue|||20201231145045|20211231145045||||||||||||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.46 to Identifier 3
                // IN1.47 to IN1.53 NOT REFERENCED
                + "|MEMBER36||||||||||Value46|||||||\n";
```
1.  On/near the line of the associated assert, comment which Field(s) are being tested.
```java
        assertThat(coverage.getIdentifier()).hasSize(4);
        assertThat(coverage.getIdentifier().get(0).getValue()).isEqualTo("Value1"); // IN1.2.1
        assertThat(coverage.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:System3"); // IN1.2.3
```


### Check multiple data fields in the same message parsing.
During test, the parsing of the HL7 is the most time consuming step.  When possible and reasonable, and not where it might cause confusion, test multiple field from the same message parsing.  Provide clear documentation.  Use SonarQube override `squid:S5961` if needed.

### Be thorough
1. When checking number of resources, check there are no extras by testing an expected total.
1. Check what shouldn't be there, especially at low level functions
1. If another test is doing the thorough test, note it in documentation.

### Use parameterized tests 
This is especially useful as we want to check the _same segment_ under _different messages._
#### Example of single value parameter passing
From [HL7ConditionFHIRConversionTest. validateProblemWithOnsetDateTimeWithNoOnsetString](src/test/java/io/github/linuxforhealth/hl7/segments/HL7ConditionFHIRConversionTest.java)
```java
    @ParameterizedTest
    @ValueSource(strings = {
            // PRB.1 to PRB.4 required
            // PRB.16 to onset date
            // PRB.17 purposely empty 
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||||20180310074000||\r",
            // PRB.1 to PRB.4 required
            // PRB.16 to onset date
            // PRB.17 present
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||||20180310074000|textual representation of the time when the problem began|\r" })
    void validateProblemWithOnsetDateTimeWithNoOnsetString(String segmentPRB) {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + segmentPRB;
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Condition condition = (Condition) conditionResource.get(0);

        // Verify onset is set correctly to PRB.16
        assertThat(condition.getOnset().toString()).containsPattern("2018-03-10T07:40:00"); // PRB.16

    }
```
#### Example of double value parameter passing
From [ConditionUtilTest. testValueComparisionValueSimpleConditions](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/src/test/java/io/github/linuxforhealth/core/expression/condition/ConditionUtilTest.java)
```java
    private static Stream<Arguments> parmsTestValueComparisionValueSimpleConditions() {
        return Stream.of(
                //  Arguments are: (condition, val1, val2, operator)

                // simple_equals_string_condition
                Arguments.of("$var1 EQUALS abc", "$var1", "abc", "EQUALS"),

                // simple_greaterthan_condition
                Arguments.of("$var1 GREATER_THAN 4", "$var1", "4", "GREATER_THAN"),

                // simpleQuotedCharactersParsing 
                Arguments.of("$var1 EQUALS ':'", "$var1", ":", "EQUALS"),

                // another simpleQuotedCharactersParsing
                Arguments.of("$var1 EQUALS '/'", "$var1", "/", "EQUALS"));
    }

    @ParameterizedTest
    @MethodSource("parmsTestValueComparisionValueSimpleConditions")
    void testValueComparisionValueSimpleConditions(String condition, String val1, String val2, String operator) {
        SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getVar1()).isEqualTo(val1);
        assertThat(simplecondition.getVar2()).isEqualTo(val2);
        assertThat(simplecondition.getConditionOperator()).isEqualTo(operator);
    }
```

### Use common utilities
Use common methods in DatatypeUtils and ResourceUtils to reduce code duplication and clutter:
1. *ResourceUtils. createFHIRBundleFromHL7MessageReturnEntryList* Given a message, return the entries from a bundle
`List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);`
1. *ResourceUtils. getResourceList* get a list of this kind of resource by providing the structure and the type of resource.
`List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);`
1. *DatatypeUtils* has methods to check `CodeableConcepts` and `codings`



