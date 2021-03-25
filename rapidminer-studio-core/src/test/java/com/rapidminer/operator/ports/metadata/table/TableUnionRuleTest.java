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
package com.rapidminer.operator.ports.metadata.table;

import static com.rapidminer.operator.ports.metadata.table.TableMetaDataConverterTest.generateDummyOutputPort;
import static com.rapidminer.operator.ports.metadata.table.TablePassThroughRuleTest.generateDummyInputPort;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the {@link TableUnionRule}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TableUnionRuleTest {

	@Test
	public void testNull() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						null);
		tableUnionRule.transformMD();
	}

	@Test
	public void testPostfix() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						"_from_es2");
		assertEquals("_from_es2", tableUnionRule.getPostfix());
	}

	@Test
	public void testTableMD() {
		InputPort port = mock(InputPort.class);
		when(port.getRawMetaData()).thenReturn(new TableMetaData());
		when(port.getMetaDataAsOrNull(any())).thenReturn(new TableMetaData());
		OutputPort out = mock(OutputPort.class);
		MetaData[] result = new MetaData[1];
		doAnswer(invocation -> {
			final Object argument = invocation.getArgument(0);
			result[0] = (MetaData) argument;
			return null;
		}).when(out).deliverMD(any());
		final TableUnionRule tableUnionRule =
				new TableUnionRule(port, port, out, null);
		tableUnionRule.transformMD();
		assertEquals(TableMetaData.class, result[0].getClass());
	}

	@Test
	public void testOther() {
		InputPort port = mock(InputPort.class);
		when(port.getRawMetaData()).thenReturn(new ModelMetaData(new ExampleSetMetaData()));
		when(port.getMetaDataAsOrNull(any())).thenReturn(null);
		OutputPort out = mock(OutputPort.class);
		MetaData[] result = new MetaData[1];
		doAnswer(invocation -> {
			final Object argument = invocation.getArgument(0);
			result[0] = (MetaData) argument;
			return null;
		}).when(out).deliverMD(any());
		final TableUnionRule tableUnionRule =
				new TableUnionRule(port, port, out, null);
		tableUnionRule.transformMD();
		assertEquals(TableMetaData.class, result[0].getClass());
	}

	@Test
	public void testAllDuplicatesNoPostfix() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						null);
		final TableMetaData tmd = new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build();
		final TableMetaData result = tableUnionRule.modifyMetaData(tmd, tmd);
		assertEquals(tmd.labels(), result.labels());
	}

	@Test
	public void testAllDuplicatesPostfix() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						"_from_2");
		final TableMetaData tmd = new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build();
		final TableMetaData result = tableUnionRule.modifyMetaData(tmd, tmd);
		assertEquals(new HashSet<>(Arrays.asList("real", "int", "nominal", "real_from_2", "int_from_2",
				"nominal_from_2")), result.labels());
		assertEquals(ColumnRole.LABEL, result.getFirstColumnMetaData("nominal_from_2", ColumnRole.class));
	}

	@Test
	public void testMixedPostfix() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						"_from_2");
		final TableMetaData tmd1 = new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("real_from_2", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build();
		final TableMetaData tmd2 = new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("polynominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("polynominal", ColumnRole.LABEL)
				.build();
		final TableMetaData result = tableUnionRule.modifyMetaData(tmd1, tmd2);
		assertEquals(new HashSet<>(Arrays.asList("real", "real_from_2", "nominal", "real_from_2_from_2", "int",
				"polynominal")), result.labels());
	}

	@Test
	public void testAsSuperset() {
		final TableUnionRule tableUnionRule =
				new TableUnionRule(generateDummyInputPort(), generateDummyInputPort(), generateDummyOutputPort(),
						null) {
					@Override
					protected ColumnInfo transformAddedColumnInfo(TableMetaData leftTmd, ColumnInfo rightToAdd) {
						return new ColumnInfoBuilder(rightToAdd.getType().orElse(null))
								.setValueSetRelation(SetRelation.UNKNOWN).setMissings(leftTmd.height()).build();
					}
				};
		final TableMetaData tmd1 = new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build();
		final TableMetaData tmd2 = new TableMetaDataBuilder(20)
				.addReal("real2", null, SetRelation.EQUAL, null)
				.addInteger("int", new Range(1,5), SetRelation.EQUAL, null)
				.addNominal("polynominal", Arrays.asList("yes", "no"), SetRelation.EQUAL, null)
				.addColumnMetaData("polynominal", ColumnRole.LABEL)
				.build();
		final TableMetaData result = tableUnionRule.modifyMetaData(tmd1, tmd2);
		assertEquals(new HashSet<>(Arrays.asList("real", "int", "nominal", "real2", "polynominal",
				"polynominal")), result.labels());
		final ColumnInfo real2 = result.column("real2");
		final ColumnInfo polynominal = result.column("polynominal");
		assertEquals(SetRelation.UNKNOWN, real2.getValueSetRelation());
		assertEquals(SetRelation.UNKNOWN, polynominal.getValueSetRelation());
		assertEquals(10, polynominal.getMissingValues().getNumber(),0);
		assertEquals(10, real2.getMissingValues().getNumber(),0);
	}
}
