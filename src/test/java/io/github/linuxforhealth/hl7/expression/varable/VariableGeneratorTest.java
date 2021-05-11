/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.varable;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import io.github.linuxforhealth.hl7.expression.variable.DataTypeVariable;
import io.github.linuxforhealth.hl7.expression.variable.ExpressionVariable;
import io.github.linuxforhealth.hl7.expression.variable.VariableGenerator;

public class VariableGeneratorTest {

  /**
   *
   * Test that parse expressions with * at the end works 
   * 
   * var1: STRING, OBX-5 *
   * 
   * @throws IOException
   */
  @Test
  public void parseDataTypeVariableWithAsterixAtEnd() throws IOException {
	  String varName = "var1";
	  String variableExpression = "STRING, OBX-5 *";
	  DataTypeVariable v = (DataTypeVariable) VariableGenerator.parse(varName, variableExpression);	
	  
	  Assert.assertTrue("Variable name not set correctly", v.getVariableName().equalsIgnoreCase(varName));
	  Assert.assertTrue("Variable spec not set correctly", v.getSpec().get(0).equalsIgnoreCase("OBX-5"));
	  Assert.assertTrue("Variable type not set correctly", v.getValueType().equalsIgnoreCase("STRING"));
	  Assert.assertTrue("Variable extract multiple should be true", v.extractMultiple());
  }

  /**
  *
  * Test that parse expressions with * and call to evaluate java function
  * 
  *  var1: OBX.5 *, GeneralUtils.testFunction(x, y)
  * 
  * @throws IOException
  */
  @Test
  public void parseExpressionVariableWithAsterixAndEvaluatingJavaFunction() throws IOException {
	  String varName = "var1";
	  String variableExpression = "OBX.5 *, GeneralUtils.testFunction(x, y)";
	  ExpressionVariable v = (ExpressionVariable) VariableGenerator.parse(varName, variableExpression);	
	  
	  Assert.assertTrue("Variable name not set correctly", v.getVariableName().equalsIgnoreCase(varName));
	  Assert.assertTrue("Variable spec not set correctly", v.getSpec().get(0).equalsIgnoreCase("OBX.5"));
	  Assert.assertTrue("Variable expression not set correctly", v.getExpression().equalsIgnoreCase(" GeneralUtils.testFunction(x, y)"));
	  Assert.assertTrue("Variable extract multiple should be true", v.extractMultiple());
  }
}