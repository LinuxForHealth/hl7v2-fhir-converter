/*
 * (c) Te Whatu Ora, Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.message.HL7FHIRResourceCondition;

class HL7FHIRResourceConditionTest {


  // These tests check that we can correctly parse Resource.condition string to make it ready for evaluation
  @Test
  void testResourceConditionComponentEQUALS() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("AL1.2.1 EQUALS 12");

    assertThat(cond.fieldSpec).hasToString("[AL1.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.EQUALS);
    assertThat(cond.values).hasSize(1);
    assertThat(cond.values.get(0)).isEqualTo("12");
  }

  @Test
  void testResourceConditionComponentNOTEQUALS() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2.1 NOT_EQUALS 12");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.NOT_EQUALS);
    assertThat(cond.values).hasSize(1);
    assertThat(cond.values.get(0)).isEqualTo("12");
  }

  @Test
  void testResourceConditionComponentIN() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2.1 IN [L3, L4, H1, H2]");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.IN);
    assertThat(cond.values).hasSize(4);
    assertThat(cond.values.get(0)).isEqualTo("L3");
    assertThat(cond.values.get(1)).isEqualTo("L4");
    assertThat(cond.values.get(2)).isEqualTo("H1");
    assertThat(cond.values.get(3)).isEqualTo("H2");
  }

  @Test
  void testResourceConditionComponentNOTIN() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2.1 NOT_IN [L3, L4, H1, H2]");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.NOT_IN);
    assertThat(cond.values).hasSize(4);
    assertThat(cond.values.get(0)).isEqualTo("L3");
    assertThat(cond.values.get(1)).isEqualTo("L4");
    assertThat(cond.values.get(2)).isEqualTo("H1");
    assertThat(cond.values.get(3)).isEqualTo("H2");
  }

  @Test
  void testResourceConditionSubComponentEQUALS() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.3.2.1 EQUALS 13");

    assertThat(cond.fieldSpec).hasToString("[ZAL.3.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.EQUALS);
    assertThat(cond.values).hasSize(1).element(0).hasToString("13");
  }

  @Test
  void testResourceConditionFieldEQUALS() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2 EQUALS 13");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.EQUALS);
    assertThat(cond.values).hasSize(1).element(0).hasToString("13");
  }

  @Test
  void testResourceConditionSegmentEQUALS() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL EQUALS 13");

    assertThat(cond.fieldSpec).hasToString("[ZAL]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.EQUALS);
    assertThat(cond.values).hasSize(1).element(0).hasToString("13");
  }

  @Test
  void testResourceConditionSegmentISNULL() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2.1 NULL");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.NULL);
    assertThat(cond.values).hasSize(0);
  }

  @Test
  void testResourceConditionSegmentNOTNULL() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2.1 NOT_NULL");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2.1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.NOT_NULL);
    assertThat(cond.values).hasSize(0);
  }

  @Test
  void testResourceConditionSegmentFieldRepetition() {

    HL7FHIRResourceCondition cond = new HL7FHIRResourceCondition("ZAL.2(1).1 NOT_NULL");

    assertThat(cond.fieldSpec).hasToString("[ZAL.2(1).1]");
    assertThat(cond.op).isEqualTo(HL7FHIRResourceCondition.Operator.NOT_NULL);
    assertThat(cond.values).hasSize(0);
  }

}
