/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import com.google.common.collect.Lists;
import ca.uhn.hl7v2.model.Structure;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.FHIRResourceTemplate;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.MessageEngine;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.exception.RequiredConstraintFailureException;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.resource.ResourceResult;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.fhir.FHIRResourceMapper;
import io.github.linuxforhealth.hl7.message.util.SegmentExtractorUtil;
import io.github.linuxforhealth.hl7.message.util.SegmentGroup;

/**
 * Implements Message engine for HL7 message data
 * 
 *
 * @author pbhallam
 */
public class HL7MessageEngine implements MessageEngine {



  private static final Logger LOGGER = LoggerFactory.getLogger(HL7MessageEngine.class);
  private static final ObjectMapper OBJ_MAPPER = ObjectMapperUtil.getJSONInstance();
  private FHIRContext context;
  private BundleType bundleType;

  /**
   * 
   * @param context
   */
  public HL7MessageEngine(FHIRContext context) {
    this(context, Constants.DEFAULT_BUNDLE_TYPE);
  }

  /**
   * 
   * @param context
   * @param bundleType
   */
  public HL7MessageEngine(FHIRContext context, BundleType bundleType) {
    this.context = context;
    this.bundleType = bundleType;
  }


  /**
   * Converts a HL7 message to a FHIR bundle with the list of resources specified
   * 
   * @see io.github.linuxforhealth.api.MessageEngine#transform(io.github.linuxforhealth.api.InputDataExtractor,
   *      java.lang.Iterable, java.util.Map)
   */
  @Override
  public Bundle transform(final InputDataExtractor dataInput,
      final Iterable<FHIRResourceTemplate> resources,
      final Map<String, EvaluationResult> contextValues) {
    Preconditions.checkArgument(dataInput != null, "dataInput cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Preconditions.checkArgument(resources != null, "resources cannot be null");

    HL7MessageData hl7DataInput = (HL7MessageData) dataInput;
    Bundle bundle = initBundle();
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    for (FHIRResourceTemplate genericTemplate : resources) {
      HL7FHIRResourceTemplate hl7ResourceTemplate = (HL7FHIRResourceTemplate) genericTemplate;
      ResourceModel rs = genericTemplate.getResource();
      List<ResourceResult> resourceResults = new ArrayList<>();
      try {
        MDC.put("Resource", rs.getName());
        List<ResourceResult> results =
            generateResources(hl7DataInput, hl7ResourceTemplate, localContextValues, bundle);
        if (results != null) {
          resourceResults.addAll(results);
        }

        resourceResults.removeIf(isEmpty());
        Map<String, EvaluationResult> newContextValues =
            getContextValuesFromResource(hl7ResourceTemplate, resourceResults);
        localContextValues.putAll(newContextValues);
      } catch (IllegalArgumentException | IllegalStateException e) {
        LOGGER.error("Exception during  resource {} generation", rs.getName(), e);

      } finally {
        MDC.remove("Resource");
      }



    }
    LOGGER.info(
        "Successfully converted Message: {} , Message Control Id: {} to FHIR bundle resource with id {}",
        dataInput.getName(), dataInput.getId(), bundle.getId());
    return bundle;
  }

  private List<ResourceResult> generateResources(HL7MessageData hl7DataInput,
      HL7FHIRResourceTemplate template, Map<String, EvaluationResult> contextValues,
      Bundle bundle) {

    ResourceModel resourceModel = template.getResource();
    List<String> segmentGroup = template.getAttributes().getSegment().getGroup();
    String segment = template.getAttributes().getSegment().getSegment();
    List<ResourceResult> resourceResults = null;
    List<SegmentGroup> multipleSegments =
        getMultipleSegments(hl7DataInput, template, segmentGroup, segment);
    if (!multipleSegments.isEmpty()) {

      resourceResults = generateMultipleResources(hl7DataInput, resourceModel, contextValues,
          multipleSegments, template.isGenerateMultiple());

    }

    if (resourceResults != null && !resourceResults.isEmpty()) {
      for (ResourceResult resReult : resourceResults) {
        addToBundle(bundle, Lists.newArrayList(resReult.getValue()));
        addToBundle(bundle, resReult.getAdditionalResources());
      }
    }

    return resourceResults;
  }

  private static Predicate<ResourceResult> isEmpty() {
    return (ResourceResult p) -> {
      return p == null || p.isEmpty() || p.getValue().isEmpty();

    };
  }

  private static Map<String, EvaluationResult> getContextValuesFromResource(
      FHIRResourceTemplate resTemplate, List<ResourceResult> resourceResults) {
    Map<String, EvaluationResult> localContextValues = new HashMap<>();
    // Add resource generated to context map so other resources can reference this resource
    if (resourceResults.isEmpty() || !resTemplate.isReferenced()) {
      return localContextValues;
    }
    if (!resTemplate.isGenerateMultiple()) {
      localContextValues.put(resTemplate.getResourceName(), EvaluationResultFactory
          .getEvaluationResult(resourceResults.get(0).getValue().getResource()));

    } else {
      Map<String, List<ResourceResult>> resourcesByGroup = resourceResults.stream()
          .collect(Collectors.groupingBy(r -> getResultIdentifier(resTemplate, r)));
      for (Entry<String, List<ResourceResult>> e : resourcesByGroup.entrySet()) {
        List<ResourceValue> evl = new ArrayList<>();
        e.getValue().forEach(res -> evl.add(res.getValue()));
        localContextValues.put(e.getKey(), EvaluationResultFactory.getEvaluationResult(evl));
      }
    }

    return localContextValues;
  }

  private static String getResultIdentifier(FHIRResourceTemplate resTemplate,
      ResourceResult result) {
    if (result != null && result.getGroupId() != null) {
      return resTemplate.getResourceName() + "_" + result.getGroupId();
    } else {
      return resTemplate.getResourceName();
    }
  }

  private Bundle initBundle() {
    Bundle bundle = new Bundle();
    bundle.setType(this.bundleType);
    bundle.setId(UUID.randomUUID().toString());
    Meta m = new Meta();
    m.setLastUpdated(LocalDateTime.now().toDate());
    bundle.setMeta(m);
    return bundle;
  }



  private static Map<String, EvaluationResult> getContextMap(SegmentGroup segmentGroup) {
    // populate local context with additional segment details
    Map<String, EvaluationResult> localContextValues = new HashMap<>();
    if (!segmentGroup.getAdditionalSegments().isEmpty()) {
      for (Entry<String, List<Structure>> e : segmentGroup.getAdditionalSegments().entrySet()) {
        localContextValues.put(e.getKey(),
            EvaluationResultFactory.getEvaluationResult(e.getValue()));
      }
    }
    return localContextValues;
  }



  private static List<SegmentGroup> getMultipleSegments(final HL7MessageData hl7DataInput,
      final HL7FHIRResourceTemplate template, List<String> segmentGroup, String segment) {
    List<SegmentGroup> multipleSegments;
    if (segmentGroup != null && !segmentGroup.isEmpty()) {
      multipleSegments = SegmentExtractorUtil.extractSegmentGroups(segmentGroup, segment,
          template.getAttributes().getAdditionalSegments(), hl7DataInput.getHL7DataParser(),
          template.getAttributes().getGroup());


    } else {
      multipleSegments = SegmentExtractorUtil.extractSegmentNonGroups(segment,
          template.getAttributes().getAdditionalSegments(), hl7DataInput.getHL7DataParser());

    }
    return multipleSegments;
  }

  private static List<ResourceResult> generateMultipleResources(final HL7MessageData hl7DataInput,
      final ResourceModel rs, final Map<String, EvaluationResult> contextValues,
      final List<SegmentGroup> multipleSegments, boolean generateMultiple) {
    List<ResourceResult> resourceResults = new ArrayList<>();
    for (SegmentGroup currentGroup : multipleSegments) {


      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
      localContextValues.put(Constants.GROUP_ID,
          EvaluationResultFactory.getEvaluationResult(currentGroup.getGroupId()));
      // Resource needs to be generated for each base value in the group
      List<EvaluationResult> baseValues = new ArrayList<>();
      currentGroup.getSegments()
          .forEach(struct -> baseValues.add(EvaluationResultFactory.getEvaluationResult(struct)));

      localContextValues.putAll(getContextMap(currentGroup));

      for (EvaluationResult baseValue : baseValues) {
        try {
          ResourceResult result =
              rs.evaluate(hl7DataInput, ImmutableMap.copyOf(localContextValues), baseValue);
          if (result != null && result.getValue() != null) {
            resourceResults.add(result);
            if (!generateMultiple) {
              // If only single resource needs to be generated then return.
              return resourceResults;
            }

          }


        } catch (RequiredConstraintFailureException | IllegalArgumentException
            | IllegalStateException e) {
          LOGGER.warn("Exception encountered", e);
        }
      }

    }
    return resourceResults;
  }


  private void addToBundle(Bundle bundle, List<ResourceValue> objects) {
    if (objects != null && !objects.isEmpty()) {
      objects.forEach(obj -> {
        addEntry(obj.getFHIRResourceType(), obj, bundle);
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

          bundle.addEntry().setResource(parsed).setFullUrl("urn:uuid:" + parsed.getId());
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
