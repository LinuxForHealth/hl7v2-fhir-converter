/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import io.github.linuxforhealth.api.Expression;

public enum ExpressionType {

  HL7SPEC(Hl7Expression.class), //
  RESOURCE(ResourceExpression.class), //
  REFERENCE(ReferenceExpression.class), //
  SIMPLE(SimpleExpression.class), //
  JEXL(JEXLExpression.class);


  private Class<? extends Expression> evaluator;


  ExpressionType(Class<? extends Expression> evaluator) {
    this.evaluator = evaluator;

  }

  public Class<? extends Expression> getEvaluator() {
    return evaluator;
  }

}
