/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import com.google.common.collect.Lists;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.OBX;

public class Hl7DataHandlerUtilTest {

  private static final String SOME_TEXT_VALUE = "SOME TEXT VALUE";
  private static final String SOME_OTHER_TEXT_VALUE = "SOME OTHER TEXT VALUE";

  @Test
  public void test_getStringValue_returns_value() {
    String value = "any string value";
    assertThat(Hl7DataHandlerUtil.getStringValue(value)).isEqualTo(value);
  }


  @Test
  public void test_getStringValue_returns_null_for_null_input() {
    assertThat(Hl7DataHandlerUtil.getStringValue(null)).isEqualTo(null);
  }

  @Test
  public void test_getStringValue_returns_value_for_hl7_primitive() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    TX tx = new TX(message);
    tx.setValue(SOME_TEXT_VALUE);
    assertThat(Hl7DataHandlerUtil.getStringValue(tx)).isEqualTo(SOME_TEXT_VALUE);
  }

  @Test
  public void test_getStringValue_returns_value_for_hl7_compositive() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    CWE ce = new CWE(message);
    ce.getIdentifier().setValue(SOME_TEXT_VALUE);
    ce.getText().setValue("Some Value");
    ce.getNameOfCodingSystem().setValue("SNM");
    assertThat(Hl7DataHandlerUtil.getStringValue(ce)).isEqualTo(SOME_TEXT_VALUE);
  }


  @Test
  public void test_getStringValue_returns_value_for_hl7_compositive_all_components()
      throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    CWE ce = new CWE(message);
    ce.getIdentifier().setValue(SOME_TEXT_VALUE);
    ce.getText().setValue("Some Value");
    ce.getNameOfCodingSystem().setValue("SNM");
    assertThat(Hl7DataHandlerUtil.getStringValue(ce, true))
        .isEqualTo(SOME_TEXT_VALUE + ", Some Value, SNM");
  }

  @Test
  public void test_getStringValue_returns_value_for_hl7_varies() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    ORU_R01_ORDER_OBSERVATION orderObservation = message.getPATIENT_RESULT().getORDER_OBSERVATION();
    ORU_R01_OBSERVATION observation = orderObservation.getOBSERVATION(0);
    OBX obx = observation.getOBX();
    TX tx = new TX(message);
    tx.setValue(SOME_TEXT_VALUE);
    Varies value = obx.getObservationValue(0);
    value.setData(tx);
    assertThat(Hl7DataHandlerUtil.getStringValue(value)).isEqualTo(SOME_TEXT_VALUE);
  }


  @Test
  public void test_getStringValue_returns_value_for_list_of_hl7_compositive()
      throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    CWE ce = new CWE(message);
    ce.getIdentifier().setValue(SOME_TEXT_VALUE);
    ce.getText().setValue("Some Value");
    ce.getNameOfCodingSystem().setValue("SNM");

    CWE ce2 = new CWE(message);
    ce2.getIdentifier().setValue(SOME_OTHER_TEXT_VALUE);
    ce2.getText().setValue("Some other Value");
    ce2.getNameOfCodingSystem().setValue("OTH");
    assertThat(Hl7DataHandlerUtil.getStringValue(Lists.newArrayList(ce, ce2)))
        .isEqualTo(SOME_TEXT_VALUE + ". " + SOME_OTHER_TEXT_VALUE + ".");
  }


  @Test
  public void test_getStringValue_returns_value_for_list_of_hl7_compositive_all_components()
      throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    CWE ce = new CWE(message);
    ce.getIdentifier().setValue(SOME_TEXT_VALUE);
    ce.getText().setValue("Some Value");
    ce.getNameOfCodingSystem().setValue("SNM");

    CWE ce2 = new CWE(message);
    ce2.getIdentifier().setValue(SOME_OTHER_TEXT_VALUE);
    ce2.getText().setValue("Some other Value");
    ce2.getNameOfCodingSystem().setValue("OTH");
    assertThat(Hl7DataHandlerUtil.getStringValue(Lists.newArrayList(ce, ce2), true))
        .isEqualTo(SOME_TEXT_VALUE + ", Some Value, SNM. " + SOME_OTHER_TEXT_VALUE
            + ", Some other Value, OTH.");
  }

}
