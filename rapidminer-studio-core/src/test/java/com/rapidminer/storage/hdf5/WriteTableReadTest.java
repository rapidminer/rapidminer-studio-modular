/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.WriteReadTest.createAllTypes;
import static com.rapidminer.storage.hdf5.WriteReadTest.createDataSet;
import static com.rapidminer.storage.hdf5.WriteReadTest.createDifferentRoles;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetDatetime;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNom;
import static com.rapidminer.storage.hdf5.WriteReadTest.createExampleSetNum;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Columns;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.StringColumnDescriptor;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Tests the different possibilities to write io table using the {@link IOTableHdf5Writer} and read them with the {@link
 * Hdf5TableReader}.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class WriteTableReadTest {

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
			IOTable table = BeltConverter.convert(set, new SequentialConcurrencyContext());
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
			assertEquals(table.getAnnotations(), read.getAnnotations());
			for (String label : table.getTable().labels()) {
				assertEquals(table.getTable().getMetaData(label), read.getTable().getMetaData(label));
			}
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			IOTable table = BeltConverter.convert(set, new SequentialConcurrencyContext());
			new IOTableHdf5Writer(table).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected = new TableMetaData(table, false);
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
			IOTable table = BeltConverter.convert(set, new SequentialConcurrencyContext());
			TableMetaData md = new TableMetaData(table, false);
			new IOTableHdf5Writer(md).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected = md;
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(expected.getColumnMetaData(label), read.getColumnMetaData(label));
			}
		}

	}

	@RunWith(Parameterized.class)
	public static class ReadTableWrittenByWriter {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public IOTable table;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"boolean", createBooleanTable(100)});
			sets.add(new Object[]{"nominal with gaps", createWithGaps()});
			sets.add(new Object[]{"time-datetime", createTableTimeDatetime(50, 1000)});
			sets.add(new Object[]{"all roles", createDifferentRolesAndAnnotations()});
			sets.add(new Object[]{"more column md", createMoreColumnMD()});
			return sets;
		}

		@Test
		public void testWriteAndRead() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
			assertEquals(table.getAnnotations(), read.getAnnotations());
			for (String label : table.getTable().labels()) {
				assertEquals(new HashSet<>(table.getTable().getMetaData(label)), new HashSet<>(read.getTable().getMetaData(label)));
			}
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IOTableHdf5Writer(table).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected = new TableMetaData(table, false);
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(new HashSet<>(expected.getColumnMetaData(label)), new HashSet<>(read.getColumnMetaData(label)));
			}
		}

		@Test
		public void testWriteMDAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			TableMetaData md = new TableMetaData(table, false);
			new IOTableHdf5Writer(md).write(f.toPath());
			TableMetaData read = Hdf5TableReader.readMetaData(f.toPath());
			TableMetaData expected = md;
			assertEquals(expected.height().getNumber(), read.height().getNumber());
			assertEquals(expected.labels(), read.labels());
			for (String label : expected.labels()) {
				assertEquals(expected.column(label), read.column(label));
				assertEquals(new HashSet<>(expected.getColumnMetaData(label)),
						new HashSet<>(read.getColumnMetaData(label)));
			}
		}

	}

	@RunWith(Parameterized.class)
	public static class StringRawFormats {

		public class RawStringsWriter extends IOTableHdf5Writer {

			private final IOTable ioTable;

			public RawStringsWriter(IOTable table) {
				super(table);
				this.ioTable = table;
			}

			public void writeRaw(Path f) throws IOException {
				int allAttributeCount = ioTable.getTable().width();
				int rowCount = ioTable.getTable().height();
				Iterator<String> iterator = ioTable.getTable().labels().iterator();
				ColumnDescriptor[] columnInfos = new ColumnDescriptor[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					String next = iterator.next();
					Column attribute = ioTable.getTable().column(next);
					StringColumnDescriptor stringColumnDescriptor = new StringColumnDescriptor(next,
							ColumnDescriptor.Hdf5ColumnType.NOMINAL, null,
							new ColumnDescriptionCreator.FakeDictionaryCollection(attribute.getDictionary()), v -> false,
							ColumnDescriptor.StorageType.STRING_RAW, i % 2 == 0);
					final LegacyType legacyType = ioTable.getTable().getFirstMetaData(next, LegacyType.class);
					if (legacyType != null) {
						stringColumnDescriptor.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
								(byte) legacyType.ontology());
					}
					columnInfos[i] = stringColumnDescriptor;
				}
				write(columnInfos, ioTable.getAnnotations(), rowCount, null, f);
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
			IOTable table = BeltConverter.convert(set, new SequentialConcurrencyContext());
			new RawStringsWriter(table).writeRaw(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
			assertEquals(table.getAnnotations(), read.getAnnotations());
			for (String label : table.getTable().labels()) {
				assertEquals(table.getTable().getMetaData(label), read.getTable().getMetaData(label));
			}
		}
	}

	public static class CustomSerializationExceptions {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
			Plugin plugin = mock(Plugin.class);
			when(plugin.getExtensionId()).thenReturn("rmx_professional");

			ColumnMetaDataStorageRegistry.register(plugin, "int_column2",
					IntColumnMD2.class, int.class, m -> {throw new RuntimeException("boom");}, i -> new IntColumnMD2((int) i));
			ColumnMetaDataStorageRegistry.register(plugin, "long column_md2",
					LongColumnMD.class, long.class, m -> ((LongColumnMD) m).getNb(), i -> {throw new RuntimeException("boom");});
			ColumnMetaDataStorageRegistry.register(plugin, "short_column_mdä2",
					ShortColumnMD2.class, short.class, m -> ((ShortColumnMD2) m).getNb(), i -> {throw new ColumnMetaDataStorageRegistry.IllegalHdf5FormatException("Dont want this number");});
			ColumnMetaDataStorageRegistry.register(plugin, "double column_md2",
					DoubleColumnMD2.class, byte.class, m -> ((DoubleColumnMD2) m).getNb(),
					i -> new DoubleColumnMD2((double) i));
		}

		@Test(expected = IllegalArgumentException.class)
		public void testNotAllowedClass(){
			Plugin plugin = mock(Plugin.class);
			when(plugin.getExtensionId()).thenReturn("rmx_professional");

			ColumnMetaDataStorageRegistry.register(plugin, "boom",
					ColumnRole.class, Number.class, m -> {throw new RuntimeException("boom");}, i -> new IntColumnMD2((int) i));
		}

		@Test
		public void testSerializationException() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();

			final TableBuilder builder = Builders.newTableBuilder(11);
			builder.addNominal("col", i -> "val");
			builder.addMetaData("col", new IntColumnMD2(42));
			final IOTable table = new IOTable(builder.build(Belt.defaultContext()));
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
		}

		@Test
		public void testSerializationWrongClass() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();

			final TableBuilder builder = Builders.newTableBuilder(11);
			builder.addNominal("col", i -> "val");
			builder.addMetaData("col", new DoubleColumnMD2(42));
			final IOTable table = new IOTable(builder.build(Belt.defaultContext()));
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
		}

		@Test
		public void testSerializationNoSerializer() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();

			final TableBuilder builder = Builders.newTableBuilder(11);
			builder.addNominal("col", i -> "val");
			builder.addMetaData("col", () -> "new dummy type");
			final IOTable table = new IOTable(builder.build(Belt.defaultContext()));
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
		}

		@Test
		public void testDeserializationException() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();

			final TableBuilder builder = Builders.newTableBuilder(10);
			builder.addNominal("col", i -> "val");
			builder.addMetaData("col", new LongColumnMD(42));
			final IOTable table = new IOTable(builder.build(Belt.defaultContext()));
			new IOTableHdf5Writer(table).write(f.toPath());
			IOTable read = Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
			RapidAssert.assertEquals(table, read);
		}

		@Test(expected = HdfReaderException.class)
		public void testDeserializationIllegal() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();

			final TableBuilder builder = Builders.newTableBuilder(10);
			builder.addNominal("col", i -> "val");
			builder.addMetaData("col", new ShortColumnMD2((short)42));
			final IOTable table = new IOTable(builder.build(Belt.defaultContext()));
			new IOTableHdf5Writer(table).write(f.toPath());
			Hdf5TableReader.read(f.toPath(), Belt.defaultContext());
		}

	}

	private static IOTable createBooleanTable(int rows) {
		final Table table =
				Builders.newTableBuilder(rows).addBoolean("yes-no-yes", i -> i % 2 == 0 ? "Yes" : "No", "Yes")
						.addBoolean("yes-no-no", i -> i % 2 == 0 ? "Yes" : "No", "No")
						.addBoolean("yes-null-yes", i -> i % 2 == 0 ? "Yes" : null, "Yes")
						.addBoolean("null-no-no", i -> i % 2 == 0 ? null : "No", "No")
						.addBoolean("null-no-null", i -> i % 2 == 0 ? null : "No", null)
						.addBoolean("null-null-null", i -> null, null).build(Belt.defaultContext());
		return new IOTable(table);
	}

	private static IOTable createTableTimeDatetime(int columns, int rows) {
		SplittableRandom random = new SplittableRandom();
		final TableBuilder builder = Builders.newTableBuilder(rows);
		for (int i = 0; i < columns; i++) {
			if (i % 2 == 0) {
				builder.addTime("att" + i,
						j -> LocalTime.ofNanoOfDay(random.nextLong(24 * 60 * 60 * 1000000000L - 1)));
			} else {
				builder.addDateTime("att" +
						i, j -> Instant.ofEpochSecond(random.nextLong(Instant.MAX.getEpochSecond()),
						random.nextInt(999_999_999)));
			}
		}
		return new IOTable(builder.build(Belt.defaultContext()));
	}

	private static IOTable createDifferentRolesAndAnnotations() {
		SplittableRandom random = new SplittableRandom();
		final TableBuilder builder = Builders.newTableBuilder(42);
		builder.addNominal("withoutRole", i -> "val" + i);
		builder.addMetaData("withoutRole", new ColumnReference("bläß", "äih"));
		for (ColumnRole role : ColumnRole.values()) {
			int max = random.nextInt(Integer.MAX_VALUE);
			builder.addNominal(role.toString(), i -> "val" + random.nextInt(max), max);
			builder.addMetaData(role.toString(), role);
		}
		builder.addMetaData(ColumnRole.values()[0].toString(), new ColumnAnnotation(StringUtils.repeat("I am long äää"
				, 0xffff)));
		builder.addMetaData(ColumnRole.values()[0].toString(), new ColumnReference(StringUtils.repeat("äääcolumn",
				0xffff)));
		builder.addMetaData(ColumnRole.values()[1].toString(), new ColumnReference("label", StringUtils.repeat(
				"ääävalue", 0xffff)));
		builder.addMetaData(ColumnRole.values()[2].toString(), new ColumnReference(null, StringUtils.repeat("ääävalue"
				, 0xffff)));
		return new IOTable(builder.build(Belt.defaultContext()));
	}

	private static IOTable createWithGaps() {
		final NominalBuffer nominalBuffer = Buffers.nominalBuffer(100);
		nominalBuffer.set(0, "red");
		nominalBuffer.set(1, "green");
		for (int i = 1; i <= 50; i++) {
			nominalBuffer.set(i, "blue");
		}
		for (int i = 51; i < 100; i++) {
			nominalBuffer.set(i, "green");
		}
		Table table =
				Builders.newTableBuilder(100).add("nominal", nominalBuffer.toColumn()).add("nominal2",
						nominalBuffer.toColumn()).add("nominal3", nominalBuffer.toColumn()).build(Belt.defaultContext());
		table = table.rows(0, 50, Belt.defaultContext());
		table =
				Builders.newTableBuilder(50).add("nominal",
						Columns.removeUnusedDictionaryValues(table.column("nominal"), Columns.CleanupOption.REMOVE,
								Belt.defaultContext())).add("nominal2",
						table.column("nominal2")).add("nominal3",
						Columns.removeUnusedDictionaryValues(table.column("nominal3"), Columns.CleanupOption.COMPACT,
								Belt.defaultContext())).build(Belt.defaultContext());
		return new IOTable(table);
	}

	private static IOTable createMoreColumnMD() {
		Plugin plugin = mock(Plugin.class);
		when(plugin.getExtensionId()).thenReturn("rmx_professional");

		ColumnMetaDataStorageRegistry.register(plugin, "int_column_mdä",
				IntColumnMD.class, int.class, m -> ((IntColumnMD) m).getNb(), i -> new IntColumnMD((int) i));

		ColumnMetaDataStorageRegistry.register(plugin, "long column_md",
				LongColumnMD2.class, long.class, m -> ((LongColumnMD2) m).getNb(), i -> new LongColumnMD2((long) i));

		ColumnMetaDataStorageRegistry.register(plugin, "short_column_mdä",
				ShortColumnMD.class, short.class, m -> ((ShortColumnMD) m).getNb(), i -> new ShortColumnMD((short) i));

		ColumnMetaDataStorageRegistry.register(plugin, "double column_md",
				DoubleColumnMD.class, double.class, m -> ((DoubleColumnMD) m).getNb(),
				i -> new DoubleColumnMD((double) i));

		final TableBuilder builder = Builders.newTableBuilder(100);
		builder.addNominal("intcolumnmd", i -> "intColumnMd");
		builder.addMetaData("intcolumnmd", new IntColumnMD(42));

		builder.addNominal("longcolumnmd", i -> "longColumnMd");
		builder.addMetaData("longcolumnmd", new LongColumnMD2(420000000));

		builder.addNominal("doublecolumnmd", i -> "doubleColumnMd");
		builder.addMetaData("doublecolumnmd", new DoubleColumnMD(1234.5678));
		builder.addMetaData("doublecolumnmd", new ShortColumnMD((short) 420));
		return new IOTable(builder.build(Belt.defaultContext()));
	}

	private static class IntColumnMD implements ColumnMetaData {

		private int nb;

		private IntColumnMD(int nb) {
			this.nb = nb;
		}

		@Override
		public String type() {
			return "com.rapidminer.new.int.md";
		}

		public int getNb() {
			return nb;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			IntColumnMD that = (IntColumnMD) o;
			return nb == that.nb;
		}

		@Override
		public int hashCode() {
			return Objects.hash(nb);
		}
	}

	private static class LongColumnMD implements ColumnMetaData {

		private long nb;

		private LongColumnMD(long nb) {
			this.nb = nb;
		}

		@Override
		public String type() {
			return "com.rapidminer.new.long.md";
		}

		public long getNb() {
			return nb;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			LongColumnMD that = (LongColumnMD) o;
			return nb == that.nb;
		}

		@Override
		public int hashCode() {
			return Objects.hash(nb);
		}
	}

	private static class ShortColumnMD implements ColumnMetaData {

		private short nb;

		private ShortColumnMD(short nb) {
			this.nb = nb;
		}

		@Override
		public String type() {
			return "com.rapidminer.new.short.md";
		}

		public short getNb() {
			return nb;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ShortColumnMD that = (ShortColumnMD) o;
			return nb == that.nb;
		}

		@Override
		public int hashCode() {
			return Objects.hash(nb);
		}
	}

	private static class DoubleColumnMD implements ColumnMetaData {

		private double nb;

		private DoubleColumnMD(double nb) {
			this.nb = nb;
		}

		@Override
		public String type() {
			return "com.rapidminer.new.int.md";
		}

		public double getNb() {
			return nb;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DoubleColumnMD that = (DoubleColumnMD) o;
			return nb == that.nb;
		}

		@Override
		public int hashCode() {
			return Objects.hash(nb);
		}
	}

	private static class DoubleColumnMD2 extends DoubleColumnMD{

		private DoubleColumnMD2(double nb) {
			super(nb);
		}
	}

	private static class ShortColumnMD2 extends ShortColumnMD{

		private ShortColumnMD2(short nb) {
			super(nb);
		}
	}

	private static class LongColumnMD2 extends LongColumnMD{

		private LongColumnMD2(long nb) {
			super(nb);
		}
	}

	private static class IntColumnMD2 extends IntColumnMD{

		private IntColumnMD2(int nb) {
			super(nb);
		}
	}
}
