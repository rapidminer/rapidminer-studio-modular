/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.io.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * @author Andreas Timm
 * @since 8.1
 */
public class XMLToolsTest {

	@BeforeClass
	public static void setup() throws Exception {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		EncryptionProvider.initialize();
		if (!ProductConstraintManager.INSTANCE.isInitialized()) {
			ProductConstraintManager.INSTANCE.initialize(null, null);
		}
		OperatorService.init();
	}

	@Test(expected = SAXParseException.class)
	public void loadCorruptXml() throws IOException, SAXException {
		XMLTools.createDocumentBuilder().parse(XMLToolsTest.class.getResourceAsStream("/com/rapidminer/io/process/XXE_corrupted.xml"));
	}

	@Test
	public void loadSomeProcessXml() throws IOException, SAXException {
		Document document = XMLTools.createDocumentBuilder().parse(XMLToolsTest.class.getResourceAsStream("/com/rapidminer/io/process/some_process.xml"));
		Assert.assertNotNull(document);
	}

	@Test
	public void checkParseTostringParse() throws IOException, SAXException, XMLException {
		String xmlSource = "/com/rapidminer/tools/plugin/extensions.xml";
		URL xmlUrl = XMLToolsTest.class.getResource(xmlSource);
		InputStream xmlInputstream = XMLToolsTest.class.getResourceAsStream(xmlSource);
		File xmlFile = new File(xmlUrl.getFile());
		String xmlString = FileUtils.readFileToString(xmlFile, StandardCharsets.UTF_8);

		Document parsedFromString = XMLTools.parse(xmlString);
		Document parsedFromFile = XMLTools.parse(xmlFile);
		Document parsedFromInputstream = XMLTools.parse(xmlInputstream);

		Assert.assertTrue(parsedFromFile.isEqualNode(parsedFromString));
		Assert.assertTrue(parsedFromInputstream.isEqualNode(parsedFromFile));

		String stringFromDocument = XMLTools.toString(parsedFromString);
		Assert.assertNotNull(stringFromDocument);

		Document documentFromStringFromDocument = XMLTools.parse(stringFromDocument);
		Assert.assertTrue(parsedFromInputstream.isEqualNode(documentFromStringFromDocument));
	}

	@Test
	public void testParameterEncodingDuringXMLCreation() {
		Process p = new Process();
		String logFileName = "日本語-öäüÖÄÜ.log";
		p.getRootOperator().setParameter(ProcessRootOperator.PARAMETER_LOGFILE, logFileName);
		String processXML = p.getRootOperator().getXML(false, EncryptionProvider.DEFAULT_CONTEXT);

		Assert.assertTrue("process XML did not contain expected log file name! Process XML was: " + processXML, processXML.contains(logFileName));
	}
}
