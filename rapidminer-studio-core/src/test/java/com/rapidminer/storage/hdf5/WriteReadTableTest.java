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
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.MILLISECONDS_PER_SECOND;
import static com.rapidminer.storage.hdf5.TableStatisticsHandler.STATISTICS_MAX;
import static com.rapidminer.storage.hdf5.TableStatisticsHandler.STATISTICS_MIN;
import static com.rapidminer.storage.hdf5.TableStatisticsHandler.STATISTICS_MISSING;
import static com.rapidminer.storage.hdf5.WriteReadTest.createDataSet;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetDatetime;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNom;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNum;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNumFloat;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNumNoFractions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.hdf5.SeekableDataOutputByteChannel;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.NumericColumnDescriptor;
import com.rapidminer.hdf5.file.StringColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.hdf5.message.data.DataType;
import com.rapidminer.hdf5.message.data.DefaultDataType;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Tests the different possibilities to write example sets using the {@link TableWriter} and read them with the {@link
 * Hdf5TableReader}.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class WriteReadTableTest {

	private static final RandomGenerator rng = new RandomGenerator(RandomGenerator.DEFAULT_SEED);

	@RunWith(Parameterized.class)
	public static class ReadWrittenByWriter {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNum(50, 10000, true)});
			sets.add(new Object[]{"datetime", createExampleSetDatetime(50, 1000, true)});
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			sets.add(new Object[]{"many varlength values ending on null", createExampleSetNom(5, 300, 300, false,
					false, true)});
			sets.add(new Object[]{"all types", createAllTypes()});
			sets.add(new Object[]{"all roles", createDifferentRoles()});
			return sets;
		}

		@Test
		public void testWriteAndRead() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new ExampleSetHdf5Writer(set).write(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			set.recalculateAllAttributeStatistics();
			new ExampleSetHdf5Writer(set).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected =
					new TableMetaData(BeltConversionTools.asIOTableOrNull(set, new SequentialConcurrencyContext()),
							false);
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(expected.getColumnMetaData(label), read.getColumnMetaData(label));
			}
		}

		@Test
		public void testWriteMDAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			final ExampleSetMetaData md = new ExampleSetMetaData(set);
			new ExampleSetHdf5Writer(md).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected = ToTableMetaDataConverter.convert(md);
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(expected.getColumnMetaData(label), read.getColumnMetaData(label));
			}
		}

		@Test
		public void testReadingNonExistentMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new ExampleSetHdf5Writer(set).write(f.toPath(), false);
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			assertNull(read);
		}

	}

	@RunWith(Parameterized.class)
	public static class ReadStringRawFormats {

		public class RawStringsWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public RawStringsWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeRaw(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NominalMapping mapping = attribute.getMapping();
					StringColumnDescriptor stringColumnDescriptor = new StringColumnDescriptor(attribute.getName(),
							ColumnDescriptor.Hdf5ColumnType.NOMINAL, null,
							mapping.getValues(), v -> mapping.getIndex(v) >= 0, ColumnDescriptor.StorageType.STRING_RAW,
							i % 2 == 0);
					ExampleSetStatisticsHandler.addStatistics(stringColumnDescriptor, attribute, exampleSet);
					if (attribute.getValueType() != Ontology.NOMINAL) {
						stringColumnDescriptor.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
								(byte) attribute.getValueType());
					}
					if (attribute.getValueType() == Ontology.BINOMINAL) {
						int size = attribute.getMapping().size();
						if (size == 2) {
							byte index = 2;
							boolean previousNan = true;
							int counter = 0;
							while(previousNan){
								final double value = exampleSet.getExample(counter++).getValue(attribute);
								if (!Double.isNaN(value)) {
									previousNan=false;
									if (value ==
											attribute.getMapping().getPositiveIndex()) {
										index = 1;
									}
								}
							}
							stringColumnDescriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class,
									index);
						} else if (size < 2) {
							stringColumnDescriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class,
									(byte) -1);
						}
					}
					columnInfos[i] = stringColumnDescriptor;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount,
						Collections.singletonMap(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, (byte) 1)),
						f);
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			return sets;
		}


		@Test
		public void testReadRawStringColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new RawStringsWriter(set).writeRaw(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			set.recalculateAllAttributeStatistics();
			new RawStringsWriter(set).writeRaw(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected =
					new TableMetaData(BeltConversionTools.asIOTableOrNull(set, new SequentialConcurrencyContext()),
							false);
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(expected.getColumnMetaData(label), read.getColumnMetaData(label));
			}
		}
	}


	@RunWith(Parameterized.class)
	public static class ReadIntCategoricalFormats {

		public class IntCategoriesWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public IntCategoriesWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeInts(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NominalMapping mapping = attribute.getMapping();
					StringColumnDescriptor stringColumnDescriptor = new StringColumnDescriptor(attribute.getName(),
							ColumnDescriptor.Hdf5ColumnType.NOMINAL, null,
							mapping.getValues(), v -> mapping.getIndex(v) >= 0,
							ColumnDescriptor.StorageType.STRING_DICTIONARY,
							false) {

						@Override
						public DataType getDataType() {
							return DefaultDataType.FIXED32;
						}
					};
					stringColumnDescriptor.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					columnInfos[i] = stringColumnDescriptor;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}
		}


		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			return sets;
		}


		@Test
		public void testReadCategoricalColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IntCategoriesWriter(set).writeInts(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadFloatFormats {

		public class FloatWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public FloatWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeFloats(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnDescriptor columnInfo = new NumericColumnDescriptor(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnDescriptor.Hdf5ColumnType.INTEGER :
									ColumnDescriptor.Hdf5ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DataType.createGeneric(DataType.FLOAT_TYPE, new byte[]{0x20, 0x1f, 0x00}, 4,
									new byte[]{0x00, 0x00, 0x20, 0x00, 0x17, 0x08, 0x00, 0x17, 0x7f, 0x00, 0x00,
											0x00});
						}
					};

					columnInfo.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			public void writeDoubleData(ColumnDescriptor columnInfo, SeekableDataOutputByteChannel channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeFloat((float) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumFloat(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new FloatWriter(set).writeFloats(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadLongFormats {

		public class LongWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public LongWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeLongs(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnDescriptor columnInfo = new NumericColumnDescriptor(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnDescriptor.Hdf5ColumnType.INTEGER :
									ColumnDescriptor.Hdf5ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DefaultDataType.FIXED64;
						}
					};

					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			protected void writeLongData(ColumnDescriptor columnInfo, SeekableDataOutputByteChannel channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeLong((long) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumNoFractions(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new LongWriter(set).writeLongs(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadIntegerFormats {

		public class IntegerWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public IntegerWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeInts(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnDescriptor columnInfo = new NumericColumnDescriptor(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnDescriptor.Hdf5ColumnType.INTEGER :
									ColumnDescriptor.Hdf5ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DefaultDataType.FIXED32;
						}
					};

					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			public void writeDoubleData(ColumnDescriptor columnInfo, SeekableDataOutputByteChannel channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeInt((int) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumNoFractions(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IntegerWriter(set).writeInts(f.toPath());
			ExampleSet read =
					TableViewCreator.INSTANCE.convertOnWriteView(Hdf5TableReader.read(f.toPath(),
							Belt.defaultContext()), true);
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadOldTimeFormats {

		public class OldTimeWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public OldTimeWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeOldTime(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnDescriptor dateTime =
							NumericColumnDescriptor.createDateTime(attribute.getName(), null, i % 3 != 0);
					dateTime.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) Ontology.TIME);
					addStatisticsAsDateTime(dateTime, attribute, exampleSet, i % 3 != 0);
					columnInfos[i] = dateTime;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount,
						Collections.singletonMap(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, (byte) 1)),
						f);
			}

			/**
			 * Does the same as {@link ExampleSetStatisticsHandler} but as if the attribute were date-time instead of
			 * time.
			 */
			private void addStatisticsAsDateTime(NumericColumnDescriptor descriptor, Attribute attribute,
												 ExampleSet set, boolean withNanoseconds) {
				descriptor.addAdditionalAttribute(STATISTICS_MISSING, int.class, 0);
				//need the min/max wrt only the time part, otherwise statistics test fails
				Calendar calendar = Tools.getPreferredCalendar();
				double statisticsMin = Double.POSITIVE_INFINITY;
				double statisticsMax = Double.NEGATIVE_INFINITY;
				for (Example example : set) {
					double value = example.getValue(attribute);
					calendar.setTimeInMillis((long) value);
					calendar.set(1970, Calendar.JANUARY, 1);
					double rightValue = calendar.getTimeInMillis();
					if (rightValue < statisticsMin) {
						statisticsMin = rightValue;
					}
					if (rightValue > statisticsMax) {
						statisticsMax = rightValue;
					}
				}
				if (!withNanoseconds) {
					descriptor.addAdditionalAttribute(STATISTICS_MIN, long.class,
							(long) (statisticsMin / MILLISECONDS_PER_SECOND));
					descriptor.addAdditionalAttribute(STATISTICS_MAX, long.class,
							(long) (statisticsMax / MILLISECONDS_PER_SECOND));
				} else {
					descriptor.addAdditionalAttribute(STATISTICS_MIN, long[].class,
							new long[]{(long) (statisticsMin / MILLISECONDS_PER_SECOND),
									(long) (statisticsMin % MILLISECONDS_PER_SECOND * NANOS_PER_MILLISECOND)});
					descriptor.addAdditionalAttribute(STATISTICS_MAX, long[].class,
							new long[]{(long) (statisticsMax / MILLISECONDS_PER_SECOND),
									(long) (statisticsMax % MILLISECONDS_PER_SECOND * NANOS_PER_MILLISECOND)});
				}
			}

			@Override
			protected void writeLongData(ColumnDescriptor columnDescriptor, SeekableDataOutputByteChannel channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnDescriptor.getName());
				for (Example example : exampleSet) {
					double value = example.getValue(att);
					if (Double.isNaN(value)) {
						channel.writeLong(Long.MAX_VALUE);
					} else {
						channel.writeLong(Math.floorDiv((long) value, MILLISECONDS_PER_SECOND));
					}
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"date-time", createAdjustedExampleSetDatetime(15, 147, true)});
			sets.add(new Object[]{"date-time2", createAdjustedExampleSetDatetime(5, 14711, false)});
			return sets;
		}

		private static ExampleSet createAdjustedExampleSetDatetime(int cols, int rows, boolean date) {
			ExampleSet set = createExampleSetDatetime(cols, rows, date);
			//make all attributes time
			for (Attribute attribute : set.getAttributes().createRegularAttributeArray()) {
				Attribute newAtt = AttributeFactory.createAttribute(attribute.getName(), Ontology.TIME);
				newAtt.setTableIndex(attribute.getTableIndex());
				set.getAttributes().replace(attribute, newAtt);
			}
			//get rid of milliseconds analog to what is done while writing
			Attribute[] regularAttributeArray = set.getAttributes().createRegularAttributeArray();
			for (int i = 0; i < regularAttributeArray.length; i++) {
				if (i % 3 == 0) {
					Attribute att = regularAttributeArray[i];
					for (Example example : set) {
						double value = example.getValue(att);
						if (!Double.isNaN(value)) {
							example.setValue(att,
									Math.floorDiv((long) value, MILLISECONDS_PER_SECOND) * MILLISECONDS_PER_SECOND);
						}
					}
				}
			}
			return set;
		}


		@Test
		public void testReadOldTimeColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new OldTimeWriter(set).writeOldTime(f.toPath());

			IOTable expected = BeltConverter.convert(set, new SequentialConcurrencyContext());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(expected, read);
			assertEquals(expected.getAnnotations(), read.getAnnotations());
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			set.recalculateAllAttributeStatistics();
			new OldTimeWriter(set).writeOldTime(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			IOTable converted = BeltConverter.convert(set, new SequentialConcurrencyContext());
			TableMetaData expected = new TableMetaData(converted, false);
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
			}
		}
	}


	private static ExampleSet createAllTypes() {
		List<Attribute> attributes = IntStream.range(1, Ontology.VALUE_TYPE_NAMES.length)
				.mapToObj(i -> {
					Attribute att = AttributeFactory.createAttribute("att-" + i, i);
					if (att.isNominal()) {
						att.getMapping().mapString("Yes");
					}
					return att;
				}).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] row = new double[attributes.size()];
		Arrays.fill(row, 0);
		builder.addRow(row);
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createDifferentRoles() {
		List<Attribute> attributes = IntStream.range(1, 12)
				.mapToObj(i -> AttributeFactory.createAttribute(
						"att-" + i, Ontology.REAL)).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		builder.withRole(attributes.get(0), Attributes.LABEL_NAME);
		builder.withRole(attributes.get(1), Attributes.BATCH_NAME);
		builder.withRole(attributes.get(2), Attributes.CLASSIFICATION_COST);
		builder.withRole(attributes.get(3), Attributes.CLUSTER_NAME);
		builder.withRole(attributes.get(4), "interpretation");
		builder.withRole(attributes.get(5), Attributes.ID_NAME);
		builder.withRole(attributes.get(6), Attributes.OUTLIER_NAME);
		builder.withRole(attributes.get(7), Attributes.PREDICTION_NAME);
		builder.withRole(attributes.get(8), Attributes.WEIGHT_NAME);
		builder.withRole(attributes.get(9), "some Random String");
		builder.withRole(attributes.get(10), "some Random String longer and with utf8 ääµµüÖÖÖ%}          ");
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}
}
