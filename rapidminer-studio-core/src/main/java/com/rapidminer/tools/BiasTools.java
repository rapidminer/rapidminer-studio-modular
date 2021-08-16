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

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Simple implementation of bias tools for usage in RapidMiner.
 *
 * This is currently a very simple implementation of a bias check.  All it is currently
 * doing is to check for suspicious substrings.  Future versions may check also in different
 * languages, check the data values (not just the column names), and also for suspicious
 * distributions etc.
 *
 * @author ingomierswa
 * @since 9.10
 */
public final class BiasTools {

    public static final double VALUES_THRESHOLD = 0.4;

    private static final String HTML_OPEN = "<html>";
    private static final String HTML_CLOSE = "</i></html>";
    private static final String HR = "<hr /><i>";
    private static final String BR = "<br />";

    private static final String EXPL_HINT = "bias.hint";
    private static final String EXPL_NAME = "bias.explanation.name";
    private static final String EXPL_VALUES = "bias.explanation.values";

    private BiasTools() {}

    /**
     * A list of suspicious terms which may indicate potential problematic bias connected to your data.
     */
    private static final String[] BIAS_TERMS = {
            "age",
            "birth",
            "sex",
            "sexual",
            "orientation",
            "gender",
            "male",
            "female",
            "diverse",
            "miss",
            "mrs",
            "mr",
            "race",
            "racial",
            "ethnicity",
            "color",
            "minority",
            "national",
            "nationality",
            "origin",
            "accent",
            "parental",
            "marital",
            "married",
            "single",
            "foster",
            "pregnant",
            "pregnancy",
            "profile",
            "religious",
            "religion",
            "belief",
            "believes"
    };

    /**
     * This is currently a very simple implementation of a bias check.  All it is currently
     * doing is to check for suspicious substrings.  Future versions may check also in different
     * languages, check the data values (not just the column names), and also for suspicious
     * distributions etc.
     *
     * @param table the input table
     * @return a  map of column names to bias explanations for suspicious columns (may be empty)
     */
    public static Map<String, BiasExplanation> checkForBias(Table table) {
        Map<String, BiasExplanation> result = new HashMap<>();
        List<String> labels = table.labels();
        for (String label : labels) {
            BiasExplanation explanation = checkSingleColumn(table, label);
            if (explanation != null) {
                result.put(label, explanation);
            }
        }
        return result;
    }

    /**
     * This is currently a very simple implementation of a bias check.  All it is currently
     * doing is to check for suspicious substrings.  Future versions may check also in different
     * languages, check the data values (not just the column names), and also for suspicious
     * distributions etc.
     *
     * @param exampleSet the input data
     * @return a  map of column names to bias explanations for suspicious columns (may be empty)
     *
     * @deprecated  please use the version for Belt tables whenever possible
     */
    @Deprecated
    public static Map<String, BiasExplanation> checkForBias(ExampleSet exampleSet) {
        Map<String, BiasExplanation> result = new HashMap<>();
        List<String> labels = new LinkedList<>();
        for (Attribute attribute : exampleSet.getAttributes()) {
            labels.add(attribute.getName());
        }
        for (String label : labels) {
            BiasExplanation explanation = checkSingleAttribute(exampleSet, label);
            if (explanation != null) {
                result.put(label, explanation);
            }
        }
        return result;
    }

    /**
     * This is currently a very simple implementation of a bias check.  All it is currently
     * doing is to check for suspicious substrings.  Future versions may check also in different
     * languages, check the data values (not just the column names), and also for suspicious
     * distributions etc.
     *
     * @param table the input data
     * @param label the column in question
     * @return a sorted set of suspicious columns which may be empty
     */
    public static BiasExplanation checkForBias(Table table, String label) {
        return checkSingleColumn(table, label);
    }

    /**
     * This is currently a very simple implementation of a bias check.  All it is currently
     * doing is to check for suspicious substrings.  Future versions may check also in different
     * languages, check the data values (not just the column names), and also for suspicious
     * distributions etc.
     *
     * @param exampleSet the input data
     * @param attribute the attribute in question
     * @return a sorted set of suspicious columns which may be empty
     *
     * @deprecated please use the version for Belt tables whenever possible
     */
    @Deprecated
    public static BiasExplanation checkForBias(ExampleSet exampleSet, Attribute attribute) {
        return checkSingleAttribute(exampleSet, attribute.getName());
    }

    /**
     * Returns true if the column with the given name (label) is suspicious for bias.
     *
     * @param exampleSet the example set
     * @param label the column name
     * @return true if this is a suspicous column
     */
    private static BiasExplanation checkSingleAttribute(ExampleSet exampleSet, String label) {
        // first check the name itself
        String offendingTerm = checkNameForBias(label);
        if (offendingTerm != null) {
            BiasExplanation explanation = new BiasExplanation(BiasExplanation.BiasTypes.NAME);
            explanation.setProperty(BiasExplanation.PROPERTY_TERM, offendingTerm);
            return explanation;
        } else {
            // if the column name was not suspicious, also check the possible column values
            // for categorical columns
            Attribute attribute = exampleSet.getAttributes().get(label);
            if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL)) {
                List<String> values = attribute.getMapping().getValues();
                double offendingFraction = checkValuesForBias(values);
                if (offendingFraction > VALUES_THRESHOLD) {
                    BiasExplanation explanation = new BiasExplanation(BiasExplanation.BiasTypes.VALUES);
                    explanation.setProperty(BiasExplanation.PROPERTY_VALUE_FRACTION, offendingFraction);
                    return explanation;
                }
            }
            return null;
        }
    }

    /**
     * Returns true if the column with the given name (label) is suspicious for bias.
     *
     * @param table the table
     * @param label the column name
     * @return true if this is a suspicous column
     */
    private static BiasExplanation checkSingleColumn(Table table, String label) {
        // first check the name itself
        String offendingTerm = checkNameForBias(label);
        if (offendingTerm != null) {
            BiasExplanation explanation = new BiasExplanation(BiasExplanation.BiasTypes.NAME);
            explanation.setProperty(BiasExplanation.PROPERTY_TERM, offendingTerm);
            return explanation;
        } else {
            // if the column name was not suspicious, also check the possible column values
            // for categorical columns
            Column column = table.column(label);
            if (column.type().category().equals(Column.Category.CATEGORICAL)) {
                List<String> values = new LinkedList<>();
                Dictionary dictionary = column.getDictionary();
                for (Dictionary.Entry valueEntry : dictionary) {
                    String value = valueEntry.getValue();
                    values.add(value);
                }
                double offendingFraction = checkValuesForBias(values);
                if (offendingFraction > VALUES_THRESHOLD) {
                    BiasExplanation explanation = new BiasExplanation(BiasExplanation.BiasTypes.VALUES);
                    explanation.setProperty(BiasExplanation.PROPERTY_VALUE_FRACTION, offendingFraction);
                    return explanation;
                }
            }
            return null;
        }
    }

    /**
     * This is the most simple implementation of a bias check.  All it is currently
     * doing is to check for suspicious substrings.  Future versions may check also in different
     * languages or do other naming related checks.
     *
     * @param columnName the name of the column
     * @return the offending term if one was found as a substring and null otherwise
     */
    public static String checkNameForBias(String columnName) {
        for (String term : BIAS_TERMS) {
            // since we currently only support english terms anyway...
            if (columnName.toLowerCase(Locale.ENGLISH).contains(term.toLowerCase(Locale.ENGLISH))) {
                return term;
            }
        }
        return null;
    }

    /**
     * This is the most simple implementation of a column value check.  It is basically performing
     * the same checks as for the column names but this time a column is suspicious if at least a
     * certain threshold of values contains suspicious terms.  This is to avoid that short terms
     * like 'mr' would trigger too many cases if we would check for only one occurence.
     *
     * @param values the possible values for a column
     * @return the fraction of values which contained an offending term (between 0 and 1)
     */
    public static double checkValuesForBias(List<String> values) {
        int counter = 0;
        for (String value : values) {
            for (String term : BIAS_TERMS) {
                // since we currently only support english terms anyway...
                if (value.toLowerCase(Locale.ENGLISH).contains(term.toLowerCase(Locale.ENGLISH))) {
                    counter++;
                    break;
                }
            }
        }
        return (double) counter / (double) values.size();
    }

    /**
     * Generates a warning message for the given column and bias explanation.
     *
     * @param column the column name
     * @param explanation the bias explanation
     * @return the warning message
     */
    public static String getWarningMessage(String column, BiasExplanation explanation) {
        switch (explanation.getBiasType()) {
            case NAME:
                return column + ": " + I18N.getGUILabel(EXPL_HINT) +
                        I18N.getGUILabel(EXPL_NAME) + " " +
                        explanation.getProperty(BiasExplanation.PROPERTY_TERM);
            case VALUES:
                String fractionString = Tools.formatPercent((Double) explanation.getProperty(BiasExplanation.PROPERTY_VALUE_FRACTION));
                return column + ": " + I18N.getGUILabel(EXPL_HINT) +
                        I18N.getGUILabel(EXPL_VALUES) + " " +
                        fractionString;
            default:
                return column + ": " + I18N.getGUILabel(EXPL_HINT);
        }
    }

    /**
     * Generates a short warning message for the given bias explanation.
     *
     * @param explanation the bias explanation
     * @return the warning message
     */
    public static String getShortWarningMessage(BiasExplanation explanation) {
        switch (explanation.getBiasType()) {
            case NAME:
                return I18N.getGUILabel(EXPL_NAME) + " " +
                        explanation.getProperty(BiasExplanation.PROPERTY_TERM);
            case VALUES:
                String fractionString = Tools.formatPercent((Double) explanation.getProperty(BiasExplanation.PROPERTY_VALUE_FRACTION));
                return I18N.getGUILabel(EXPL_VALUES) + " " +
                        fractionString;
            default:
                return I18N.getGUILabel(EXPL_HINT);
        }
    }

    /**
     * Returns a complete HTML tooltip including the column name and a bias explanation.
     *
     * @param column the column
     * @param explanation the explanation
     * @return the complete HTML tooltip
     */
    public static String generateHTMLTooltip(String column, BiasExplanation explanation) {
        switch (explanation.getBiasType()) {
            case NAME:
                return HTML_OPEN + column + BR + HR + I18N.getGUILabel(EXPL_HINT) +
                        BR + I18N.getGUILabel(EXPL_NAME) + " " +
                        explanation.getProperty(BiasExplanation.PROPERTY_TERM) + HTML_CLOSE;
            case VALUES:
                String fractionString = Tools.formatPercent((Double) explanation.getProperty(BiasExplanation.PROPERTY_VALUE_FRACTION));
                return HTML_OPEN + column + BR + HR + I18N.getGUILabel(EXPL_HINT) +
                        BR + I18N.getGUILabel(EXPL_VALUES) + " " +
                        fractionString + HTML_CLOSE;
            default:
                return HTML_OPEN + column + BR + HR + I18N.getGUILabel(EXPL_HINT) + HTML_CLOSE;
        }
    }

    /**
     * Returns only the actual bias explanation part of a tooltip which can be added to other tooltips.
     *
     * @param explanation the explanation
     * @return only the bias part which can be used in other tooltips
     */
    public static String getHTMLExplanation(BiasExplanation explanation) {
        switch (explanation.getBiasType()) {
            case NAME:
                return I18N.getGUILabel(EXPL_HINT) + BR +
                        I18N.getGUILabel(EXPL_NAME) + " " +
                        explanation.getProperty(BiasExplanation.PROPERTY_TERM);
            case VALUES:
                String fractionString = Tools.formatPercent((Double) explanation.getProperty(BiasExplanation.PROPERTY_VALUE_FRACTION));
                return I18N.getGUILabel(EXPL_HINT) + BR +
                        I18N.getGUILabel(EXPL_VALUES) + " " +
                        fractionString;
            default:
                return I18N.getGUILabel(EXPL_HINT);
        }
    }
}