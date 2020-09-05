package com.ibm.whi.core;

import java.io.File;

public class Constants {

  public static final File DEFAULT_HL7_RESOURCES = new File("src/main/resources/hl7");
  public static final File DEAFULT_HL7_MESSAGE_FOLDER = new File(DEFAULT_HL7_RESOURCES, "message");
  private Constants() {}
}
