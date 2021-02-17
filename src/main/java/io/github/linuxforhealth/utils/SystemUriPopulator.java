package io.github.linuxforhealth.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linuxforhealth.core.ObjectMapperUtil;

public class SystemUriPopulator {

  public static void main(String[] args) throws IOException {
    // generateCodingTable();
    generateCodingTableAll();

  }

  private static void generateCodingTable() throws FileNotFoundException, IOException,
      JsonParseException, JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = ObjectMapperUtil.getJSONInstance();
    FileInputStream fis =
        new FileInputStream("/Users/pbhallam@us.ibm.com/Downloads/0396_table.txt");
    List<String> lines = IOUtils.readLines(fis, "UTF-8");
    File codingSystemFolder = new File("/Users/pbhallam@us.ibm.com/Downloads/package2");
    String filePrefix = "CodeSystem-";
    List<SystemURI> urls = new ArrayList<>();
    for (String line : lines) {
      String[] tokens = line.split("\\s", 2);
          //StringUtils.split(line,2);

      File path = new File(codingSystemFolder, filePrefix + tokens[0].strip() + ".json");
      // System.out.println(path.getAbsolutePath());
      if (path.exists()) {
        // System.out.println(path.getAbsolutePath());
        FileInputStream is = new FileInputStream(path);
        SystemURI uri = mapper.readValue(is, SystemURI.class);
        urls.add(uri);
      } else if (new File(codingSystemFolder,
          filePrefix + tokens[0].strip().toLowerCase() + ".json").exists()) {
        path = new File(codingSystemFolder, filePrefix + tokens[0].strip().toLowerCase() + ".json");
        System.out.println(path.getAbsolutePath());
        FileInputStream is = new FileInputStream(path);
        SystemURI uri = mapper.readValue(is, SystemURI.class);
        urls.add(uri);
      }

      else {
        System.out.println("NOT Found " + path.getAbsolutePath());
        SystemURI uri = new SystemURI(tokens[0], tokens[1].strip(), null, null);
        urls.add(uri);
      }

    }
    System.out.println(ObjectMapperUtil.getYAMLInstance().writeValueAsString(urls));
  }




  private static void generateCodingTableAll() throws FileNotFoundException, IOException,
      JsonParseException, JsonMappingException, JsonProcessingException {

    FileInputStream fis =
        new FileInputStream("/Users/pbhallam@us.ibm.com/Downloads/0396_table.txt");
    List<String> lines = IOUtils.readLines(fis, "UTF-8");
    File codingSystemFolder = new File("/Users/pbhallam@us.ibm.com/Downloads/package2");
    String filePrefix = "CodeSystem-";
    List<String> knownCodes = new ArrayList<>();
    for (String line : lines) {
      String[] tokens = line.split("\\s", 2);
      File path = new File(codingSystemFolder, filePrefix + tokens[0].strip() + ".json");
      knownCodes.add(path.getAbsolutePath());

    }

    ObjectMapper mapper = ObjectMapperUtil.getJSONInstance();
    List<SystemURI> urls = new ArrayList<>();
    FilenameFilter filter = (dir, name) -> name.startsWith("CodeSystem-");

    for (File path : codingSystemFolder.listFiles(filter)) {
      if (!knownCodes.contains(path.getAbsolutePath())) {
        FileInputStream is = new FileInputStream(path);
      SystemURI uri = mapper.readValue(is, SystemURI.class);
      urls.add(uri);
      }

    }
    ObjectMapperUtil.getYAMLInstance().writeValue(Paths.get("codesystem.yaml").toFile(), urls);
  }

}
