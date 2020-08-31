package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.whi.hl7.resource.ObjectMapperUtil;



public class DeserializationTest {
  private static final ObjectMapper objMapper = ObjectMapperUtil.getInstance();
  @Test
  public void test() throws JsonMappingException, JsonProcessingException {
    JsonNode jnode = objMapper.readTree("\"test\"");
    DefaultExpression exp = objMapper.convertValue(jnode, DefaultExpression.class);
    
    assertThat(exp.getValue()).isEqualTo("");
  }

}
