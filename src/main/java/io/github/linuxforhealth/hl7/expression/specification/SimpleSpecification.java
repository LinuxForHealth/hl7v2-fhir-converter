/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.specification;

import java.util.HashMap;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.VariableUtils;


/**
 * Represents simple specification, where value has to be extracted using the variable name from the
 * context map
 * 
 *
 * @author pbhallam
 */

public class SimpleSpecification implements Specification {

  private String variableName;
  private boolean isExtractMultiple;
  private boolean useGroup;
  private boolean isFuzzyMatch;

  private InputDataExtractor primaryDataSource = new ContextMapData();


  public SimpleSpecification(String variableName, boolean isMultiple, boolean useGroup) {
		this.variableName = variableName;
		this.isExtractMultiple = isMultiple;
		this.useGroup = useGroup;
        this.isFuzzyMatch = VariableUtils.isFuzzyMatch(variableName);

	}



  public boolean isExtractMultiple() {
    return isExtractMultiple;
  }



  public String getVariable() {
    return variableName;
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

  public boolean isFuzzyMatch() {
    return isFuzzyMatch;
  }




}
