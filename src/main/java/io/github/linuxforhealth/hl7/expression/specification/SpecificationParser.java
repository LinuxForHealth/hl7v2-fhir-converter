package io.github.linuxforhealth.hl7.expression.specification;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;

import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.hl7.message.util.SupportedSegments;

public class SpecificationParser {
  private SpecificationParser() {}

  public static Specification parse(String rawSpec, boolean extractMultiple, boolean useGroup) {
	  return parse(rawSpec, extractMultiple, useGroup, false);
  }
  
  public static Specification parse(String rawSpec, boolean extractMultiple, boolean useGroup, boolean retainEmpty) {
	if (StringUtils.startsWith(rawSpec, "$")) {
      return new SimpleSpecification(rawSpec, extractMultiple, useGroup);
    } else {
      return getHL7Spec(rawSpec, extractMultiple, retainEmpty);
    }
  }

  
  
  private static Specification getHL7Spec(String rawSpec, boolean extractMultiple, boolean retainEmpty) {
    StringTokenizer stk = new StringTokenizer(rawSpec, ".");
    String segment = null;
    String field = null;
    int component = -1;
    int subComponent = -1;
    if (stk.hasNext()) {
      String tok = stk.next();

      // tokens that start with Z are also valid SEGMENTS
      if (SupportedSegments.contains(tok) || tok.startsWith("Z")) {
        segment = tok;
        if (stk.hasNext()) {
          field = stk.nextToken();
        }
        if (stk.hasNext()) {
          component = NumberUtils.toInt(stk.nextToken());
        }

        // Don't forget the SubComponents - PID.5.1.2 & PID.5.1.3, can be quite useful
        //   - allowing us to separate the 'van' and 'van der' prefixes from names like Ludwig van Beethoven, and Cornelius van der Westhuizen
        if (stk.hasNext()) {
          subComponent = NumberUtils.toInt(stk.nextToken());
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

    return new HL7Specification(segment, field, component, subComponent, extractMultiple, retainEmpty);
  }

}
