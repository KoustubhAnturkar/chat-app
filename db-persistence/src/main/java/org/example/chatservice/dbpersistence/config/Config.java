package org.example.chatservice.dbpersistence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class.getSimpleName());
    private final static String CONFIG_FILE = "dbpersistence.properties";
    private static Properties configProperties;

    private Config(){
        // private constructor to prevent instantiation
    }

    public static void readConfig(){
        if(configProperties == null){
            configProperties = new Properties();
            try {
                configProperties.load(Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE));
            } catch (Exception e) {
                log.error("Error loading configuration file: {}", e.getMessage());
            }
        }
    }

    public static String getProperty(String key){
        if(configProperties == null){
            readConfig();
        }
        return configProperties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue){
        if(configProperties == null){
            readConfig();
        }
        return configProperties.getProperty(key, defaultValue);
    }

    public static Integer[] getIntArrayProperty(String s) throws ConfigException {
        try{
            String property = getProperty(s);
            String[] stringValues = property.split(",");
            Integer[] intValues = new Integer[stringValues.length];
            for (int i = 0; i < stringValues.length; i++) {
                intValues[i] = Integer.parseInt(stringValues[i].trim());
            }
            return intValues;
        } catch (Exception e){
            throw new ConfigException("Error parsing integer array property for key " + s + ": " + e.getMessage());
        }
    }

}
