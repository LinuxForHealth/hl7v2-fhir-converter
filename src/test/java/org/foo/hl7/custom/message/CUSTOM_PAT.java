/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.foo.hl7.custom.message;

import ca.uhn.hl7v2.model.v26.segment.*;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.model.*;

/**
 * <p>
 * Represents a CUSTOM_PAT message structure. This structure
 * contains the following elements:
 * </p>
 * <ul>
 * <li>MSH (Message Header) <b> </b></li>
 * <li>PID (Patient Identification) <b> </b></li>
 * <li>PRB (Problem Details) <b>optional repeating</b></li>
 * <li>AL1 (Allergy) <b>optional repeating</b></li>
 * </ul>
 *
 * Sample Message:
 * MSH|^~\\&|||||20210709162149||HIST^PAT|20210709162149|P|2.6
 * PID|1|100021^^^FAC|100021^^^FAC||DOE^JANE||196705270000|F|||||5733551967|||U ++
 * PRB|1|20210726|11334^ABNORMALITIES OF
 * HAIR^ICD9||||201208070948||201208070948|||||||201208070948 ++
 * AL1|90|MA|dog|MO|itch|20210629
 */

public class CUSTOM_PAT extends AbstractMessage {

	/**
	 * Creates a new CUSTOM_PAT message with DefaultModelClassFactory.
	 */
	public CUSTOM_PAT() {
		this(new DefaultModelClassFactory());
	}

	/**
	 * Creates a new CUSTOM_PAT message with custom ModelClassFactory.
	 */
	public CUSTOM_PAT(ModelClassFactory factory) {
		super(factory);
		init(factory);
	}

	private void init(ModelClassFactory factory) {
		try {
			// parms: ClassName, required, repeats
			this.add(MSH.class, true, false);
			this.add(PID.class, true, false);
			this.add(PRB.class, false, true);
			this.add(AL1.class, false, true);
		} catch (HL7Exception e) {
			log.error("Unexpected error creating CUSTOM_PAT - this is probably a bug in the source code generator.");
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
	 * Returns MSH (Message Header) - creates it if necessary
	 * </p>
	 *
	 *
	 */
	public MSH getMSH() {
		return getTyped("MSH", MSH.class);
	}

	/**
	 * <p>
	 * Returns PID (Patient Identification) - creates it if necessary
	 * </p>
	 *
	 *
	 */
	public PID getPID() {
		return getTyped("PID", PID.class);
	}

	/**
	 * <p>
	 * Returns the first repetition of PRB (Problem Details) - creates it if
	 * necessary
	 * </p>
	 *
	 *
	 */
	public PRB getPRB() {
		return getTyped("PRB", PRB.class);
	}

	/**
	 * <p>
	 * Returns a specific repetition of PRB (Problem Details) - creates it if
	 * necessary
	 * </p>
	 *
	 *
	 * @param rep The repetition index (0-indexed, i.e. the first repetition is at
	 *            index 0)
	 * @throws HL7Exception if the repetition requested is more than one greater
	 *                      than the number of existing repetitions.
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
	 * Returns a non-modifiable List containing all current existing repetitions of
	 * PRB.
	 * <p>
	 * <p>
	 * Note that unlike {@link #getPRB()}, this method will not create any reps if
	 * none are already present, so an empty list may be returned.
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
	 * Returns the first repetition of AL1 (Patient Allergy Information) - creates
	 * it if necessary
	 * </p>
	 *
	 *
	 */
	public AL1 getAL1() {
		return getTyped("AL1", AL1.class);
	}

	/**
	 * <p>
	 * Returns a specific repetition of AL1 (Patient Allergy Information) - creates
	 * it if necessary
	 * </p>
	 *
	 *
	 * @param rep The repetition index (0-indexed, i.e. the first repetition is at
	 *            index 0)
	 * @throws HL7Exception if the repetition requested is more than one greater
	 *                      than the number of existing repetitions.
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
	 * Returns a non-modifiable List containing all current existing repetitions of
	 * AL1.
	 * <p>
	 * <p>
	 * Note that unlike {@link #getAL1()}, this method will not create any reps if
	 * none are already present, so an empty list may be returned.
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
