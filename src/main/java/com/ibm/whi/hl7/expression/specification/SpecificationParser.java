package com.ibm.whi.hl7.expression.specification;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import com.ibm.whi.api.Specification;
import com.ibm.whi.hl7.message.util.SupportedSegments;

public class SpecificationParser {
  private SpecificationParser() {}


  public static Specification parse(String rawSpec, boolean extractMultiple, boolean useGroup) {
    if (StringUtils.startsWith(rawSpec, "$")) {
      return new SimpleSpecification(rawSpec, extractMultiple, useGroup);
    } else {
      return getHL7Spec(rawSpec, extractMultiple);
    }
  }

  private static Specification getHL7Spec(String rawSpec, boolean extractMultiple) {
    StringTokenizer stk = new StringTokenizer(rawSpec, ".");
    String segment = null;
    String field = null;
    int component = -1;
    int subComponent = -1;
    if (stk.hasNext()) {
      String tok = stk.next();
      if (EnumUtils.isValidEnumIgnoreCase(SupportedSegments.class, tok)) {
        segment = tok;
        if (stk.hasNext()) {
          field = stk.nextToken();
        }
        if (stk.hasNext()) {
          component = NumberUtils.toInt(stk.nextToken());
        }


      } else {
        field = tok;
        if (stk.hasNext()) {
          component = NumberUtils.toInt(stk.nextToken());
        }
        if (stk.hasNext()) {
          subComponent = NumberUtils.toInt(stk.nextToken());
        }
      }
    }


    return new HL7Specification(segment, field, component, subComponent, extractMultiple);
  }



}
