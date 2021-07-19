/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Utility class for mapping HL7 codes from tables
 * 
 * Load tables from files into a cache; provides lookup capability.
 * Use Constants (EXTENSION_URL_MAPPING, CODING_SYSTEM_MAPPING) for urlType.
 */
public class UrlLookup {
    static Map<String, Map<String, CodingSystem>> urlMaps = new HashMap<String, Map<String, CodingSystem>>(); // key is urlType
    static Map<String, String> urlMappingPaths; // key=urlType, value=resource mapping path
    static {
        urlMappingPaths = new HashMap<String, String>();
        urlMappingPaths.put(Constants.CODING_SYSTEM_MAPPING, Constants.CODING_SYSTEM_MAPPING_PATH);
        urlMappingPaths.put(Constants.EXTENSION_URL_MAPPING, Constants.EXTENSION_URL_MAPPING_PATH);
    }

    /**
     * Get the extension URL
     */
    public static String getExtensionUrl(String value) {
        return getUrl(Constants.EXTENSION_URL_MAPPING, value);
    }

    /**
     * Get the system associated with the value for the coding system.
     */
    public static String getSystemUrl(String value) {
        return getUrl(Constants.CODING_SYSTEM_MAPPING, value);
    }

    /**
     * Get the system associated with the value for the URL set.
     */
    public static String getUrl(String urlType, String value) {
        Map<String, CodingSystem> urlMap = getUrlMap(urlType);
        if (StringUtils.startsWith(value, "http://") || StringUtils.startsWith(value, "https://") || StringUtils.startsWith(value, "urn")) {
            return value;
        } else if (value != null) {
            CodingSystem system = urlMap.get(StringUtils.upperCase(value));
            if (system != null) {
                return system.getUrl();
            }
        }
        return null;
    }

    /**
     * Reloads the urls from the file.
     */
    public static void reset() {
        urlMaps.clear();
        getUrlMap(Constants.CODING_SYSTEM_MAPPING);
        getUrlMap(Constants.EXTENSION_URL_MAPPING);
    }

    public static void reset(String urlType) {
        urlMaps.remove(urlType);
        getUrlMap(urlType);
    }

    private static Map<String, CodingSystem> getUrlMap(String urlKey) {
        if (urlMaps.get(urlKey) == null) {
            Map<String, CodingSystem> urls = loadFromFile(urlKey);
            urls.putAll(loadAdditionalFromFile());
            urlMaps.put(urlKey, urls);
        }
        return urlMaps.get(urlKey);
    }

    // ConverterConfiguration
    private static Map<String, CodingSystem> loadFromFile(String urlKey) {
        TypeReference<List<CodingSystem>> typeRef = new TypeReference<List<CodingSystem>>() {
        };
        try {
            String content = ResourceReader.getInstance().getResourceInHl7Folder(urlMappingPaths.get(urlKey));
            List<CodingSystem> systems = ObjectMapperUtil.getYAMLInstance().readValue(content, typeRef);
            return systems.stream().collect(Collectors.toMap(CodingSystem::getId, codeSystem -> codeSystem));

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read extension/ExtensionUrlMapping.yml", e);
        }
    }

    private static Map<String, CodingSystem> loadAdditionalFromFile() {
        TypeReference<List<CodingSystem>> typeRef = new TypeReference<List<CodingSystem>>() {
        };
        String filePath = ConverterConfiguration.getInstance().getAdditionalConceptmapFile();
        if (StringUtils.isNotBlank(filePath)) {
            try {
                FileInputStream fis = new FileInputStream(filePath);
                List<CodingSystem> systems = ObjectMapperUtil.getYAMLInstance().readValue(fis, typeRef);
                return systems.stream().collect(Collectors.toMap(CodingSystem::getId, codeSystem -> codeSystem));

            } catch (IOException e) {
                throw new IllegalArgumentException(filePath, e);
            }
        }
        return new HashMap<>();
    }

}
