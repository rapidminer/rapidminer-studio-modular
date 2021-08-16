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
package com.rapidminer.tools.signature;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.gui.tools.VersionNumber;


/**
 * Read/write test for {@link IOSignatureRegistry}
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class IOSignatureRegistryTest {

	private static Map<String, IOHolderProviderInfo> signatureMap;
	private static VersionNumber version;
	private IOSignatureRegistry registry;

	@BeforeClass
	public static void setup() {
		signatureMap = new TreeMap<>();
		version = new VersionNumber(1, 1, 1);
		Class[] rndClasses = {Object.class, String.class, Integer.class, Double.class};
		Random rnd = new Random();
		Supplier<String> rndClassName = () -> rndClasses[rnd.nextInt(rndClasses.length)].getName();
		Supplier<IOType> rndIOType = () -> new IOType(rndClassName.get(), rnd.nextBoolean(), rnd.nextBoolean());
		char[] chars = new char[26];
		for (char c = 'a'; c <= 'z'; c++) {
			chars[c - 'a'] = c;
		}

		for (int i = 0; i < 5; i++) {
			LinkedHashMap<String, IOSignature> signatures = new LinkedHashMap<>();
			for (int j = 0; j < 6; j++) {
				IOSignature signature = IOSignature.builder()
						.ioHolderKey(RandomStringUtils.random(10, chars))
						.ioHolderClass(rndClassName.get())
						.capabilities(IntStream.range(0, 1 + rnd.nextInt(rndClasses.length))
								.mapToObj(index -> rndClasses[index].getName()).collect(Collectors.toList()))
						.build();

				// top level I/O
				for (int k = 0; k < 3; k++) {
					signature.getInputs().put(RandomStringUtils.random(6, chars), rndIOType.get());
				}
				for (int k = 0; k < 4; k++) {
					signature.getOutputs().put(RandomStringUtils.random(6, chars), rndIOType.get());
				}

				// sub nodes + I/O
				if (rnd.nextBoolean()) {
					SubNodeSignature subSignature = SubNodeSignature.builder().index(0).build();
					for (int k = 0; k < 3; k++) {
						subSignature.getInputs().put(RandomStringUtils.random(6, chars), rndIOType.get());
					}
					for (int k = 0; k < 4; k++) {
						subSignature.getOutputs().put(RandomStringUtils.random(6, chars), rndIOType.get());
					}
					signature.getSubNodes().add(subSignature);
				}

				// parameters
				for (int k = 0; k < 2; k++) {
					signature.getParameters().put(RandomStringUtils.random(6, chars),
							new ParameterSignature(RandomStringUtils.random(20, chars), rndClassName.get(), rnd.nextBoolean()));
				}

				signatures.put(signature.getIoHolderKey(), signature);
			}
			IOHolderProviderInfo info = IOHolderProviderInfo.builder()
					.providerID(RandomStringUtils.random(8, chars))
					.version(version).signatures(signatures).build();
			signatureMap.put(info.getProviderID() + ':' + info.getVersion(), info);
		}
	}

	@Before
	public void before() {
		registry = new IOSignatureRegistry(Object.class) {

			{
				signatureMap.forEach((key, value) -> signatures.put(key, new IOHolderProviderInfo(value)));
			}

			@Override
			protected VersionNumber getVersion(String provider) {
				return version;
			}
		};
	}

	@After
	public void after() {
		registry = null;
	}

	@Test
	public void testReadWrite() throws IOException {
		Path regPath = Paths.get("test signature");
		try {
			registry.write(regPath, true);
			AtomicReference<Map<String, IOHolderProviderInfo>> targetRef = new AtomicReference<>();
			IOSignatureRegistry readRegistry = new IOSignatureRegistry(Object.class) {

				{
					targetRef.set(signatures);
				}

				@Override
				protected VersionNumber getVersion(String provider) {
					return version;
				}
			};
			IOSignatureRegistry.read(regPath, readRegistry);
			registry.signatures.equals(targetRef.get());
			assertEquals(registry.signatures, targetRef.get());
		} finally {
			FileUtils.deleteQuietly(regPath.toFile());
		}
	}

}