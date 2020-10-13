/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.Map;
import com.ibm.whi.core.resource.ResourceResult;

/**
 * Represents FHIR resource that needs to be generated based on the provided expressions
 * 
 *
 * @author pbhallam
 */
public interface ResourceModel {

  /**
   * Evaluates the resource to generate the FHIR resource equivalent represented by the resource
   * template.
   * 
   * @param dataExtractor - {@link InputDataExtractor}
   * @param contextValues - Map of key:String, value: {@link EvaluationResult} which can be during
   *        expression evaluation.
   * @param baseValue - {@link EvaluationResult} Base value is the values that this resource is
   *        Primarily based on. - Example for a Patient Resource , the base value could be the PID
   *        segment.
   * @return result {@link ResourceResult }
   */
  ResourceResult evaluate(InputDataExtractor dataExtractor,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue);

  /**
   * Returns the list of Expression that needs to be evaluated to generate the resource
   * 
   * @return Map of key:String, value: {@link Expression}
   */
  Map<String, Expression> getExpressions();



  /**
   * Name of the resource
   * 
   * @return String - Name of the resource
   */
  String getName();


}
