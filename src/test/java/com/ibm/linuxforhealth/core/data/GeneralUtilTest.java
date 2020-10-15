package com.ibm.linuxforhealth.core.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.OBX;

public class GeneralUtilTest {
  private static final String SOME_TEXT_VALUE = "SOME TEXT VALUE";

  @Test
  public void test_getDataType_returns_value() {
    String value = "any string value";
    assertThat(DataTypeUtil.getDataType(value)).isEqualTo(String.class.getSimpleName());
  }


  @Test
  public void test_getDataType_returns_null_for_null_input() {
    assertThat(DataTypeUtil.getDataType(null)).isEqualTo(null);
  }

  @Test
  public void test_getDataType_returns_value_for_hl7_primitive() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    TX tx = new TX(message);
    tx.setValue(SOME_TEXT_VALUE);
    assertThat(DataTypeUtil.getDataType(tx)).isEqualTo("TX");
  }

  @Test
  public void test_getDataTypee_returns_value_for_hl7_compositive() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    CWE ce = new CWE(message);
    ce.getIdentifier().setValue("SOME Identifier");
    ce.getText().setValue("Some Value");
    ce.getNameOfCodingSystem().setValue("SNM");
    assertThat(DataTypeUtil.getDataType(ce)).isEqualTo("CWE");
  }

  @Test
  public void test_getDataType_returns_value_for_hl7_varies() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    ORU_R01_ORDER_OBSERVATION orderObservation = message.getPATIENT_RESULT().getORDER_OBSERVATION();
    ORU_R01_OBSERVATION observation = orderObservation.getOBSERVATION(0);
    OBX obx = observation.getOBX();
    TX tx = new TX(message);
    tx.setValue(SOME_TEXT_VALUE);
    Varies value = obx.getObservationValue(0);
    value.setData(tx);
    assertThat(DataTypeUtil.getDataType(value)).isEqualTo("TX");
  }

}
