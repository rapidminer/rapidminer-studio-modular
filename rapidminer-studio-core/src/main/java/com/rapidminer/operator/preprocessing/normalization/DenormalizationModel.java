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
package com.rapidminer.operator.preprocessing.normalization;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption;
import com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.normalization.DenormalizationOperator.LinearTransformation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This Model can invert each possible linear transformation given by a normalization model.
 * 
 * @author Sebastian Land
 */
public class DenormalizationModel extends AbstractNormalizationModel {

	private static final long serialVersionUID = 1370670246351357686L;

	private static final Pattern PREDICTION_PATTERN = Pattern.compile(Attributes.PREDICTION_NAME + "\\((.+)\\)");

	private Map<String, LinearTransformation> attributeTransformations;
	private AbstractNormalizationModel invertedModel;
	private boolean failOnMissing;
	private boolean denormPredictions;

	protected DenormalizationModel(ExampleSet exampleSet, Map<String, LinearTransformation> attributeTransformations,
			AbstractNormalizationModel model) {
		this(exampleSet, attributeTransformations, model, false);
	}

	protected DenormalizationModel(ExampleSet exampleSet, Map<String, LinearTransformation> attributeTransformations,
							   AbstractNormalizationModel model, boolean failOnMissingAttributes) {
		this(exampleSet, attributeTransformations, model, failOnMissingAttributes, false);
	}

	protected DenormalizationModel(ExampleSet exampleSet, Map<String, LinearTransformation> attributeTransformations,
								   AbstractNormalizationModel model, boolean failOnMissingAttributes, boolean denormPredictions) {
		super(exampleSet);
		this.attributeTransformations = attributeTransformations;
		this.invertedModel = model;
		this.failOnMissing = failOnMissingAttributes;
		this.denormPredictions = denormPredictions;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet viewParent) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> roleIterator = viewParent.getAttributes().allAttributeRoles();
		while (roleIterator.hasNext()) {
			AttributeRole role = roleIterator.next();
			if (role.isSpecial()) {
				attributes.add(role);
			}
		}
		// add regular attributes
		for (Attribute attribute : viewParent.getAttributes()) {
			if (!attribute.isNumerical() || getTransformationWithPrediction(attribute.getName()) == null) {
				attributes.addRegular(attribute);
			} else {
				// giving new attributes old name: connection to rangesMap
				attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), Ontology.NUMERICAL, null));
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		String targetName = targetAttribute.getName();
		LinearTransformation linearTransformation = getTransformationWithPrediction(targetName);

		if (linearTransformation != null) {
			return (value - linearTransformation.b) / linearTransformation.a;
		}
		return value;
	}

	private LinearTransformation getTransformationWithPrediction(String targetName) {
		LinearTransformation linearTransformation = attributeTransformations.get(targetName);
		if (linearTransformation != null || !denormPredictions) {
			return linearTransformation;
		}
		return Optional.ofNullable(targetName)
				.map(PREDICTION_PATTERN::matcher)
				.filter(Matcher::matches)
				.map(m -> attributeTransformations.get(m.group(1)))
				.orElse(null);
	}

	@Override
	public String toResultString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Denormalization Model of the following Normalization:" + Tools.getLineSeparator());
		builder.append(invertedModel.toResultString());

		return builder.toString();

	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		if (failOnMissing) {
			ExampleSetUtilities.checkAttributesMatching(null, getTrainingHeader().getAttributes(),
					exampleSet.getAttributes(), SetsCompareOption.ALLOW_SUPERSET, TypesCompareOption.ALLOW_SAME_PARENTS);
		}
		return super.applyOnData(exampleSet);
	}

	@Override
	protected ExampleSet getNonSpecialRemappedTarget(ExampleSet exampleSet, HeaderExampleSet trainingHeader, boolean keepAdditional) {
		if (denormPredictions) {
			trainingHeader = (HeaderExampleSet) trainingHeader.clone();
			Attributes targetAttributes = trainingHeader.getAttributes();
			exampleSet.getAttributes().allAttributeRoles()
					.forEachRemaining(role -> {
						String attName = role.getAttribute().getName();
						if (targetAttributes.findRoleByName(attName) == null) {
							Matcher matcher = PREDICTION_PATTERN.matcher(attName);
							if (matcher.matches() && targetAttributes.findRoleByName(matcher.group(1)) != null) {
								targetAttributes.addRegular(role.getAttribute());
							}
						}
					});
		}
		return super.getNonSpecialRemappedTarget(exampleSet, trainingHeader, keepAdditional);
	}

	public Map<String, LinearTransformation> getAttributeTransformations() {
		return attributeTransformations;
	}
	
	public AbstractNormalizationModel getInvertedModel() {
		return invertedModel;
	}
	
	public boolean shouldFailOnMissing() {
		return failOnMissing;
	}
	
}
