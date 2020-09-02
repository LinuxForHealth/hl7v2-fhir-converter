package com.ibm.whi.hl7.parsing.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ca.uhn.hl7v2.model.Structure;

/**
 * Represents the result of parsing HL7 message.
 * 
 *
 * @author {user}
 */
public class Hl7ParsingStructureResult implements ParsingResult<Structure> {


  private List<Structure> values;

  public Hl7ParsingStructureResult(List<Structure> values) {
    this.values = new ArrayList<>();
    if (values != null) {
      values.removeIf(Objects::isNull);
    this.values.addAll(values);
    }


  }

  public Hl7ParsingStructureResult(Structure value) {
    this.values = new ArrayList<>();
    if (value != null) {
      this.values.add(value);
    }

  }





  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  @Override
  public Structure getValue() {
    if (!this.values.isEmpty()) {
      return this.values.get(0);
    }
    return null;
  }

  @Override
  public List<Structure> getValues() {
    return new ArrayList<>(this.values);
  }

}
