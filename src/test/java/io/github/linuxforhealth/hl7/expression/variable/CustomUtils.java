package io.github.linuxforhealth.hl7.expression.variable;

/*
 * (C) Copyright Te Whatu Ora - Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * A trivial set of customFunctions - to be called by JEXL
 */
import io.github.linuxforhealth.hl7.data.Hl7DataHandlerUtil;

import java.util.Map;

public class CustomUtils {

    // Turn Y,N,U unto "yes", "no" or "unknown"  - for Code fields
    public static String ynuCode(Object input) {

        String ynu = Hl7DataHandlerUtil.getStringValue(input);
        Map<String, String> ynuMap = Map.of("Y", "yes",
                                            "N", "no",
                                            "U", "unknown");

        return ynuMap.get(ynu);
    }
}
