/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.varable;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.hl7.expression.variable.DataTypeVariable;
import io.github.linuxforhealth.hl7.expression.variable.ExpressionVariable;
import io.github.linuxforhealth.hl7.expression.variable.VariableGenerator;

class VariableGeneratorTest {

  /**
   *
   * Test that parse expressions with * at the end works 
   * 
   * var1: STRING, OBX-5 *
   * 
   * @throws IOException
   */
  @Test
  void parseDataTypeVariableWithAsterixAtEnd() throws IOException {
	  String varName = "var1";
	  String variableExpression = "STRING, OBX-5 *";
	  DataTypeVariable v = (DataTypeVariable) VariableGenerator.parse(varName, variableExpression);	
	  
	  Assertions.assertTrue(v.getVariableName().equalsIgnoreCase(varName), "Variable name not set correctly");
	  Assertions.assertTrue(v.getSpec().get(0).equalsIgnoreCase("OBX-5"), "Variable spec not set correctly");
	  Assertions.assertTrue(v.getValueType().equalsIgnoreCase("STRING"), "Variable type not set correctly");
	  Assertions.assertTrue(v.extractMultiple(), "Variable extract multiple should be true");
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
  void parseExpressionVariableWithAsterixAndEvaluatingJavaFunction() throws IOException {
	  String varName = "var1";
	  String variableExpression = "OBX.5 *, GeneralUtils.testFunction(x, y)";
	  ExpressionVariable v = (ExpressionVariable) VariableGenerator.parse(varName, variableExpression);	
	  
	  Assertions.assertTrue(v.getVariableName().equalsIgnoreCase(varName), "Variable name not set correctly");
	  Assertions.assertTrue(v.getSpec().get(0).equalsIgnoreCase("OBX.5"), "Variable spec not set correctly");
	  Assertions.assertTrue(v.getExpression().equalsIgnoreCase(" GeneralUtils.testFunction(x, y)"), "Variable expression not set correctly");
	  Assertions.assertTrue(v.extractMultiple(), "Variable extract multiple should be true");
  }
}