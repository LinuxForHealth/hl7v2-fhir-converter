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
 * <p>
 * Represents a HIST_ENC message structure (see chapter 3.3.9). This structure contains the
 * following elements:
 * </p>
 * 
 * 
 * MSH|^~\&|WHI BULK|WHI|WHI||20210709142435||HIST^ENC|20210709142435|P|2.6
 * PID|1|100000^^^FAC|100000^^^FAC||Vickers^Tony||197910280000|M|||229 S Tyler St^^BEVERLY HILLS^FL^34465^ US
 * ++ OBR|1|||X73600^XRay Ankle 2
 * views^INTERNAL||201206080800||||||||||123456789^TEST^ORDERING^^^||||||||||||||||||89^TEST^TECH^^^
 * ++ RXE|1|20^Ibuprofen||||||||100|MG|||||2||||||||||||||||201704010000
 * ++ DG1|1||R03.0^Elevated blood-pressure reading, without diagnosis of hypertension|Elevated blood-pressure reading,
 * without diagnosis of hypertension|202004101405
 * 
 * <ul>
 * <li>1: MSH (Message Header) <b> </b></li>
 * <li>5: PID (Patient Identification) <b> </b></li>
 * <li>9: OBR (Observation) <b>optional repeating</b></li>
 * <li>9: RXE (Prescriptions) <b>optional repeating</b></li>
 * <li>10: DG1 (Diagnosis) <b>optional repeating</b></li>
 * </ul>
 * Modeled after: import ca.uhn.hl7v2.model.v26.message.ADT_A09;
 */
//@SuppressWarnings("unused")
public class HIST_ENC extends AbstractMessage {

   /**
    * Creates a new HIST_ENC message with DefaultModelClassFactory.
    */
   public HIST_ENC() {
      this(new DefaultModelClassFactory());
   }

   /**
    * Creates a new HIST_ENC message with custom ModelClassFactory.
    */
   public HIST_ENC(ModelClassFactory factory) {
      super(factory);
      init(factory);
   }

   private void init(ModelClassFactory factory) {
      try {
         // parms:  ClassName, required, repeats
         this.add(MSH.class, true, false);
         this.add(PID.class, true, false);
         this.add(OBR.class, false, true);
         this.add(DG1.class, false, true);
      } catch (HL7Exception e) {
         log.error("Unexpected error creating HIST_ENC - this is probably a bug in the source code generator.", e);
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
    * the first repetition of
    * OBR (Observation Request) - creates it if necessary
    * </p>
    * 
    *
    */
   public OBR getOBR() {
      return getTyped("OBR", OBR.class);
   }

   /**
    * <p>
    * Returns a specific repetition of
    * OBR (Observation Request) - creates it if necessary
    * </p>
    * 
    *
    * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
    * @throws HL7Exception if the repetition requested is more than one
    *            greater than the number of existing repetitions.
    */
   public OBR getOBR(int rep) {
      return getTyped("OBR", rep, OBR.class);
   }

   /**
    * <p>
    * Returns the number of existing repetitions of OBR
    * </p>
    * 
    */
   public int getOBRReps() {
      return getReps("OBR");
   }

   /**
    * <p>
    * Returns a non-modifiable List containing all current existing repetitions of OBR.
    * <p>
    * <p>
    * Note that unlike {@link #getOBR()}, this method will not create any reps
    * if none are already present, so an empty list may be returned.
    * </p>
    * 
    */
   public java.util.List<OBR> getOBRAll() throws HL7Exception {
      return getAllAsList("OBR", OBR.class);
   }

   /**
    * <p>
    * Inserts a specific repetition of OBR (Observation Request)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public void insertOBR(OBR structure, int rep) throws HL7Exception {
      super.insertRepetition("OBR", structure, rep);
   }

   /**
    * <p>
    * Inserts a specific repetition of OBR (Observation Request)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public OBR insertOBR(int rep) throws HL7Exception {
      return (OBR) super.insertRepetition("OBR", rep);
   }

   /**
    * <p>
    * Removes a specific repetition of OBR (Observation Request)
    * </p>
    * 
    *
    * @see AbstractGroup#removeRepetition(String, int)
    */
   public OBR removeOBR(int rep) throws HL7Exception {
      return (OBR) super.removeRepetition("OBR", rep);
   }

   /**
    * <p>
    * Returns
    * the first repetition of
    * DG1 (Diagnosis) - creates it if necessary
    * </p>
    * 
    *
    */
   public DG1 getDG1() {
      return getTyped("DG1", DG1.class);
   }

   /**
    * <p>
    * Returns a specific repetition of
    * DG1 (Diagnosis) - creates it if necessary
    * </p>
    * 
    *
    * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
    * @throws HL7Exception if the repetition requested is more than one
    *            greater than the number of existing repetitions.
    */
   public DG1 getDG1(int rep) {
      return getTyped("DG1", rep, DG1.class);
   }

   /**
    * <p>
    * Returns the number of existing repetitions of DG1
    * </p>
    * 
    */
   public int getDG1Reps() {
      return getReps("DG1");
   }

   /**
    * <p>
    * Returns a non-modifiable List containing all current existing repetitions of DG1.
    * <p>
    * <p>
    * Note that unlike {@link #getDG1()}, this method will not create any reps
    * if none are already present, so an empty list may be returned.
    * </p>
    * 
    */
   public java.util.List<DG1> getDG1All() throws HL7Exception {
      return getAllAsList("DG1", DG1.class);
   }

   /**
    * <p>
    * Inserts a specific repetition of DG1 (Diagnosis)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public void insertDG1(DG1 structure, int rep) throws HL7Exception {
      super.insertRepetition("DG1", structure, rep);
   }

   /**
    * <p>
    * Inserts a specific repetition of DG1 (Diagnosis)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public DG1 insertDG1(int rep) throws HL7Exception {
      return (DG1) super.insertRepetition("DG1", rep);
   }

   /**
    * <p>
    * Removes a specific repetition of DG1 (Diagnosis)
    * </p>
    * 
    *
    * @see AbstractGroup#removeRepetition(String, int)
    */
   public DG1 removeDG1(int rep) throws HL7Exception {
      return (DG1) super.removeRepetition("DG1", rep);
   }

   /**
    * <p>
    * Returns
    * the first repetition of
    * RXE (Prescription) - creates it if necessary
    * </p>
    * 
    *
    */
   public RXE getRXE() {
      return getTyped("RXE", RXE.class);
   }

   /**
    * <p>
    * Returns a specific repetition of
    * RXE (Prescription) - creates it if necessary
    * </p>
    * 
    *
    * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
    * @throws HL7Exception if the repetition requested is more than one
    *            greater than the number of existing repetitions.
    */
   public RXE getRXE(int rep) {
      return getTyped("RXE", rep, RXE.class);
   }

   /**
    * <p>
    * Returns the number of existing repetitions of RXE
    * </p>
    * 
    */
   public int getRXEReps() {
      return getReps("RXE");
   }

   /**
    * <p>
    * Returns a non-modifiable List containing all current existing repetitions of RXE.
    * <p>
    * <p>
    * Note that unlike {@link #getRXE()}, this method will not create any reps
    * if none are already present, so an empty list may be returned.
    * </p>
    * 
    */
   public java.util.List<RXE> getRXEAll() throws HL7Exception {
      return getAllAsList("RXE", RXE.class);
   }

   /**
    * <p>
    * Inserts a specific repetition of RXE (Prescription)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public void insertRXE(RXE structure, int rep) throws HL7Exception {
      super.insertRepetition("RXE", structure, rep);
   }

   /**
    * <p>
    * Inserts a specific repetition of RXE (Prescription)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public RXE insertRXE(int rep) throws HL7Exception {
      return (RXE) super.insertRepetition("RXE", rep);
   }

   /**
    * <p>
    * Removes a specific repetition of RXE (Prescription)
    * </p>
    * 
    *
    * @see AbstractGroup#removeRepetition(String, int)
    */
   public RXE removeRXE(int rep) throws HL7Exception {
      return (RXE) super.removeRepetition("RXE", rep);
   }

}
