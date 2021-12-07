/*
 * This class is an auto-generated source file for a HAPI
 * HL7 v2.x standard structure class.
 *
 * For more information, visit: http://hl7api.sourceforge.net/
 * 
 * The contents of this file are subject to the Mozilla Public License Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/ 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. 
 * 
 * The Original Code is "[file_name]".  Description: 
 * "[one_line_description]" 
 * 
 * The Initial Developer of the Original Code is University Health Network. Copyright (C) 
 * 2012.  All Rights Reserved. 
 * 
 * Contributor(s): ______________________________________. 
 * 
 * Alternatively, the contents of this file may be used under the terms of the 
 * GNU General Public License (the  "GPL"), in which case the provisions of the GPL are 
 * applicable instead of those above.  If you wish to allow use of your version of this 
 * file only under the terms of the GPL and not to allow others to use your version 
 * of this file under the MPL, indicate your decision by deleting  the provisions above 
 * and replace  them with the notice and other provisions required by the GPL License.  
 * If you do not delete the provisions above, a recipient may use your version of 
 * this file under either the MPL or the GPL. 
 * 
 */


package com.ibm.whpa.hl7.custom.message;

import ca.uhn.hl7v2.model.v26.group.*;
import ca.uhn.hl7v2.model.v26.segment.*;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.model.*;


/**
 * <p>Represents a HIST_RPT message structure (see chapter 3.3.9). This structure contains the 
 * following elements: </p>
 * 
 *                 "MSH|^~\\&|WHI BULK|WHI|WHI||20210709162149||RPT^HIST|20210709162149|P|2.6"
                +"PID|1|100008^^^FAC|||^Clinton||197005150000|M|||KEENE ST^^COLUMBIA^MO^65201^ US"
                +"PV1|||||||||||||||||||3.2191"
                +"OBR|||1206141230440|X71010^Xray chest 1 vw|||201206141232||||||||||||||||||F|||||||123456789^TEST^INTERP^^^|||123456789^TEST^TRANSCRIPT^^^"
                +"OBX|||||MEASUREMENTS:~ 2D ECHO~ LV Diastolic Diameter Base LX     3.9 cm                3.6-5.4~ Surjya Das MD~ (Electronically Signed)||||||F"
                ;

 * <ul>
		                 * <li>1: MSH (Message Header) <b> </b> </li>
		                 * <li>5: PID (Patient Identification) <b> </b> </li>
		                 * <li>7: PV1 (Patient Visit) <b> </b> </li>
		                 * <li>9: OBR () <b>optional</b> </li>
		                 * <li>10: OBX (Observation/Result) <b>optional repeating</b> </li>
 * </ul>
 * Modeled after: import ca.uhn.hl7v2.model.v26.message.ADT_A09;
 */
//@SuppressWarnings("unused")
public class HIST_RPT extends AbstractMessage  {

    /**
     * Creates a new HIST_RPT message with DefaultModelClassFactory. 
     */ 
    public HIST_RPT() { 
       this(new DefaultModelClassFactory());
    }

    /** 
     * Creates a new HIST_RPT message with custom ModelClassFactory.
     */
    public HIST_RPT(ModelClassFactory factory) {
       super(factory);
       init(factory);
    }

    private void init(ModelClassFactory factory) {
       try {
                                // parms:  ClassName, required, repeats
                          this.add(MSH.class, true, false);
	                          this.add(PID.class, true, false);
	                          this.add(PV1.class, true, false);
                              this.add(OBR.class, false, false);  // set not to repeat for now.
	                          this.add(OBX.class, false, true);
	       } catch(HL7Exception e) {
          log.error("Unexpected error creating HIST_RPT - this is probably a bug in the source code generator.", e);
       }
    }


    /** 
     * Returns "2.6"
     */
    public String getVersion() {
       return "2.6";
    }




    /**
     * <p>
     * Returns
     * MSH (Message Header) - creates it if necessary
     * </p>
     * 
     *
     */
    public MSH getMSH() { 
       return getTyped("MSH", MSH.class);
    }


    /**
     * <p>
     * Returns
     * PID (Patient Identification) - creates it if necessary
     * </p>
     * 
     *
     */
    public PID getPID() { 
       return getTyped("PID", PID.class);
    }



    /**
     * <p>
     * Returns
     * PV1 (Patient Visit) - creates it if necessary
     * </p>
     * 
     *
     */
    public PV1 getPV1() { 
       return getTyped("PV1", PV1.class);
    }

        /**
     * <p>
     * Returns
     * PV1 (Patient Visit) - creates it if necessary
     * </p>
     * 
     *
     */
    public OBR getOBR() { 
        return getTyped("OBR", OBR.class);
     }


    /**
     * <p>
     * Returns
     * the first repetition of 
     * OBX (Observation/Result) - creates it if necessary
     * </p>
     * 
     *
     */
    public OBX getOBX() { 
       return getTyped("OBX", OBX.class);
    }


    /**
     * <p>
     * Returns a specific repetition of
     * OBX (Observation/Result) - creates it if necessary
     * </p>
     * 
     *
     * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
     * @throws HL7Exception if the repetition requested is more than one 
     *     greater than the number of existing repetitions.
     */
    public OBX getOBX(int rep) { 
       return getTyped("OBX", rep, OBX.class);
    }

    /** 
     * <p>
     * Returns the number of existing repetitions of OBX 
     * </p>
     * 
     */ 
    public int getOBXReps() { 
    	return getReps("OBX");
    } 

    /** 
     * <p>
     * Returns a non-modifiable List containing all current existing repetitions of OBX.
     * <p>
     * <p>
     * Note that unlike {@link #getOBX()}, this method will not create any reps
     * if none are already present, so an empty list may be returned.
     * </p>
     * 
     */ 
    public java.util.List<OBX> getOBXAll() throws HL7Exception {
    	return getAllAsList("OBX", OBX.class);
    } 

    /**
     * <p>
     * Inserts a specific repetition of OBX (Observation/Result)
     * </p>
     * 
     *
     * @see AbstractGroup#insertRepetition(Structure, int) 
     */
    public void insertOBX(OBX structure, int rep) throws HL7Exception { 
       super.insertRepetition( "OBX", structure, rep);
    }


    /**
     * <p>
     * Inserts a specific repetition of OBX (Observation/Result)
     * </p>
     * 
     *
     * @see AbstractGroup#insertRepetition(Structure, int) 
     */
    public OBX insertOBX(int rep) throws HL7Exception { 
       return (OBX)super.insertRepetition("OBX", rep);
    }


    /**
     * <p>
     * Removes a specific repetition of OBX (Observation/Result)
     * </p>
     * 
     *
     * @see AbstractGroup#removeRepetition(String, int) 
     */
    public OBX removeOBX(int rep) throws HL7Exception { 
       return (OBX)super.removeRepetition("OBX", rep);
    }



}

