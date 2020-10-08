/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Meta;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.api.MessageEngine;
import com.ibm.whi.api.ResourceModel;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.ObjectMapperUtil;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.message.AbstractFHIRResource;
import com.ibm.whi.core.resource.ResourceResult;
import com.ibm.whi.core.resource.ResourceValue;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.fhir.FHIRResourceMapper;
import com.ibm.whi.hl7.message.util.SegmentExtractorUtil;
import com.ibm.whi.hl7.message.util.SegmentGroup;
import ca.uhn.hl7v2.model.Structure;

public class HL7MessageEngine implements MessageEngine {



  private static final Logger LOGGER = LoggerFactory.getLogger(HL7MessageEngine.class);
  private static final ObjectMapper OBJ_MAPPER = ObjectMapperUtil.getJSONInstance();
  private FHIRContext context;
  private BundleType bundleType;

  public HL7MessageEngine(FHIRContext context) {
    this(context, Constants.DEFAULT_BUNDLE_TYPE);
  }

  public HL7MessageEngine(FHIRContext context, BundleType bundleType) {
    this.context = context;
    this.bundleType = bundleType;
  }

  /**
   * Converts a HL7 message to a FHIR bundle with the list of resources specified
   * 
   * @param dataExtractor
   * @param resources
   * @param context
   * @return
   * @throws IOException
   */
  @Override
  public Bundle transform(InputData dataInput, Iterable<? extends AbstractFHIRResource> resources,
      Map<String, EvaluationResult> contextValues) {
    Preconditions.checkArgument(dataInput != null, "dataInput cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Preconditions.checkArgument(resources != null, "resources cannot be null");

    HL7MessageData hl7DataInput = (HL7MessageData) dataInput;
    Bundle bundle = initBundle(dataInput);
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    for (AbstractFHIRResource res : resources) {
      HL7FHIRResource hres = (HL7FHIRResource) res;
      ResourceModel rs = res.getResource();

      try {

        MDC.put("Resource", rs.getName());

        if (res.isRepeats()) {

          generateMultiple(hl7DataInput, hres, localContextValues, bundle);

        } else {
          ResourceValue resourceValue =
              generateSingle(hl7DataInput, hres, localContextValues, bundle);
          // Add resource generated to context map so other resources can reference this resource
          if (resourceValue != null) {
            localContextValues.put(res.getResourceName(),
                EvaluationResultFactory.getEvaluationResult(resourceValue.getResource()));
          }
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        LOGGER.error("Exception during  resource {} generation", rs.getName(), e);

      } finally {
        MDC.remove("Resource");
      }



    }
    return bundle;
  }

  private Bundle initBundle(InputData dataInput) {
    Bundle bundle = new Bundle();
    bundle.setType(this.bundleType);
    bundle.setId(UUID.randomUUID().toString());
    Meta m = new Meta();
    m.setSource("Message: " + dataInput.getName() + ", Message Control Id: " + dataInput.getId());
    m.setLastUpdated(LocalDateTime.now().toDate());

    bundle.setMeta(m);
    return bundle;
  }



  private ResourceValue generateSingle(HL7MessageData hl7DataInput, HL7FHIRResource hres,
      Map<String, EvaluationResult> contextValues, Bundle bundle) {

    ResourceModel rs = hres.getResource();
    List<String> groups = hres.getSegment().getGroup();
    String segment = hres.getSegment().getSegment();
    SegmentGroup segmentGroup;
    if (groups == null || groups.isEmpty()) {
      segmentGroup = SegmentExtractorUtil.extractSegmentGroup(segment, hres.getAdditionalSegments(),
          hl7DataInput.getHL7DataParser());
    } else {
      segmentGroup = SegmentExtractorUtil.extractSegmentGroup(groups, segment,
          hres.getAdditionalSegments(), hl7DataInput.getHL7DataParser());
    }

    if (segmentGroup != null) {
      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
      // populate local context with segment and additional segment details
      localContextValues.put(segment,
          EvaluationResultFactory.getEvaluationResult(segmentGroup.getSegment()));
      if (!segmentGroup.getAdditionalSegments().isEmpty()) {

        for (Entry<String, List<Structure>> e : segmentGroup.getAdditionalSegments().entrySet()) {
          localContextValues.put(e.getKey(),
              EvaluationResultFactory.getEvaluationResult(e.getValue()));
        }
      }

      ResourceResult evaluatedValue =
          rs.evaluate(hl7DataInput, ImmutableMap.copyOf(localContextValues),
              EvaluationResultFactory.getEvaluationResult(segmentGroup.getSegment()));

      if (evaluatedValue != null && evaluatedValue.getResource() != null
          && evaluatedValue.getResource().getResource() != null
          && !evaluatedValue.getResource().getResource().isEmpty()) {

        addEntry(hres.getResourceName(), evaluatedValue.getResource(), bundle);

        List<ResourceValue> additionalResourceObjects = evaluatedValue.getAdditionalResources();
        addValues(bundle, additionalResourceObjects);
        return evaluatedValue.getResource();
      }
    }
    return null;
  }



  private void generateMultiple(HL7MessageData hl7DataInput, HL7FHIRResource hl7res,
      Map<String, EvaluationResult> variables, Bundle bundle) {
    Map<String, EvaluationResult> localVariables = new HashMap<>(variables);
    List<ResourceValue> resourcevalues = new ArrayList<>();
    List<ResourceValue> additionalResources = new ArrayList<>();
    ResourceModel rs = hl7res.getResource();
    List<String> groups = hl7res.getSegment().getGroup();
    String segment = hl7res.getSegment().getSegment();
    if (groups != null && !groups.isEmpty()) {
      List<SegmentGroup> multipleSegments = SegmentExtractorUtil.extractSegmentGroups(groups,
          segment, hl7res.getAdditionalSegments(), hl7DataInput.getHL7DataParser());
      if (!multipleSegments.isEmpty()) {
          generateResources(hl7DataInput, rs, localVariables, resourcevalues, additionalResources,
              multipleSegments);
      }
      
    } else {
      List<SegmentGroup> multipleSegments = SegmentExtractorUtil.extractSegmentGroups(segment,
          hl7res.getAdditionalSegments(), hl7DataInput.getHL7DataParser());
      if (!multipleSegments.isEmpty()) {
        generateResources(hl7DataInput, rs, localVariables, resourcevalues, additionalResources,
            multipleSegments);
      }
    }



    if (!resourcevalues.isEmpty()) {
      addValues(bundle, resourcevalues);
      addValues(bundle, additionalResources);
    }
  }

  private static void generateResources(HL7MessageData hl7DataInput, ResourceModel rs,
      Map<String, EvaluationResult> localContextValues, List<ResourceValue> resourcevalues,
      List<ResourceValue> additionalResources, List<SegmentGroup> multipleSegments) {

    for (SegmentGroup segGroup : multipleSegments) {
      List<EvaluationResult> baseValues = new ArrayList<>();

      segGroup.getSegments()
          .forEach(d -> baseValues.add(EvaluationResultFactory.getEvaluationResult(d)));
      if (!segGroup.getAdditionalSegments().isEmpty()) {

        for (Entry<String, List<Structure>> e : segGroup.getAdditionalSegments().entrySet()) {
          localContextValues.put(e.getKey(),
              EvaluationResultFactory.getEvaluationResult(e.getValue()));
        }
      }

      for (EvaluationResult baseValue : baseValues) {
        ResourceResult result =
            rs.evaluate(hl7DataInput, ImmutableMap.copyOf(localContextValues), baseValue);
        if (result != null && result.getResource() != null) {
          resourcevalues.add(result.getResource());
          additionalResources.addAll(result.getAdditionalResources());

        }
      }
    }
  }


  private void addValues(Bundle bundle, List<ResourceValue> objects) {
    if (objects != null && !objects.isEmpty()) {
      objects.forEach(obj -> {
        addEntry(obj.getResourceClass(), obj, bundle);
      });

    }
  }



  private void addEntry(String resourceClass, ResourceValue obj, Bundle bundle) {

    try {
      if (obj != null) {
        LOGGER.info("Converting resourceName {} to FHIR {}", resourceClass, obj.getResource());
        String json = OBJ_MAPPER.writeValueAsString(obj.getResource());
        if (json != null) {
          org.hl7.fhir.r4.model.Resource parsed = context.getParser()
              .parseResource(FHIRResourceMapper.getResourceClass(resourceClass), json);

          bundle.addEntry().setResource(parsed);
        }
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Processing exception when Serialization", e);
    }


  }

  @Override
  public FHIRContext getFHIRContext() {
    return context;
  }


}
