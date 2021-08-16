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

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.rapidminer.Process;
import com.rapidminer.TestUtils;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.features.construction.AttributeConstruction;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;


/**
 * Testing lookup for {@link OperatorSignatureRegistry}
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class OperatorSignatureRegistryTest {

	@Test
	public void testLookup() throws Exception {
		TestUtils.INSTANCE.minimalProcessUsageSetup();
		if (OperatorService.getOperatorDescription("generate_attributes") == null) {
			OperatorService.registerOperator(
					new OperatorDescription("com.rapidminer.operator.features.construction.AttributeConstruction",
							"generate_attributes", AttributeConstruction.class,
							OperatorSignatureRegistry.class.getClassLoader(), "elements_selection.png", null), null);
		}

		Process process = new Process();
		process.getRootOperator().getSubprocess(0).addOperator(OperatorService.createOperator(AttributeConstruction.class));
		process.getAllOperators().forEach(op -> {
			op.assumePreconditionsSatisfied();
			op.transformMetaData();
			testLookup(op.getInputPorts(), op.getOperatorDescription().getKey(), -1);
			testLookup(op.getOutputPorts(), op.getOperatorDescription().getKey(), -1);
			if (op instanceof OperatorChain) {
				AtomicInteger index = new AtomicInteger();
				((OperatorChain) op).getSubprocesses().forEach(unit -> {
					int unitIndex = index.getAndIncrement();
					testLookup(unit.getInnerSources(), op.getOperatorDescription().getKey(), unitIndex);
					testLookup(unit.getInnerSinks(), op.getOperatorDescription().getKey(), unitIndex);
				});
			}
		});
	}

	private void testLookup(Ports<?> ports, String opKey, int index) {
		boolean input = ports instanceof InputPorts == (index == -1);
		ports.getAllPorts().forEach(port -> {
			String pName = port.getName();
			String message = String.format("Error for port %s of operator key %s", pName, opKey);
			if (index == -1) {
				assertEquals(message, getExpectedIOOClass(port),
						OperatorSignatureRegistry.INSTANCE.lookup(opKey, pName, input).getClassName());
			} else {
				assertEquals(message, getExpectedIOOClass(port),
						OperatorSignatureRegistry.INSTANCE.lookup(opKey, index, pName, input).getClassName());
			}
		});
	}

	private String getExpectedIOOClass(Port<?,?> port) {
		if (port instanceof InputPort) {
			return Optional.of((InputPort) port)
					.map(InputPort::getAllPreconditions).filter(pc -> !pc.isEmpty())
					.map(pc -> pc.iterator().next())
					.map(Precondition::getExpectedMetaData)
					.map(MetaData::getObjectClass)
					.map(Class::getName)
					.orElse(IOObject.class.getName());
		}
		return Optional.of(port)
				.map(Port::getRawMetaData)
				.map(MetaData::getObjectClass).map(Class::getName)
				.orElse(IOObject.class.getName());
	}
}