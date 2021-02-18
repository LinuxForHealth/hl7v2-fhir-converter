/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import org.hl7.fhir.r4.model.Bundle.BundleType;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.core.Constants;

/**
 * Converts HL7 message to FHIR bundle resource based on the customizable templates.
 * 
 *
 * @author pbhallam
 */
public class ConverterOptions {
  public static final ConverterOptions SIMPLE_OPTIONS = new Builder().build();


  private BundleType bundleType;
  private boolean prettyPrint;
  private boolean validateResource;


  private ConverterOptions(Builder builder) {
    if (builder.bundleType != null) {
      this.bundleType = builder.bundleType;
    } else {
      this.bundleType = Constants.DEFAULT_BUNDLE_TYPE;
    }
    this.prettyPrint = builder.prettyPrint;
    this.validateResource = builder.validateResource;

  }

  public static class Builder {
    private BundleType bundleType;
    private boolean prettyPrint;
    private boolean validateResource;


    public Builder withBundleType(BundleType bundleType) {
      Preconditions.checkArgument(bundleType != null, "Bundle type cannot be null");
      this.bundleType = bundleType;
      return this;
    }

    public Builder withPrettyPrint() {
      this.prettyPrint = true;
      return this;
    }

    public Builder withValidateResource() {
      this.validateResource = true;
      return this;
    }





    public ConverterOptions build() {
      return new ConverterOptions(this);
    }


  }

  public BundleType getBundleType() {
    return bundleType;
  }

  public boolean isPrettyPrint() {
    return prettyPrint;
  }

  public boolean isValidateResource() {
    return validateResource;
  }



}