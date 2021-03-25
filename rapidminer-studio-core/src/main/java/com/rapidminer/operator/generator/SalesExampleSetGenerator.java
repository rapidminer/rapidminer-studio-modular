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
package com.rapidminer.operator.generator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates a random example set for testing purposes. The data represents a sales example set.
 *
 * @author Ingo Mierswa
 */
public class SalesExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	/** After this version dates do not contain time information */
	public static final OperatorVersion BEFORE_STRIP_TIME_FROM_DATE = new OperatorVersion(9, 7, 1);

	private static final int MAX_STORES = 15;

	private static final int MAX_CUSTOMERS = 2000;

	private static final String[] ATTRIBUTE_NAMES = { "transaction_id", "store_id", "customer_id", "product_id",
			"product_category", "date", "amount", "single_price" };

	private static final String[] STORES = {"Store 01", "Store 02", "Store 03", "Store 04", "Store 05",
			"Store 06", "Store 07", "Store 08", "Store 09", "Store 10", "Store 11", "Store 12", "Store 13", "Store 14",
			"Store 15"};

	private static final int ATT_TRANSACTION_ID = 0;
	private static final int ATT_STORE_ID = 1;
	private static final int ATT_CUSTOMER_ID = 2;
	private static final int ATT_PRODUCT_ID = 3;
	private static final int ATT_PRODUCT_CATEGORY = 4;
	private static final int ATT_DATE = 5;
	private static final int ATT_AMOUNT = 6;
	private static final int ATT_SINGLE_PRICE = 7;

	private static final long DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

	private static final String[] PRODUCT_CATEGORIES = new String[] { "Books", "Movies", "Electronics", "Home/Garden",
			"Health", "Toys", "Sports", "Clothing" };

	/** @since 9.2.0 */
	private static final ExampleSetMetaData DEFAULT_META_DATA;

	private static final int PRODUCT_ID_LOWER = 10_000;
	private static final int PRODUCT_ID_UPPER = 100_000;

	private static final int UPDATE_INTERVAL = 500;
	static {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.addAttribute(new AttributeMetaData("transaction_id", Ontology.INTEGER, Attributes.ID_NAME));
		emd.addAttribute(new AttributeMetaData("store_id", null, STORES));
		String[] customers = new String[MAX_CUSTOMERS];
		for (int i = 0; i < MAX_CUSTOMERS; i++) {
			customers[i] = "Customer " + (i + 1);
		}

		emd.addAttribute(new AttributeMetaData("customer_id", null, customers));
		emd.addAttribute(new AttributeMetaData("product_id", null, Ontology.INTEGER, new Range(PRODUCT_ID_LOWER, PRODUCT_ID_UPPER)));
		emd.addAttribute(new AttributeMetaData("product_category", null, PRODUCT_CATEGORIES));

		emd.addAttribute(new AttributeMetaData("date", Ontology.DATE));
		emd.addAttribute(new AttributeMetaData("amount", null, Ontology.INTEGER, new Range(1, 10)));
		emd.addAttribute(new AttributeMetaData("single_price", null, Ontology.REAL, new Range(10, 100)));
		DEFAULT_META_DATA = emd;
	}

	public SalesExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);

		List<Attribute> attributes = new ArrayList<>();

		// transaction id
		Attribute transactionId = AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_TRANSACTION_ID], Ontology.INTEGER);
		attributes.add(transactionId);

		// store id
		Attribute storeId = AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_STORE_ID], Ontology.NOMINAL);
		for (String store : STORES) {
			storeId.getMapping().mapString(store);
		}
		attributes.add(storeId);

		// customer id
		Attribute customerId = AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_CUSTOMER_ID], Ontology.NOMINAL);
		for (int s = 1; s <= MAX_CUSTOMERS; s++) {
			customerId.getMapping().mapString("Customer " + s);
		}
		attributes.add(customerId);

		// product id
		attributes.add(AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_PRODUCT_ID], Ontology.INTEGER));

		// product category
		Attribute productCategory = AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_PRODUCT_CATEGORY],
				Ontology.NOMINAL);
		for (String category : PRODUCT_CATEGORIES) {
			productCategory.getMapping().mapString(category);
		}
		attributes.add(productCategory);

		// date
		attributes.add(AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_DATE], Ontology.DATE));

		// amount
		attributes.add(AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_AMOUNT], Ontology.INTEGER));

		// single price
		attributes.add(AttributeFactory.createAttribute(ATTRIBUTE_NAMES[ATT_SINGLE_PRICE], Ontology.REAL));

		// create builder
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(numberOfExamples);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// init operator progress
		getProgress().setTotal(numberOfExamples / UPDATE_INTERVAL);

		long firstDate = new GregorianCalendar(2005, Calendar.FEBRUARY, 1).getTime().getTime();
		long secondDate = new GregorianCalendar(2008, Calendar.NOVEMBER, 30).getTime().getTime();
		boolean stripDate = getCompatibilityLevel().isAbove(BEFORE_STRIP_TIME_FROM_DATE);
		for (int n = 1; n <= numberOfExamples; n++) {
			double[] values = new double[ATTRIBUTE_NAMES.length];
			// "transaction_id", "store_id", "customer_id", "product_id", "product_category",
			// "date", "amount", "single_price"
			values[ATT_TRANSACTION_ID] = n;
			values[ATT_STORE_ID] = random.nextIntInRange(0, MAX_STORES);
			values[ATT_CUSTOMER_ID] = random.nextIntInRange(0, MAX_CUSTOMERS);
			values[ATT_PRODUCT_ID] = random.nextIntInRange(PRODUCT_ID_LOWER, PRODUCT_ID_UPPER);
			values[ATT_PRODUCT_CATEGORY] = random.nextInt(PRODUCT_CATEGORIES.length);
			long randomDate = random.nextLongInRange(firstDate, secondDate);
			values[ATT_DATE] = stripDate ? removeTimePart(randomDate) : randomDate;
			values[ATT_AMOUNT] = random.nextIntInRange(1, 10);
			values[ATT_SINGLE_PRICE] = random.nextDoubleInRange(10, 100);

			builder.addRow(values);

			if (n % UPDATE_INTERVAL == 0) {
				getProgress().step();
			}
		}

		getProgress().complete();

		// create example set and return it
		return builder.withRole(transactionId, Attributes.ID_NAME).build();
	}

	/**
	 * Removes the time part of a timestamp in UTC
	 *
	 * @param timestamp a timestamp
	 * @return the timestamp at 00:00:00.000 UTC
	 * @since 9.8.0
	 */
	private static long removeTimePart(long timestamp) {
		// Unix timestamps don't care about leap seconds, a day is always 86400 "seconds" long
		return (timestamp / DAY_IN_MILLIS) * DAY_IN_MILLIS;
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = getDefaultMetaData();
		emd.setNumberOfExamples(getParameterAsInt(PARAMETER_NUMBER_EXAMPLES));
		return emd;
	}

	/** @since 9.2.0 */
	@Override
	protected ExampleSetMetaData getDefaultMetaData() {
		return DEFAULT_META_DATA.clone();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_EXAMPLES, "The number of generated examples.", 1,
				Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(), BEFORE_STRIP_TIME_FROM_DATE);
	}
}
