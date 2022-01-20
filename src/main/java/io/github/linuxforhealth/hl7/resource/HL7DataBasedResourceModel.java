/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.exception.DataExtractionException;
import io.github.linuxforhealth.core.exception.RequiredConstraintFailureException;
import io.github.linuxforhealth.core.resource.ResourceResult;
import io.github.linuxforhealth.core.resource.SimpleResourceValue;
import io.github.linuxforhealth.hl7.resource.deserializer.HL7DataBasedResourceDeserializer;
import io.github.linuxforhealth.hl7.util.ExpressionUtility;

@JsonDeserialize(using = HL7DataBasedResourceDeserializer.class)
public class HL7DataBasedResourceModel implements ResourceModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceModel.class);

    private Map<String, Expression> expressions;
    private String spec;

    private String name;

    /**
     * 
     * @param name Name of the model
     * @param expressions Map of expressions to put in the model
     * @param hl7spec Which HL7 specification in use
     */

    public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions,
            String hl7spec) {
        this.expressions = new HashMap<>();
        this.expressions.putAll(expressions);
        this.spec = hl7spec;

        this.name = name;

    }

    public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions) {
        this(name, expressions, null);
    }

    @Override
    public Map<String, Expression> getExpressions() {
        return expressions;
    }

    @Override
    public ResourceResult evaluate(InputDataExtractor dataSource,
            Map<String, EvaluationResult> context, EvaluationResult baseValue) {
        ResourceResult resources = null;

        try {

            ResourceEvaluationResult result = ExpressionUtility.evaluate(dataSource, context, baseValue,
                    this.expressions);

            if (result != null && !result.getResolveValues().isEmpty()) {
                String groupId = getGroupId(context);
                resources = new ResourceResult(new SimpleResourceValue(result.getResolveValues(), this.name),
                        result.getAdditionalResolveValues(), groupId, result.getPendingExpressions());

            }

        } catch (RequiredConstraintFailureException e) {
            LOGGER.warn("Resource Constraint condition not satisfied for {}.", this.name);
            LOGGER.debug("Resource Constraint condition not satisfied for {}, exception {}", this.name, e.toString());
            return null;

        } catch (IllegalArgumentException | IllegalStateException | DataExtractionException e) {
            LOGGER.error("Exception during resource {} evaluation reason", this.name);
            LOGGER.debug("Exception during resource {} evaluation reason {}", this.name, e.toString());
            return null;

        }
        return resources;
    }

    private static String getGroupId(Map<String, EvaluationResult> localContext) {
        EvaluationResult result = localContext.get(Constants.GROUP_ID);
        if (result != null && result.getValue() instanceof String) {
            return result.getValue();
        }
        return null;
    }

    public String getSpec() {
        return spec;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
