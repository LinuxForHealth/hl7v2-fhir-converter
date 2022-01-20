/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;
import ca.uhn.hl7v2.model.primitive.ID;
import ca.uhn.hl7v2.model.primitive.IS;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.ST;

public class Hl7DataHandlerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7DataHandlerUtil.class);

    private Hl7DataHandlerUtil() {
    }

    public static String getStringValue(Object obj) {
        return getStringValue(obj, false);
    }

    public static String getStringValue(Object obj, boolean allComponents) {
        return getStringValue(obj, allComponents, ". ", true, true);
    }

    /**
     * @param obj - object to return the string value of
     * @param allComponents - should all components be handled in same call?
     * @param separatorString - string to insert between items when obj is a list
     * @param trim - whether to trim (whitespace) from the result
     * @param separatorAtEnd - whether to add the separatorString after the final element when obj parm is a list
     * @return The string value
     */
    public static String getStringValue(Object obj, boolean allComponents, String separatorString, boolean trim,
            boolean separatorAtEnd) {
        if (obj == null) {
            return null;
        }
        LOGGER.debug("Extracting string value for {} type {}", obj, obj.getClass());

        Object local = obj;
        String returnValue;
        if (local instanceof Collection) {
            List<Object> list = ((List) local);
            if (list.size() == 1) {
                returnValue = toStringValue(list.get(0), allComponents);
            } else if (!list.isEmpty()) {
                int listSize = list.size();
                int listCount = 1;
                StringBuilder sb = new StringBuilder();
                for (Object listItem : list) {
                    sb.append(toStringValue(listItem, allComponents));
                    if (separatorAtEnd || listCount < listSize) {
                        // append separator, except after last item when separatorAtEnd=false
                        sb.append(separatorString);
                    }
                    listCount++;
                }
                returnValue = sb.toString();
                if (trim) {
                    returnValue = StringUtils.strip(returnValue);
                }
            } else {
                returnValue = null;
            }

        } else {
            returnValue = toStringValue(local, allComponents);
        }

        return returnValue;
    }

    public static String getTableNumber(Object obj) {
        if (obj instanceof ID) {
            ID id = (ID) obj;
            return "v2-" + StringUtils.leftPad(String.valueOf(id.getTable()), 4, '0');
        } else if (obj instanceof IS) {
            IS id = (IS) obj;
            return "v2-" + StringUtils.leftPad(String.valueOf(id.getTable()), 4, '0');
        } else if (obj instanceof CWE) {
            CWE id = (CWE) obj;
            return getAssociatedtable(id);
        }
        return null;
    }

    public static String getOriginalDisplayText(Object obj) {
        if (obj instanceof CWE) {
            CWE id = (CWE) obj;
            ST st = id.getText();
            if (st != null) {
                String str = st.getValue();
                if (str != null) {
                    return str.trim();
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public static String getVersion(Object obj) {
        if (obj instanceof CWE) {
            CWE id = (CWE) obj;
            ST st = id.getCodingSystemVersionID();
            if (st != null) {
                String str = st.getValue();
                if (str != null) {
                    return str.trim();
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private static String getAssociatedtable(CWE id) {
        ID val = id.getCwe3_NameOfCodingSystem();
        if (val != null && StringUtils.startsWith(val.getValue(), "HL7")) {
            return "v2-" + StringUtils
                    .leftPad(String.valueOf(StringUtils.removeStart(val.getValue(), "HL7")), 4, '0');
        } else if (val != null) {
            String returnValue = val.getValue();
            return returnValue != null ? returnValue.trim() : null;
        }
        return null;
    }

    private static String toStringValue(Object local, boolean allComponents) {
        if (local == null) {
            return null;
        }
        String returnvalue;
        if (local instanceof Variable) {
            returnvalue = convertVariesDataTypeToString(local, allComponents);
        } else if (local instanceof Composite) {
            Composite com = (Composite) local;
            if (allComponents) {
                returnvalue = getValueFromComposite(com);
            } else {
                try {
                    returnvalue = com.getComponent(0).toString();
                } catch (DataTypeException e) {
                    LOGGER.warn("Failure when extracting string value");
                    LOGGER.debug("Failure when extracting string value for {}", local, e);
                    returnvalue = null;
                }
            }
        } else if (local instanceof Primitive) {
            Primitive prem = (Primitive) local;
            returnvalue = prem.getValue();
        } else {
            returnvalue = local.toString();
        }

        return returnvalue;
    }

    private static String convertVariesDataTypeToString(Object obj, boolean allComponents) {
        if (obj instanceof Variable) {
            Variable v = (Variable) obj;
            return getStringValue(v.getData(), allComponents);
        }
        return obj.toString();
    }

    private static String getValueFromComposite(Composite com) {
        Type[] types = com.getComponents();
        StringBuilder sb = new StringBuilder();
        for (Type t : types) {
            String text = t.toString();
            if (StringUtils.isNotBlank(text)) {
                sb.append(t.toString()).append(", ");
            }
        }
        return StringUtils.stripEnd(sb.toString(), ", ");
    }

}
