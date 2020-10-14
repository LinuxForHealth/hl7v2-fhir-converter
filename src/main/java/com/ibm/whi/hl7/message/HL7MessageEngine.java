/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message;

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
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.FHIRResourceTemplate;
import com.ibm.whi.api.InputDataExtractor;
import com.ibm.whi.api.MessageEngine;
import com.ibm.whi.api.ResourceModel;
import com.ibm.whi.api.ResourceValue;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.ObjectMapperUtil;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.resource.ResourceResult;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.fhir.FHIRResourceMapper;
import com.ibm.whi.hl7.exception.RequiredConstraintFailureException;
import com.ibm.whi.hl7.message.util.SegmentExtractorUtil;
import com.ibm.whi.hl7.message.util.SegmentGroup;
import ca.uhn.hl7v2.model.Structure;

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
   * @see com.ibm.whi.api.MessageEngine#transform(com.ibm.whi.api.InputDataExtractor,
   *      java.lang.Iterable, java.util.Map)
   */
  @Override
  public Bundle transform(final InputDataExtractor dataInput, final Iterable<FHIRResourceTemplate> resources,
      final Map<String, EvaluationResult> contextValues) {
    Preconditions.checkArgument(dataInput != null, "dataInput cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Preconditions.checkArgument(resources != null, "resources cannot be null");

    HL7MessageData hl7DataInput = (HL7MessageData) dataInput;
    Bundle bundle = initBundle(dataInput);
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    for (FHIRResourceTemplate res : resources) {
      HL7FHIRResourceTemplate template = (HL7FHIRResourceTemplate) res;
      ResourceModel rs = res.getResource();
      List<ResourceResult> resourceResults = new ArrayList<>();
      try {
        MDC.put("Resource", rs.getName());

        if (res.isRepeats()) {
          List<ResourceResult> results =
              generateMultiple(hl7DataInput, template, localContextValues, bundle);
          resourceResults.addAll(results);


        } else {
          ResourceResult resourceValue =
              generateSingle(hl7DataInput, template, localContextValues, bundle);
          resourceResults.add(resourceValue);
        }

        resourceResults.removeIf(isEmpty());
        Map<String, EvaluationResult> newContextValues =
            getContextValuesFromResource(res, resourceResults);
        localContextValues.putAll(newContextValues);
      } catch (IllegalArgumentException | IllegalStateException e) {
        LOGGER.error("Exception during  resource {} generation", rs.getName(), e);

      } finally {
        MDC.remove("Resource");
      }



    }
    return bundle;
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
    if (!resTemplate.isRepeats()) {
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

  private Bundle initBundle(final InputDataExtractor dataInput) {
    Bundle bundle = new Bundle();
    bundle.setType(this.bundleType);
    bundle.setId(UUID.randomUUID().toString());
    Meta m = new Meta();
    m.setSource("Message: " + dataInput.getName() + ", Message Control Id: " + dataInput.getId());
    m.setLastUpdated(LocalDateTime.now().toDate());

    bundle.setMeta(m);
    return bundle;
  }



  private ResourceResult generateSingle(final HL7MessageData hl7DataInput,
      final HL7FHIRResourceTemplate template, final Map<String, EvaluationResult> contextValues,
      final Bundle bundle) {

    ResourceModel rs = template.getResource();
    List<String> groups = template.getSegment().getGroup();
    String segment = template.getSegment().getSegment();
    SegmentGroup segmentGroup;
    if (groups == null || groups.isEmpty()) {
      segmentGroup = SegmentExtractorUtil.extractSegmentGroup(segment,
          template.getAdditionalSegments(), hl7DataInput.getHL7DataParser());
    } else {
      segmentGroup = SegmentExtractorUtil.extractSegmentGroup(groups, segment,
          template.getAdditionalSegments(), hl7DataInput.getHL7DataParser(), template.getGroup());
    }

    if (segmentGroup != null) {
      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
      localContextValues.put(Constants.GROUP_ID,
          EvaluationResultFactory.getEvaluationResult(segmentGroup.getGroupId()));
      localContextValues.putAll(getContextMap(segmentGroup));

      ResourceResult evaluatedValue =
          rs.evaluate(hl7DataInput, ImmutableMap.copyOf(localContextValues),
              EvaluationResultFactory.getEvaluationResult(segmentGroup.getSegment()));

      if (evaluatedValue != null && !evaluatedValue.isEmpty()
          && evaluatedValue.getValue().getResource() != null
          && !evaluatedValue.getValue().getResource().isEmpty()) {

        addEntry(template.getResourceName(), evaluatedValue.getValue(), bundle);

        List<ResourceValue> additionalResourceObjects = evaluatedValue.getAdditionalResources();
        addToBundle(bundle, additionalResourceObjects);
        return evaluatedValue;
      }
    }
    return null;
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



  private List<ResourceResult> generateMultiple(final HL7MessageData hl7DataInput,
      final HL7FHIRResourceTemplate template, final Map<String, EvaluationResult> contextValues,
      final Bundle bundle) {


    ResourceModel rs = template.getResource();
    List<String> groups = template.getSegment().getGroup();
    String segment = template.getSegment().getSegment();
    List<ResourceResult> resourceResults = null;
    List<SegmentGroup> multipleSegments =
        getMultipleSegments(hl7DataInput, template, groups, segment);
    if (!multipleSegments.isEmpty()) {
      resourceResults =
          generateMultipleResources(hl7DataInput, rs, contextValues, multipleSegments);

    }

    if (resourceResults != null && !resourceResults.isEmpty()) {
      for (ResourceResult resReult : resourceResults) {
        addToBundle(bundle, Lists.newArrayList(resReult.getValue()));
        addToBundle(bundle, resReult.getAdditionalResources());
      }
    }

    return resourceResults;
  }


  private static List<SegmentGroup> getMultipleSegments(final HL7MessageData hl7DataInput,
      final HL7FHIRResourceTemplate template, List<String> groups, String segment) {
    List<SegmentGroup> multipleSegments;
    if (groups != null && !groups.isEmpty()) {
      multipleSegments = SegmentExtractorUtil.extractSegmentGroups(groups, segment,
          template.getAdditionalSegments(), hl7DataInput.getHL7DataParser(), template.getGroup());


    } else {
      multipleSegments = SegmentExtractorUtil.extractSegmentGroups(segment,
          template.getAdditionalSegments(), hl7DataInput.getHL7DataParser());

    }
    return multipleSegments;
  }

  private static List<ResourceResult> generateMultipleResources(final HL7MessageData hl7DataInput,
      final ResourceModel rs, final Map<String, EvaluationResult> contextValues,
      final List<SegmentGroup> multipleSegments) {
    List<ResourceResult> resourceResults = new ArrayList<>();
    for (SegmentGroup segGroup : multipleSegments) {

      List<EvaluationResult> baseValues = new ArrayList<>();
      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
      localContextValues.put(Constants.GROUP_ID,
          EvaluationResultFactory.getEvaluationResult(segGroup.getGroupId()));
      segGroup.getSegments()
          .forEach(struct -> baseValues.add(EvaluationResultFactory.getEvaluationResult(struct)));

      localContextValues.putAll(getContextMap(segGroup));

      for (EvaluationResult baseValue : baseValues) {
        try {
          ResourceResult result =
              rs.evaluate(hl7DataInput, ImmutableMap.copyOf(localContextValues), baseValue);
          if (result != null && result.getValue() != null) {
            resourceResults.add(result);

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
