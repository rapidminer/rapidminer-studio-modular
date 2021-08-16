/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * A bias explanation explains why a column is suspicous for bias.
 * It has a bias type and properties, e.g. which fraction
 * of values has been suspicous or which term in a name has been making a column suspicious.
 *
 * @author ingomierswa
 * @since 9.10
 */
public class BiasExplanation {

    /**
     * The possible bias types for explanations.  Currently only covering names and values.
     */
    public enum BiasTypes {
        NAME,
        VALUES
    }

    public static final String PROPERTY_TERM = "bias_property_term";
    public static final String PROPERTY_VALUE_FRACTION = "bias_property_value_fraction";

    private final BiasTypes biasType;

    private Map<String, Object> properties = new HashMap<>();

    /**
     * Creates a new bias explanation with the given type.
     *
     * @param biasType the bias type
     */
    public BiasExplanation(BiasTypes biasType) {
        this.biasType = biasType;
    }

    /**
     * Creates a new bias explanation with the given type and properties, e.g. which fraction
     * of values has been suspicous or which term in a name has been making a column suspicious.
     *
     * @param biasType the bias type
     * @param properties the properties
     */
    public BiasExplanation(BiasTypes biasType, Map<String, Object> properties) {
        this.biasType = biasType;
        this.properties = properties;
    }

    /**
     * Returns the bias type.
     *
     * @return the bias type
     */
    public BiasTypes getBiasType() {
        return biasType;
    }

    /**
     * Returns all properties.
     *
     * @return all properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Sets all properties.
     *
     * @param properties the new properties
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Sets the given property.
     *
     * @param key the key
     * @param value the value
     */
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    /**
     * Returns the desired property.
     *
     * @param key the key of the property
     *
     * @return the value of the property
     */
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
}
