/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.parsing.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ca.uhn.hl7v2.model.Type;

/**
 * Represents the result of parsing HL7 message.
 * 
 *
 * @author {user}
 */
public class Hl7ParsingTypeResult implements ParsingResult<Type> {

  private List<Type> values;

  public Hl7ParsingTypeResult(List<Type> values) {
    this.values = new ArrayList<>();
    if (values != null) {
      values.removeIf(Objects::isNull);
    this.values.addAll(values);
    }

  }

  public Hl7ParsingTypeResult(Type value) {
    this.values = new ArrayList<>();
    if (value != null) {
      this.values.add(value);
    }

  }

  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  @Override
  public Type getValue() {
    if (!this.values.isEmpty()) {
      return this.values.get(0);
    }
    return null;
  }

  @Override
  public List<Type> getValues() {
    return new ArrayList<>(this.values);
  }


}
