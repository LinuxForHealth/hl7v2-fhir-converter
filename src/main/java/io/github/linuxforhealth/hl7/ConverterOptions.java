/*
 * (C) Copyright IBM Corp. 2020, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import org.hl7.fhir.r4.model.Bundle.BundleType;

import java.util.HashMap;
import java.util.Map;

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
    private String zoneIdText;
    private HashMap<String, String> properties;

    private ConverterOptions(Builder builder) {
        if (builder.bundleType != null) {
            this.bundleType = builder.bundleType;
        } else {
            this.bundleType = Constants.DEFAULT_BUNDLE_TYPE;
        }
        this.zoneIdText = builder.zoneIdText;
        this.properties = builder.properties;
        this.prettyPrint = builder.prettyPrint;
        this.validateResource = builder.validateResource;
    }

    public static class Builder {
        private BundleType bundleType;
        private boolean prettyPrint;
        private boolean validateResource;
        private String zoneIdText;
        private HashMap<String, String> properties = new HashMap<>();

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

        public Builder withZoneIdText(String zoneIdText) {
            Preconditions.checkArgument(zoneIdText != null, "zoneIdText cannot be null");
            this.zoneIdText = zoneIdText;
            return this;
        }

        public Builder withProperty(String key, String value) {
            Preconditions.checkArgument(key != null, "Property key cannot be null");
            Preconditions.checkArgument(value != null, "Property value cannot be null");
            this.properties.put(key, value);
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

    public String getZoneIdText() {
        return zoneIdText;
    }

    public String getProperty(String key) {
        Preconditions.checkArgument(key != null, "Property key cannot be null");
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}