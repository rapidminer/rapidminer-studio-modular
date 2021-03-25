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
package com.rapidminer.operator.util.annotations;

import java.util.Collections;
import java.util.List;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;


/**
 * Adds user defined {@link Annotations} to an {@link IOObject}
 *
 * @author Marius Helf
 */
public class AnnotateOperator extends Operator {

	private InputPort inputPort = getInputPorts().createPort("input", IOObject.class);
	private OutputPort outputPort = getOutputPorts().createPort("output");

	public static final String[] DUPLICATE_HANDLING_LIST = { "overwrite", "ignore", "error" };
	public static final int OVERWRITE_DUPLICATES = 0;
	public static final int IGNORE_DUPLICATES = 1;
	public static final int ERROR_ON_DUPLICATES = 2;

	public static final String PARAMETER_DUPLICATE_HANDLING = "duplicate_annotations";
	public static final String PARAMETER_ANNOTATIONS = "annotations";
	public static final String PARAMETER_NAME = "annotation_name";
	public static final String PARAMETER_VALUE = "annotation_value";

	/**
	 * Creates a new Annotate Operator
	 *
	 * @param description the operator description
	 */
	public AnnotateOperator(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new PassThroughRule(inputPort, outputPort, false) {
			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				try {
					applyChanges(metaData.getAnnotations());
				} catch (UserError e) {
					addError(new ProcessSetupError() {

						@Override
						public String getMessage() {
							return e.getMessage();
						}

						@Override
						public PortOwner getOwner() {
							return getPortOwner();
						}

						@Override
						public List<? extends QuickFix> getQuickFixes() {
							return Collections.emptyList();
						}

						@Override
						public Severity getSeverity() {
							return Severity.ERROR;
						}
					});
				} catch (Exception e) {
					//ignore
				}
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject data = inputPort.getData(IOObject.class);
		applyChanges(data.getAnnotations());
		outputPort.deliver(data);
	}

	/**
	 * Modifies the original Annotations to contain the changes from this operator
	 *
	 * @param annotations the annotations
	 * @throws UserError in case an annotation already exists
	 */
	private void applyChanges(Annotations annotations) throws UserError {

		// get index of action for duplicate annotations
		String duplicateActionString = getParameterAsString(PARAMETER_DUPLICATE_HANDLING);
		int duplicateAction = ERROR_ON_DUPLICATES;
		for (int i = 0; i < DUPLICATE_HANDLING_LIST.length; ++i) {
			if (DUPLICATE_HANDLING_LIST[i].equals(duplicateActionString)) {
				duplicateAction = i;
				break;
			}
		}

		// just set annotations without any checks
		for (String[] nameValuePair : getParameterList(PARAMETER_ANNOTATIONS)) {
			String key = nameValuePair[0];
			String value = nameValuePair[1];
			if (value == null || "".equals(value)) {
				annotations.removeAnnotation(key);
			} else if (annotations.containsKey(key)) {
				switch (duplicateAction) {
					case OVERWRITE_DUPLICATES:
						// overwrite annotations
						annotations.setAnnotation(key, value);
						break;
					case IGNORE_DUPLICATES:
						// do nothing
						break;
					case ERROR_ON_DUPLICATES:
						// throw user error
						throw new UserError(this, "annotate.duplicate_annotation", key);
				}
			} else {
				annotations.setAnnotation(key, value);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeList(
				PARAMETER_ANNOTATIONS,
				"Defines the pairs of annotation names and annotation values. Click the button, select or type an annotation name into the left input field and enter its value into the right field. You can specify an arbitrary amount of annotations here. Please note that it is not possible to create empty annotations.",
				new ParameterTypeStringCategory(PARAMETER_NAME, "The name of the annotation", Annotations.ALL_KEYS_IOOBJECT,
						Annotations.ALL_KEYS_IOOBJECT[0]), new ParameterTypeString(PARAMETER_VALUE,
				"The value of the annotation", true), false);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeStringCategory(PARAMETER_DUPLICATE_HANDLING,
				"Indicates what should happen if duplicate annotation names are specified.", DUPLICATE_HANDLING_LIST,
				DUPLICATE_HANDLING_LIST[OVERWRITE_DUPLICATES], false));

		return types;
	}
}
