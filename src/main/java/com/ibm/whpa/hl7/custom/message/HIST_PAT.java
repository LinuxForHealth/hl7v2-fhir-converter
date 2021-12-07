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
 * Represents a HIST_PAT message structure (see chapter 3.3.9). This structure contains the
 * following elements:
 * </p>
 * 
 * 
 * MSH|^~\&|WHI BULK|WHI|WHI||20210726104500||HIST^PAT|20210726104500|P|2.6
 * PID|1|100021^^^FAC|100021^^^FAC||Lambert^Miranda||196705270000|F|||311 Keene St^^COLUMBIA^MO^65201^
 * US||5733551967|||U
 * ++ PRB|1|20210726|11334^ABNORMALITIES OF HAIR^ICD9||||201208070948||201208070948|||||||201208070948
 * ++ AL1|90|MA|dog|MO|itch|20210629
 * 
 * <ul>
 * <li>1: MSH (Message Header) <b> </b></li>
 * <li>5: PID (Patient Identification) <b> </b></li>
 * <li>9: AL1 (Allergy) <b>optional repeating</b></li>
 * <li>10: PRB (Problem Details) <b>optional repeating</b></li>
 * </ul>
 * Modeled after: import ca.uhn.hl7v2.model.v26.message.ADT_A09;
 */
//@SuppressWarnings("unused")
public class HIST_PAT extends AbstractMessage {

   /**
    * Creates a new HIST_PAT message with DefaultModelClassFactory.
    */
   public HIST_PAT() {
      this(new DefaultModelClassFactory());
   }

   /**
    * Creates a new HIST_PAT message with custom ModelClassFactory.
    */
   public HIST_PAT(ModelClassFactory factory) {
      super(factory);
      init(factory);
   }

   private void init(ModelClassFactory factory) {
      try {
         // parms:  ClassName, required, repeats
         this.add(MSH.class, true, false);
         this.add(PID.class, true, false);
         this.add(PRB.class, false, true);
         this.add(AL1.class, false, true);
      } catch (HL7Exception e) {
         log.error("Unexpected error creating HIST_PAT - this is probably a bug in the source code generator.", e);
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
    * PRB (Problem Details) - creates it if necessary
    * </p>
    * 
    *
    */
   public PRB getPRB() {
      return getTyped("PRB", PRB.class);
   }

   /**
    * <p>
    * Returns a specific repetition of
    * PRB (Problem Details) - creates it if necessary
    * </p>
    * 
    *
    * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
    * @throws HL7Exception if the repetition requested is more than one
    *            greater than the number of existing repetitions.
    */
   public PRB getPRB(int rep) {
      return getTyped("PRB", rep, PRB.class);
   }

   /**
    * <p>
    * Returns the number of existing repetitions of PRB
    * </p>
    * 
    */
   public int getPRBReps() {
      return getReps("PRB");
   }

   /**
    * <p>
    * Returns a non-modifiable List containing all current existing repetitions of PRB.
    * <p>
    * <p>
    * Note that unlike {@link #getPRB()}, this method will not create any reps
    * if none are already present, so an empty list may be returned.
    * </p>
    * 
    */
   public java.util.List<PRB> getPRBAll() throws HL7Exception {
      return getAllAsList("PRB", PRB.class);
   }

   /**
    * <p>
    * Inserts a specific repetition of PRB (Problem Details)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public void insertPRB(PRB structure, int rep) throws HL7Exception {
      super.insertRepetition("PRB", structure, rep);
   }

   /**
    * <p>
    * Inserts a specific repetition of PRB (Problem Details)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public PRB insertPRB(int rep) throws HL7Exception {
      return (PRB) super.insertRepetition("PRB", rep);
   }

   /**
    * <p>
    * Removes a specific repetition of PRB (Problem Details)
    * </p>
    * 
    *
    * @see AbstractGroup#removeRepetition(String, int)
    */
   public PRB removePRB(int rep) throws HL7Exception {
      return (PRB) super.removeRepetition("PRB", rep);
   }

   /**
    * <p>
    * Returns
    * the first repetition of
    * AL1 (Patient Allergy Information) - creates it if necessary
    * </p>
    * 
    *
    */
   public AL1 getAL1() {
      return getTyped("AL1", AL1.class);
   }

   /**
    * <p>
    * Returns a specific repetition of
    * AL1 (Patient Allergy Information) - creates it if necessary
    * </p>
    * 
    *
    * @param rep The repetition index (0-indexed, i.e. the first repetition is at index 0)
    * @throws HL7Exception if the repetition requested is more than one
    *            greater than the number of existing repetitions.
    */
   public AL1 getAL1(int rep) {
      return getTyped("AL1", rep, AL1.class);
   }

   /**
    * <p>
    * Returns the number of existing repetitions of AL1
    * </p>
    * 
    */
   public int getAL1Reps() {
      return getReps("AL1");
   }

   /**
    * <p>
    * Returns a non-modifiable List containing all current existing repetitions of AL1.
    * <p>
    * <p>
    * Note that unlike {@link #getAL1()}, this method will not create any reps
    * if none are already present, so an empty list may be returned.
    * </p>
    * 
    */
   public java.util.List<AL1> getAL1All() throws HL7Exception {
      return getAllAsList("AL1", AL1.class);
   }

   /**
    * <p>
    * Inserts a specific repetition of AL1 (Patient Allergy Information)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public void insertAL1(AL1 structure, int rep) throws HL7Exception {
      super.insertRepetition("AL1", structure, rep);
   }

   /**
    * <p>
    * Inserts a specific repetition of AL1 (Patient Allergy Information)
    * </p>
    * 
    *
    * @see AbstractGroup#insertRepetition(Structure, int)
    */
   public AL1 insertAL1(int rep) throws HL7Exception {
      return (AL1) super.insertRepetition("AL1", rep);
   }

   /**
    * <p>
    * Removes a specific repetition of AL1 (Patient Allergy Information)
    * </p>
    * 
    *
    * @see AbstractGroup#removeRepetition(String, int)
    */
   public AL1 removeAL1(int rep) throws HL7Exception {
      return (AL1) super.removeRepetition("AL1", rep);
   }

}
