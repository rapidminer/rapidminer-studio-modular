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
package com.rapidminer.gui.processeditor.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.search.AbstractGlobalSearchManager;
import com.rapidminer.search.GlobalSearchDefaultField;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.OperatorSignatureRegistry;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.signature.IOType;

/**
 * Manages operator search for the Global Search feature. See {@link OperatorGlobalSearch}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class OperatorGlobalSearchManager extends AbstractGlobalSearchManager implements OperatorService.OperatorServiceListener {

	private static final Map<String, String> ADDITIONAL_FIELDS;

	private static final String FIELD_TAG = "tag";
	private static final String FIELD_PARAMETER = "parameter";
	private static final String FIELD_INPUT_CLASS = "input";
	private static final String FIELD_OUTPUT_CLASS = "output";
	private static final String FIELD_CAPABILITIES = "capability";
	private static final String FIELD_SOURCE = "source";

	private static final String DUMMY_OPERATOR = "dummy";
	private static final String PROCESS_ROOT_OPERATOR = "process";
	private static final String IO_OBJECT = "IOObject";

	private static final float FIELD_BOOST_TAG = 0.5f;
	private static final float FIELD_BOOST_SOURCE = 0.25f;

	static {
		ADDITIONAL_FIELDS = new HashMap<>();
		ADDITIONAL_FIELDS.put(FIELD_TAG, "The tags of the operator");
		ADDITIONAL_FIELDS.put(FIELD_PARAMETER, "The parameter names of the operator");
		ADDITIONAL_FIELDS.put(FIELD_INPUT_CLASS, "The input port types of the operator");
		ADDITIONAL_FIELDS.put(FIELD_OUTPUT_CLASS, "The output port types of the operator");
		ADDITIONAL_FIELDS.put(FIELD_CAPABILITIES, "The capabilities of the operator (typically only specified for learners)");
		ADDITIONAL_FIELDS.put(FIELD_SOURCE, "The source of the operator, e.g. RapidMiner Studio Core or an extension");
	}


	protected OperatorGlobalSearchManager() {
		super(OperatorGlobalSearch.CATEGORY_ID, ADDITIONAL_FIELDS, new GlobalSearchDefaultField(FIELD_TAG, FIELD_BOOST_TAG), new GlobalSearchDefaultField(FIELD_SOURCE, FIELD_BOOST_SOURCE));
	}

	@Override
	public void operatorRegistered(final OperatorDescription description, final OperatorDocBundle bundle) {
		if (description.isDeprecated()) {
			return;
		}
		if (DUMMY_OPERATOR.equals(description.getKey()) || PROCESS_ROOT_OPERATOR.equals(description.getKey())) {
			return;
		}
		addDocumentToIndex(createDocument(description));
	}

	@Override
	public void operatorUnregistered(final OperatorDescription description) {
		removeDocumentFromIndex(createDocument(description));
	}

	@Override
	protected void init() {
		OperatorService.addOperatorServiceListener(this);
	}

	@Override
	protected List<Document> createInitialIndex() {
		// not needed
		return Collections.emptyList();
	}

	/**
	 * Creates an operator search document for the given operator description.
	 *
	 * @param opDesc
	 * 		the operator description for which to create the search document
	 * @return the document, never {@code null}
	 */
	private Document createDocument(final OperatorDescription opDesc) {
		List<Field> fields = new ArrayList<>();

		// add tags
		List<String> tags = opDesc.getTags();
		StringBuilder sb = new StringBuilder();
		if (!tags.isEmpty()) {
			for (String tag : tags) {
				sb.append(tag);
				sb.append(' ');
			}
		}
		sb.append(opDesc.getGroupName());
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_TAG, sb.toString()));
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_SOURCE, opDesc.getProviderName()));

		// add parameters and input/output port classes
		// as of 8.1, this operator creation does not cost any time, even though it looks scary
		// for over a dozen extensions, the whole try-block below with operator creation adds less than 1 second to Studio start

		// store parameter keys
		createParameterField(fields, opDesc);

		// store input port types
		createInputPortField(fields, opDesc);

		// store output port types
		createOutputPortField(fields, opDesc);

		// for learners and new operators, also store their capabilities
		if (CapabilityProvider.class.isAssignableFrom(opDesc.getOperatorClass())) {
			createCapabilitiesField(fields, opDesc);
		}
		if (TableCapabilityProvider.class.isAssignableFrom(opDesc.getOperatorClass())) {
			createCapabilitiesField(fields, opDesc);
		}
		return GlobalSearchUtilities.INSTANCE.createDocument(opDesc.getKey(), opDesc.getName(), fields.toArray(new Field[0]));
	}

	/**
	 * Creates the parameter field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param opDesc
	 * 		the operator description
	 */
	private void createParameterField(final List<Field> fields, final OperatorDescription opDesc) {
		String paramString = OperatorSignatureRegistry.INSTANCE
				.lookupParameters(opDesc.getKey())
				.keySet().stream()
				.map(key -> key.replace("_", ""))
				.collect(Collectors.joining(" "));
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_PARAMETER, paramString));
	}

	/**
	 * Creates the input port type field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param opDesc
	 * 		the operator description
	 */

	private void createInputPortField(final List<Field> fields, final OperatorDescription opDesc) {
		createPortField(fields, opDesc, true);
	}

	/**
	 * Creates the output port type field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param opDesc
	 * 		the operator description
	 */
	private void createOutputPortField(final List<Field> fields, final OperatorDescription opDesc) {
		createPortField(fields, opDesc, false);
	}

	/**
	 * Creates the port type field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param opDesc
	 * 		the operator description
	 * @param input
	 * 		whether to create the input or output field
	 */
	private void createPortField(final List<Field> fields, final OperatorDescription opDesc, boolean input) {
		Map<String, IOType> portSignature = OperatorSignatureRegistry.INSTANCE.lookup(opDesc.getKey(), input);
		if (portSignature.isEmpty()) {
			return;
		}
		StringJoiner joiner = new StringJoiner(" ", IO_OBJECT + " ", "");
		portSignature.forEach((name, type) -> {
			String portValue = name;
			if (type.isSpecific()) {
				try {
					portValue = RendererService.getName(Plugin.getMajorClassLoader().loadClass(type.getClassName()));
				} catch (ClassNotFoundException e) {
					// ignore and use port name
				}
			}
			joiner.add(portValue);
		});
		fields.add(GlobalSearchUtilities.INSTANCE
				.createFieldForTexts(input ? FIELD_INPUT_CLASS : FIELD_OUTPUT_CLASS, joiner.toString()));
	}

	/**
	 * Create the capabilities field for {@link CapabilityProvider}s.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param opDesc
	 * 		the learner description
	 */
	private void createCapabilitiesField(final List<Field> fields, final OperatorDescription opDesc) {
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_CAPABILITIES,
				OperatorSignatureRegistry.INSTANCE.lookupCapabilities(opDesc.getKey())
						.stream().collect(Collectors.joining(" "))));
	}
}
