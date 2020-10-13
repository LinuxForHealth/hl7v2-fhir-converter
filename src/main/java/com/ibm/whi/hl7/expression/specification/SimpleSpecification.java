/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.specification;

import java.util.HashMap;
import java.util.Map;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputDataExtractor;
import com.ibm.whi.api.Specification;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.expression.EvaluationResultFactory;


/**
 * Represents HL7 data specification. It defines segment, field, component and subcomponent
 * names/identifiers that can be used for extracting data.
 * 
 *
 * @author pbhallam
 */

public class SimpleSpecification implements Specification {

  private String variableName;
  private boolean isExtractMultiple;
  private boolean useGroup;
  private InputDataExtractor primaryDataSource = new ContextMapData();
  
  public SimpleSpecification(String variableName,
      boolean isMultiple, boolean useGroup) {
    this.variableName = variableName;
    this.isExtractMultiple = isMultiple;
    this.useGroup = useGroup;

  }



  public boolean isExtractMultiple() {
    return isExtractMultiple;
  }



  public String getVariable() {
    return variableName;
  }



  public Class<? extends InputDataExtractor> getSourceInputDataClass() {
    return this.primaryDataSource.getClass();
  }

  @Override
  public EvaluationResult extractValueForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues) {
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    localContextValues.put(Constants.USE_GROUP,
        EvaluationResultFactory.getEvaluationResult(useGroup));
    return primaryDataSource.extractValueForSpec(this, localContextValues);
  }


  @Override
  public EvaluationResult extractMultipleValuesForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues) {
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    localContextValues.put(Constants.USE_GROUP,
        EvaluationResultFactory.getEvaluationResult(useGroup));
    return primaryDataSource.extractMultipleValuesForSpec(this, localContextValues);
  }



  public boolean isUseGroup() {
    return useGroup;
  }



  public void setUseGroup(boolean useGroup) {
    this.useGroup = useGroup;
  }


}
