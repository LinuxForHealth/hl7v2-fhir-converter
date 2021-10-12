/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.fhir;


import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import io.github.linuxforhealth.core.Constants;

public class FHIRContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(FHIRContext.class);

  private static final FhirContext CTX = FhirContext.forR4();
  private IParser parser;
  private static FhirValidator validator;
  private boolean validateResource;

  /**
   * Constructor for FHIRContext
   */
  public FHIRContext(boolean isPrettyPrint, boolean validateResource) {
    parser = CTX.newJsonParser();
    parser.setPrettyPrint(isPrettyPrint);
    this.validateResource = validateResource;

  }



  public FHIRContext() {
    this(Constants.DEFAULT_PRETTY_PRINT, false);
  }

  public IParser getParser() {
    return parser;
  }

  public FhirContext getCtx() {
    return CTX;
  }



  public static FhirValidator getValidator() {
    initValidator();
    return validator;
  }



  public String encodeResourceToString(Bundle bundle) {
    if (validateResource) {
    ValidationResult result = getValidator().validateWithResult(bundle);
    // The result object now contains the validation results
      List<String> validationIssues = new ArrayList<>();
    for (SingleValidationMessage next : result.getMessages()) {
        if (ResultSeverityEnum.FATAL == next.getSeverity()
            || ResultSeverityEnum.ERROR == next.getSeverity()) {
          validationIssues
              .add(next.getLocationString() + " " + next.getMessage() + " " + next.getSeverity());
          LOGGER.debug("Validation issues: {} {}", next.getLocationString(),
              next.getSeverity());
          LOGGER.error("Validation issues: {}", next.getSeverity());
        } else {
          LOGGER
              .warn("Validation issues: {}", next.getSeverity());
        }
        if (!validationIssues.isEmpty()) {
          throw new IllegalArgumentException(
              "Validation issues encountered. " + StringUtils.join(validationIssues, "|"));
        }

      }
    }
    return this.parser.encodeResourceToString(bundle);
  }

  private static void initValidator() {
    if (validator == null) {
      validator = CTX.newValidator();
      // Create a validation module and register it
      IValidatorModule module = new FhirInstanceValidator(CTX);
      validator.registerValidatorModule(module);
    }


  }

}
