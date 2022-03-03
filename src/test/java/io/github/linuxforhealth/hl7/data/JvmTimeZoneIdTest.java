/*
 * (C) Copyright IBM Corp. 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.data.date.DateUtil;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

// This is a specialized test to see that with no configured time zone, the JVM time zone is used.

class JvmTimeZoneIdTest {
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    // These variables and before and after test processing are needed to
    // Store the original config, set our own config, then restore the original config.

    private static final String CONF_PROP_HOME = "hl7converter.config.home";

    @TempDir
    static File folder;
    static String originalConfigHome;

    @BeforeAll
    static void saveConfigHomeProperty() {
        originalConfigHome = System.getProperty(CONF_PROP_HOME);
    }

    @AfterEach
    void reset() {
        System.clearProperty(CONF_PROP_HOME);
        ConverterConfiguration.reset();
        UrlLookup.reset();
    }

    @AfterAll
    static void reloadPreviousConfigurations() {
        if (originalConfigHome != null)
            System.setProperty(CONF_PROP_HOME, originalConfigHome);
        else
            System.clearProperty(CONF_PROP_HOME);
        UrlLookup.reset();
    }

    @Test
    void testEmptyDefaultTimeZoneYieldsJVMZoneId() throws IOException {
        // Create our own properties file
        File configFile = new File(folder, "config.properties");
        writeSimpleProperties(configFile);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
        ConverterConfiguration.reset();

        // Prove that we're using our custom properties file with no ZoneId
        ConverterConfiguration theConvConfig = ConverterConfiguration.getInstance();
        assertThat(theConvConfig.getSupportedMessageTemplates()).hasSize(13); // Four messages supported.  (Proves we're using our created file, not the default.)
        assertThat(theConvConfig.getZoneId()).isNull(); // Purposely empty

        // IMPORTANT: TimeZoneId's are different than an offset.  TimeZoneId's are a location.  
        // The offset of the location changes depending on whether Daylight savings time is in effect.
        // Because we compare after processing, we can't compare locations, only offsets.
        // It is critical that when we compare offsets, we start with the same date, so the same daylight savings rules apply!
        // Otherwise a test might work only half of the year.

        // Calculate the local server zone offset
        LocalDateTime localDateTime = LocalDateTime.of(2002, Month.FEBRUARY, 2, 2, 0, 0); //20020202020000
        String defaultLocalZone = TimeZone.getDefault().getID();
        ZoneId localZoneId = ZoneId.of(defaultLocalZone);
        ZonedDateTime localZonedDateTime = localDateTime.atZone(localZoneId);
        ZoneOffset localOffset = localZonedDateTime.getOffset();

        // PART 1
        // Test the format utility (which will fallback to local server time and zone offset)
        String testDateTime = DateUtil.formatToDateTimeWithDefaultZone("20020202020000");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime testZonedDateTime = ZonedDateTime.parse(testDateTime, dateTimeFormatter);
        ZoneOffset testOffset = testZonedDateTime.getOffset();

        // Offset from our function call test should equal offset of the local time
        assertThat(testOffset).isEqualTo(localOffset);

        // PART 2
        // Do the same for a date going through the entire conversion
        String hl7message = "MSH|^~\\&|||||20020202020000|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                // PRB.2 to recordedDateTime (check time ZoneId)
                + "PRB|AD|20020202020000|K80.00^Cholelithiasis^I10|53956||||||||||||\r";

        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint().build();
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv, hl7message,
                customOptionsWithTenant);
        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Get the recordedDate value; convert it back to a zoned time; get the offset for comparison
        testDateTime = condition.getRecordedDateElement().getValueAsString(); // PRB.2 
        testZonedDateTime = ZonedDateTime.parse(testDateTime, dateTimeFormatter);
        testOffset = testZonedDateTime.getOffset();

        // Offset from our test should equal offset of the local time
        assertThat(testOffset).isEqualTo(localOffset);

        // After the test, the properties file resets.
    }

    private void writeSimpleProperties(File configFile) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.put("supported.hl7.messages",
                "ADT_A01, ADT_A03, DFT_P03, MDM_T02, MDM_T06, OML_O21, ORM_O01, OMP_O09, ORU_R01, PPR_PC1, RDE_O11, RDE_O25, VXU_V04");
        // default.time.zone purposely not set
        prop.store(new FileOutputStream(configFile), null);
    }

}
