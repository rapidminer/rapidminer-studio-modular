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
package com.rapidminer.repository.versioned.datasummary;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import junit.framework.TestCase;


/**
 * Tests for (de)serialization of {@link CollectionMetaData} only
 *
 * @author Jan Czogalla
 * @since 9.8
 */
public class IOCollectionDataSummarySerializerTest extends TestCase {

	private File tmpmd;

	private static final class NullTestComparator<T> implements Comparator<T> {

		@Override
		public int compare(Object a, Object b) {
			if (a == b) {
				return 0;
			}
			if (a == null) {
				return -1;
			}
			if (b == null) {
				return 1;
			}
			return 0;
		}

		@Override
		public Comparator<T> thenComparing(Comparator<? super T> other) {
			Objects.requireNonNull(other);
			return (Comparator<T> & Serializable) (c1, c2) -> {
				int res = compare(c1, c2);
				return (res != 0 || c1 == c2) ? res : other.compare(c1, c2);
			};
		}


	};

	private static<T> Comparator<T> nest(Comparator<T> comp) {
		return new NullTestComparator<T>().thenComparing(comp);
	}

	private static<U extends Comparable<? super U>, T> Comparator<T> wrap(Function<T, U> extractor) {
		return comparing(extractor.andThen(Optional::ofNullable), nest(comparing(Optional::get)));
	}

	private static<U, T> Comparator<T> wrap(Function<T, U> extractor, Comparator<U> innerComp) {
		return comparing(extractor.andThen(Optional::ofNullable), nest(comparing(Optional::get, innerComp)));
	}

	private static final class CollectionComparator<U, T extends Collection<U>> implements Comparator<T> {

		private Comparator<U> elementCompare;

		private CollectionComparator(Comparator<U> elementCompare) {
			if (elementCompare == null) {
				elementCompare = (a, b) -> ((Comparable) a).compareTo(b);
			}
			this.elementCompare = elementCompare;
		}

		@Override
		public int compare(T a, T b) {
			if (a.size() != b.size()) {
				return a.size() - b.size();
			}
			Iterator<U> aIter = a.iterator();
			Iterator<U> bIter = b.iterator();
			while (aIter.hasNext() && bIter.hasNext()) {
				int result = elementCompare.compare(aIter.next(), bIter.next());
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

	private static<U,T extends Collection<U>> Comparator<T> nestCol(Comparator<U> elementCompare) {
		return new CollectionComparator<>(elementCompare);
	}

	private static final Comparator<Range> RANGE_COMPARE = Comparator.comparingDouble(Range::getLower).thenComparingDouble(Range::getUpper);
	private static final Comparator<MDNumber> MD_NUMBER_COMPARE;
	static {
		Comparator<MDNumber> relation = comparing(MDNumber::getRelation);
		MD_NUMBER_COMPARE = relation.thenComparing(wrap(MDNumber::getNumber, comparingDouble(Number::doubleValue)));
	}

	public static final Comparator<AttributeMetaData> ATTRIBUTE_MD_COMPARATOR =
			comparingInt(AttributeMetaData::getValueType)
					.thenComparing(wrap(AttributeMetaData::getName))
					.thenComparing(wrap(AttributeMetaData::getRole))
					.thenComparing(wrap(AttributeMetaData::getMode))
					.thenComparing(wrap(AttributeMetaData::getMean, MD_NUMBER_COMPARE))
					.thenComparing(wrap(AttributeMetaData::getValueSetRelation))
					.thenComparing(wrap(AttributeMetaData::getNumberOfMissingValues))
					.thenComparing(wrap(AttributeMetaData::getValueSet, nestCol(null)))
					.thenComparing(wrap(AttributeMetaData::getValueRange, RANGE_COMPARE));

	public static final Comparator<ExampleSetMetaData> EMD_COMPARATOR =
			wrap(ExampleSetMetaData::getNumberOfExamples, MD_NUMBER_COMPARE)
					.thenComparing(wrap(ExampleSetMetaData::getAttributeSetRelation))
					.thenComparing(wrap(ExampleSetMetaData::getAllAttributes, nestCol(nest(ATTRIBUTE_MD_COMPARATOR))));

	/** Should only be used to check for 0 in assertEquals, not a real comparator. */
	public static final Comparator<TableMetaData> TMD_COMPARATOR =
			wrap(TableMetaData::height, MD_NUMBER_COMPARE)
					.thenComparing(wrap(TableMetaData::getColumnSetRelation))
					.thenComparing(wrap(TableMetaData::labels, nestCol(String::compareTo)))
					.thenComparing(wrap(TableMetaData::getColumns, nestCol(dummyEquals()))
							.thenComparing(wrap(t -> t.labels().stream().map(t::getColumnMetaData).collect(Collectors.toSet()), dummyEquals())));

	private static <T> Comparator<T> dummyEquals() {
		return (o1, o2) -> o1.equals(o2) ? 0 : 1;
	}

	@Override
	protected void setUp() throws Exception {
		tmpmd = File.createTempFile("col-", "tmpmd");
	}

	@Override
	protected void tearDown() throws Exception {
		FileUtils.deleteQuietly(tmpmd);
	}


	public void testSimpleMD() throws IOException {
		TableMetaData emd = new TableMetaDataBuilder(10).addReal("col", null, SetRelation.EQUAL, new MDInteger(5))
				.addColumnMetaData("col", ColumnRole.LABEL).build();
		emd.getAnnotations().setAnnotation("anno", "es");
		CollectionMetaData collection = new CollectionMetaData(emd);
		collection.getAnnotations().setAnnotation("anno", "level 0");

		assertTrue(tmpmd.delete());
		IOCollectionDataSummarySerializer.INSTANCE.serialize(tmpmd.toPath(), collection);
		assertTrue(tmpmd.exists());

		CollectionMetaData colRead =
				(CollectionMetaData) IOCollectionDataSummarySerializer.INSTANCE.deserialize(tmpmd.toPath());
		assertEquals(collection.getAnnotations(), colRead.getAnnotations());
		MetaData readElementMD = colRead.getElementMetaData();
		assertEquals(TableMetaData.class, readElementMD.getClass());
		TableMetaData readEMD = (TableMetaData) readElementMD;
		assertEquals(emd.getAnnotations(), readEMD.getAnnotations());
		assertEquals(0, TMD_COMPARATOR.compare(emd, readEMD));
	}

	public void testSimpleEmptyMD() throws IOException {
		CollectionMetaData collection = new CollectionMetaData();
		MetaData elementMD = collection.getElementMetaData();
		collection.getAnnotations().setAnnotation("anno", "level 0");

		assertTrue(tmpmd.delete());
		IOCollectionDataSummarySerializer.INSTANCE.serialize(tmpmd.toPath(), collection);
		assertTrue(tmpmd.exists());

		CollectionMetaData colRead = (CollectionMetaData) IOCollectionDataSummarySerializer.INSTANCE.deserialize(tmpmd.toPath());
		assertEquals(collection.getAnnotations(), colRead.getAnnotations());
		MetaData readElementMD = colRead.getElementMetaData();
		assertEquals(elementMD.getClass(), readElementMD.getClass());
		assertEquals(elementMD.getAnnotations(), readElementMD.getAnnotations());
	}

	public void testNestedCollectionMD() throws IOException {
		TableMetaData emd =
				new TableMetaDataBuilder(10).addReal("col", null, SetRelation.EQUAL, new MDInteger(5))
						.addColumnMetaData("col", ColumnRole.LABEL).build();
		emd.getAnnotations().setAnnotation("anno", "es");
		CollectionMetaData collection = new CollectionMetaData(emd);
		collection.getAnnotations().setAnnotation("anno", "level 1");
		CollectionMetaData collection2 = new CollectionMetaData(collection);
		collection2.getAnnotations().setAnnotation("anno", "level 0");

		assertTrue(tmpmd.delete());
		IOCollectionDataSummarySerializer.INSTANCE.serialize(tmpmd.toPath(), collection2);
		assertTrue(tmpmd.exists());

		CollectionMetaData colRead =
				(CollectionMetaData) IOCollectionDataSummarySerializer.INSTANCE.deserialize(tmpmd.toPath());
		assertEquals(collection2.getAnnotations(), colRead.getAnnotations());
		MetaData readElementMD = colRead.getElementMetaData();
		assertEquals(CollectionMetaData.class, readElementMD.getClass());
		colRead = (CollectionMetaData) readElementMD;
		assertEquals(collection.getAnnotations(), colRead.getAnnotations());
		readElementMD = colRead.getElementMetaData();
		assertEquals(TableMetaData.class, readElementMD.getClass());
		TableMetaData readEMD = (TableMetaData) readElementMD;
		assertEquals(emd.getAnnotations(), readEMD.getAnnotations());
		assertEquals(0, TMD_COMPARATOR.compare(emd, readEMD));
	}

	public void testNestedCollectionEmptyMD() throws IOException {
		CollectionMetaData collection = new CollectionMetaData();
		MetaData elementMD = collection.getElementMetaData();
		collection.getAnnotations().setAnnotation("anno", "level 1");
		CollectionMetaData collection2 = new CollectionMetaData(collection);
		collection2.getAnnotations().setAnnotation("anno", "level 0");

		assertTrue(tmpmd.delete());
		IOCollectionDataSummarySerializer.INSTANCE.serialize(tmpmd.toPath(), collection2);
		assertTrue(tmpmd.exists());

		CollectionMetaData colRead = (CollectionMetaData) IOCollectionDataSummarySerializer.INSTANCE.deserialize(tmpmd.toPath());
		assertEquals(collection2.getAnnotations(), colRead.getAnnotations());
		MetaData readElementMD = colRead.getElementMetaData();
		assertEquals(CollectionMetaData.class, readElementMD.getClass());
		colRead = (CollectionMetaData) readElementMD;
		assertEquals(collection.getAnnotations(), colRead.getAnnotations());
		readElementMD = colRead.getElementMetaData();
		assertEquals(elementMD.getClass(), readElementMD.getClass());
		assertEquals(elementMD.getAnnotations(), readElementMD.getAnnotations());
	}

	public void testNonserializableElementMD() throws IOException {
		ExampleSetMetaData emd = new ExampleSetMetaData(Collections.singletonList(new AttributeMetaData("col", Ontology.NUMERICAL, "label")));
		emd.getAnnotations().setAnnotation("anno", "es");
		ModelMetaData mmd = new ModelMetaData(ClusterModel.class, emd);
		mmd.getAnnotations().setAnnotation("anno", "model");
		CollectionMetaData collection = new CollectionMetaData(mmd);
		collection.getAnnotations().setAnnotation("anno", "level 0");

		assertTrue(tmpmd.delete());
		IOCollectionDataSummarySerializer.INSTANCE.serialize(tmpmd.toPath(), collection);

		assertFalse(tmpmd.exists());
	}

}