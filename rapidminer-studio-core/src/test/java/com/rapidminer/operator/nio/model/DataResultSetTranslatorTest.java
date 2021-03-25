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
package com.rapidminer.operator.nio.model;

import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.ANNOTATION_NAME;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * Tests for the {@link DataResultSetTranslator} to test that it does the same for {@link
 * com.rapidminer.example.ExampleSet}s as for {@link com.rapidminer.adaption.belt.IOTable}s.
 *
 * @author Gisa Meier
 * @since 9.9
 */
public class DataResultSetTranslatorTest {

	@BeforeClass
	public static void before() {
		RapidMiner.initAsserters();
	}

	@Test
	public void testStringOnlyResultSet() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeDataResultSet(exampleSet);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration((AbstractDataResultSetReader) null);
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), a.getName(), a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		configuration.getAnnotationsMap().clear();
		test(dataResultSet, configuration);
	}

	@Test
	public void testStringOnlyResultSetNoUserDefined() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeDataResultSet(exampleSet);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration((AbstractDataResultSetReader) null);
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), null, a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		configuration.getAnnotationsMap().clear();
		test(dataResultSet, configuration);
	}

	@Test
	public void testStringOnlyResultSetRenaming() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeDataResultSet(exampleSet);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration((AbstractDataResultSetReader) null);
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), "a", a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		configuration.getAnnotationsMap().clear();
		test(dataResultSet, configuration);
	}

	@Test
	public void testStringOnlyResultSetBinomAndAttValue() throws OperatorException {
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		Attribute polynominal = AttributeFactory.createAttribute("polynominal", Ontology.POLYNOMINAL);
		for (int i = 0; i < 5; i++) {
			nominal.getMapping().mapString("nominalValue" + i);
		}
		for (int i = 0; i < 6; i++) {
			polynominal.getMapping().mapString("polyValue" + i);
		}
		Random random = new Random();
		List<Attribute> attributes =
				Arrays.asList(nominal, polynominal);
		ExampleSet exampleSet = ExampleSets.from(attributes).withBlankSize(150)
				.withColumnFiller(nominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(5))
				.withColumnFiller(polynominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(6))
				.build();

		final DataResultSet dataResultSet = getFakeDataResultSet(exampleSet);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration((AbstractDataResultSetReader) null);
		final ColumnMetaData[] columnMetaData = new ColumnMetaData[]{
				new ColumnMetaData("bla", "blup", Ontology.BINOMINAL, "super", true, true),
				new ColumnMetaData(null, "foo", Ontology.VALUE_TYPE, "label", true, false)
		};
		configuration.setColumnMetaData(columnMetaData);
		configuration.getAnnotationsMap().clear();
		test(dataResultSet, configuration);
	}

	@Test
	public void testDifferentTypesResultSet() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeValueDataResultSet(exampleSet, false);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration(null, Arrays.asList(ANNOTATION_NAME, "Comment"));
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), a.getName(), a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		test(dataResultSet, configuration);
	}

	@Test
	public void testDifferentTypesResultSetConstantName() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeValueDataResultSet(exampleSet, true);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration(null, Arrays.asList(ANNOTATION_NAME, "Comment"));
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), a.getName(), a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		test(dataResultSet, configuration);
	}

	@Test
	public void testDifferentTypesResultSetNoUserDef() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeValueDataResultSet(exampleSet, false);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration(null, Arrays.asList(ANNOTATION_NAME, "Comment"));
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), null, a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		test(dataResultSet, configuration);
	}

	@Test
	public void testDifferentTypesResultSetSameName() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeValueDataResultSet(exampleSet, false);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration(null, Arrays.asList(ANNOTATION_NAME, "Comment"));
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"a"+a.getTableIndex(), "a"+a.getTableIndex(), a.getValueType(), Attributes.ATTRIBUTE_NAME, true,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		test(dataResultSet, configuration);
	}

	@Test
	public void testDifferentTypesResultSetNonSelected() throws OperatorException {
		ExampleSet exampleSet = getAllTypesES();
		final DataResultSet dataResultSet = getFakeValueDataResultSet(exampleSet, false);
		final DataResultSetTranslationConfiguration configuration =
				new DataResultSetTranslationConfiguration(null, Arrays.asList(ANNOTATION_NAME, "Comment"));
		final ColumnMetaData[] columnMetaData =
				Arrays.stream(exampleSet.getAttributes().createRegularAttributeArray()).map(a -> new ColumnMetaData(
						"att" +
								a.getTableIndex(), a.getName(), a.getValueType(), Attributes.ATTRIBUTE_NAME,
						a.getTableIndex() % 2 == 0,
						true)).toArray(ColumnMetaData[]::new);
		configuration.setColumnMetaData(columnMetaData);
		test(dataResultSet, configuration);
	}


	private void test(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration) throws OperatorException {
		final ExampleSet exampleSet = new DataResultSetTranslator(null).read(dataResultSet, configuration, false,
				null);
		dataResultSet.reset(null);
		final IOTable ioTable = new DataResultSetTranslator(null).readTable(dataResultSet, configuration, false, null);
		RapidAssert.assertEquals(exampleSet, TableViewCreator.INSTANCE.convertOnWriteView(ioTable, false));
	}

	private ExampleSet getAllTypesES() {
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		Attribute string = AttributeFactory.createAttribute("string", Ontology.STRING);
		Attribute polynominal = AttributeFactory.createAttribute("polynominal", Ontology.POLYNOMINAL);
		Attribute binominal = AttributeFactory.createAttribute("binominal", Ontology.BINOMINAL);
		Attribute path = AttributeFactory.createAttribute("path", Ontology.FILE_PATH);
		for (int i = 0; i < 5; i++) {
			nominal.getMapping().mapString("nominalValue" + i);
		}
		for (int i = 0; i < 4; i++) {
			string.getMapping().mapString("veryVeryLongStringValue" + i);
		}
		for (int i = 0; i < 6; i++) {
			polynominal.getMapping().mapString("polyValue" + i);
		}
		for (int i = 0; i < 2; i++) {
			binominal.getMapping().mapString("binominalValue" + i);
		}
		for (int i = 0; i < 3; i++) {
			path.getMapping().mapString("//folder/sufolder/subsubfolder/file" + i);
		}
		Attribute numeric = AttributeFactory.createAttribute("numeric", Ontology.NUMERICAL);
		Attribute real = AttributeFactory.createAttribute("real", Ontology.REAL);
		Attribute integer = AttributeFactory.createAttribute("integer", Ontology.INTEGER);
		Attribute dateTime = AttributeFactory.createAttribute("date_time", Ontology.DATE_TIME);
		Attribute date = AttributeFactory.createAttribute("date", Ontology.DATE);
		Attribute time = AttributeFactory.createAttribute("time", Ontology.TIME);
		Random random = new Random();
		List<Attribute> attributes =
				Arrays.asList(nominal, string, polynominal, binominal, path, numeric, real, dateTime, date, time,
						integer);
		return ExampleSets.from(attributes).withBlankSize(150)
				.withColumnFiller(numeric, i -> Math.random() > 0.7 ? Double.NaN : Math.random())
				.withColumnFiller(real, i -> Math.random() > 0.7 ? Double.NaN : 42 + Math.random())
				.withColumnFiller(integer, i -> Math.random() > 0.7 ? Double.NaN : Math.round(Math.random() * 100))
				.withColumnFiller(dateTime,
						i -> Math.random() > 0.7 ? Double.NaN : 1515410698d + Math.floor(Math.random() * 1000))
				.withColumnFiller(date, i -> Math.random() > 0.7 ? Double.NaN :
						230169600000d + Math.floor(Math.random() * 100) * 1000d * 60 * 60 * 24)
				.withColumnFiller(time,
						i -> Math.random() > 0.7 ? Double.NaN : randomTimeMillis(random))
				.withColumnFiller(nominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(5))
				.withColumnFiller(string, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(4))
				.withColumnFiller(polynominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(6))
				.withColumnFiller(binominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(2))
				.withColumnFiller(path, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(3))
				.build();
	}

	private static long randomTimeMillis(Random random){
		Calendar cal = Tools.getPreferredCalendar();
		cal.setTimeInMillis((long) Math.floor(random.nextDouble() * 60 * 60 * 24 * 1000));
		cal.set(1970, Calendar.JANUARY, 1);
		return cal.getTimeInMillis();
	}

	private DataResultSet getFakeDataResultSet(ExampleSet exampleSet) {
		final Attribute[] attributeArray = exampleSet.getAttributes().createRegularAttributeArray();
		return new DataResultSet() {

			private int current = -1;

			@Override
			public boolean hasNext() {
				return current < exampleSet.size() - 1;
			}

			@Override
			public void next(ProgressListener listener) throws OperatorException {
				current++;
			}

			@Override
			public int getNumberOfColumns() {
				return attributeArray.length;
			}

			@Override
			public String[] getColumnNames() {
				return Arrays.stream(attributeArray).map(Attribute::getName).toArray(String[]::new);
			}

			@Override
			public boolean isMissing(int columnIndex) {
				return Double.isNaN(exampleSet.getExample(current).getValue(attributeArray[columnIndex]));
			}

			@Override
			public Number getNumber(int columnIndex) throws ParseException {
				throw new ParseException(
						new ParsingError(-1, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, null));
			}

			@Override
			public String getString(int columnIndex) throws ParseException {
				return exampleSet.getExample(current).getValueAsString(attributeArray[columnIndex]);
			}

			@Override
			public Date getDate(int columnIndex) throws ParseException {
				throw new ParseException(
						new ParsingError(-1, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, null));
			}

			@Override
			public ValueType getNativeValueType(int columnIndex) throws ParseException {
				return ValueType.STRING;
			}

			@Override
			public void close() throws OperatorException {

			}

			@Override
			public void reset(ProgressListener listener) throws OperatorException {
				current = -1;
			}

			@Override
			public int[] getValueTypes() {
				return Arrays.stream(attributeArray).mapToInt(Attribute::getValueType).toArray();
			}

			@Override
			public int getCurrentRow() {
				return current;
			}
		};
	}

	private DataResultSet getFakeValueDataResultSet(ExampleSet exampleSet, boolean constantFirstValue) {
		final Attribute[] attributeArray = exampleSet.getAttributes().createRegularAttributeArray();
		return new DataResultSet() {

			private int current = -2;

			@Override
			public boolean hasNext() {
				return current < exampleSet.size() - 1;
			}

			@Override
			public void next(ProgressListener listener) throws OperatorException {
				current++;
			}

			@Override
			public int getNumberOfColumns() {
				return attributeArray.length;
			}

			@Override
			public String[] getColumnNames() {
				final String[] strings = new String[attributeArray.length];
				Arrays.setAll(strings, i -> "a" + i);
				return strings;
			}

			@Override
			public boolean isMissing(int columnIndex) {
				return Double.isNaN(exampleSet.getExample(current).getValue(attributeArray[columnIndex]));
			}

			@Override
			public Number getNumber(int columnIndex) throws ParseException {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeArray[columnIndex].getValueType(),
						Ontology.NUMERICAL)) {
					return exampleSet.getExample(current).getValue(attributeArray[columnIndex]);
				} else {
					throw new ParseException(
							new ParsingError(-1, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, null));
				}
			}

			@Override
			public String getString(int columnIndex) throws ParseException {
				if (current == -1) {
					if(constantFirstValue){
						return "a";
					}
					return attributeArray[columnIndex].getName();
				}
				return exampleSet.getExample(current).getValueAsString(attributeArray[columnIndex]);
			}

			@Override
			public Date getDate(int columnIndex) throws ParseException {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeArray[columnIndex].getValueType(),
						Ontology.DATE_TIME)) {
					return new Date((long) exampleSet.getExample(current).getValue(attributeArray[columnIndex]));
				} else {
					throw new ParseException(
							new ParsingError(-1, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, null));
				}
			}

			@Override
			public ValueType getNativeValueType(int columnIndex) throws ParseException {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeArray[columnIndex].getValueType(),
						Ontology.NUMERICAL)) {
					return ValueType.NUMBER;
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeArray[columnIndex].getValueType(),
						Ontology.DATE_TIME)) {
					return ValueType.DATE;
				}
				return ValueType.STRING;
			}

			@Override
			public void close() throws OperatorException {

			}

			@Override
			public void reset(ProgressListener listener) throws OperatorException {
				current = -2;
			}

			@Override
			public int[] getValueTypes() {
				return Arrays.stream(attributeArray).mapToInt(Attribute::getValueType).toArray();
			}

			@Override
			public int getCurrentRow() {
				return current;
			}
		};
	}


}