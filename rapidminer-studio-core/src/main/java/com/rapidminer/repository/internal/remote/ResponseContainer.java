/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.internal.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import com.rapidminer.tools.FunctionWithThrowable;
import com.rapidminer.tools.LogService;


/**
 * General container for the response of a request to RapidMiner AI Hub. Can only be used for responses <= 2 Gibibyte.
 *
 * <strong>
 * Avoid using this if possible. Consume responses in the {@link BaseServerClient} implementation and return proper
 * Objects instead.
 * </strong>
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class ResponseContainer {
	private FunctionWithThrowable<Void, InputStream, IOException> inputStream;
	private FunctionWithThrowable<Void, Integer, IOException> responseCode;
	private FunctionWithThrowable<Void, String, IOException> responseMessage;
	private Supplier<String> contentType;
	private FunctionWithThrowable<Void, OutputStream, IOException> outputStream;
	private Supplier<Map<String, List<String>>> headerFields;
	private Map<String, List<String>> requestProperties;


	/**
	 * The ResponseContainer reads a {@link HttpURLConnection} to keep the data even if the connection does not exist
	 * any longer. Can copy the {@link InputStream} unless keepOriginalStream is set to true.
	 *
	 * @param connection         to read and copy from
	 * @param keepOriginalStream will forward access to the original URLConnection {@link InputStream}, reading this may
	 *                           fail if the connection was closed in between.
	 * @throws IOException in case accessing the server failed technically
	 */
	public ResponseContainer(HttpURLConnection connection, boolean keepOriginalStream) throws IOException {
		// we need to keep a copy here to hold the data even if the connection was closed
		if (connection.getDoOutput()) {
			outputStream = nil -> connection.getOutputStream();
			responseCode = nil -> connection.getResponseCode();
			responseMessage = nil -> connection.getResponseMessage();
			contentType = connection::getContentType;
		} else {
			int responseCd = connection.getResponseCode();
			responseCode = nil -> responseCd;
			String responseMsg = connection.getResponseMessage();
			responseMessage = nil -> responseMsg;
			String contentTyp = connection.getContentType();
			contentType = () -> contentTyp;
		}

		if (connection.getDoInput()) {
			// cannot write output after reading input, so this needs to keep the original
			if (connection.getDoOutput() || keepOriginalStream) {
				inputStream = nil -> connection.getInputStream();
			} else {
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(connection.getInputStream()));
				inputStream = nil -> byteArrayInputStream;
				connection.disconnect();
			}
		}
		try {
			requestProperties = connection.getRequestProperties();
		} catch (IllegalStateException ise) {
			// too late, connection already established and then the request headers are unavailable
			requestProperties = Collections.emptyMap();
		}
		headerFields = connection::getHeaderFields;
	}

	/**
	 * The original responseCode of the connection. See {@link HttpURLConnection#getResponseCode()}
	 *
	 * @return a HTTP status response code like 200
	 */
	public int getResponseCode() {
		return responseCode.apply(null);
	}

	/**
	 * Retrieve the original or cached content of the original InputStream. Will throw a caught Exception when copying
	 * the InputStream already led to an IOException.
	 *
	 * @return the {@link InputStream} to be consumed by the caller
	 * @throws IOException in case accessing the {@link InputStream} fails
	 */
	public InputStream getInputStream() throws IOException {
		if (inputStream == null) {
			throw new IOException("No inputstream available");
		}
		return inputStream.applyWithException(null);
	}

	/**
	 * The original connection's responseMessage, see {@link HttpURLConnection#getResponseMessage()}
	 *
	 * @return the response of the connection
	 */
	public String getResponseMessage() {
		return responseMessage.apply(null);
	}

	/**
	 * Returns the value of the {@code content-type} header field.
	 *
	 * @return content type of the response, like "text/json"
	 */
	public String getContentType() {
		return contentType.get();
	}

	/**
	 * For writing purposes the {@link OutputStream} is referenced here directly, may be null in case the output was not
	 * requested from
	 *
	 * @return an outputStream to write to
	 * @throws IOException if accessing the {@link OutputStream} fails
	 */
	public OutputStream getOutputStream() throws IOException {
		if (outputStream == null) {
			throw new IOException("No outputstream available");
		}
		return outputStream.applyWithException(null);
	}

	/**
	 * Log details about connection issues. Unauthorized request details are logged as FINER loglevel while other
	 * response codes are logged as FINEST log.
	 *
	 * @since 9.8
	 */
	public void logDetails() {
		boolean unauthorized = getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED;
		if (unauthorized && LogService.getRoot().isLoggable(Level.FINER) || LogService.getRoot().isLoggable(Level.FINEST)) {
			StringBuilder sb = new StringBuilder();
			try {
				sb.append("Connection details - response code ")
						.append(responseCode)
						.append(": ")
						.append(getResponseMessage())
						.append("\nRequest headers:");
				getRequestProperties().forEach((key, value) ->
						sb.append("\n").append(key).append(":{").append(String.join(", ", value)).append("}"));
				sb.append("\nResponse headers:");
				getHeaderFields().forEach((key, value) ->
						sb.append("\n").append(key).append(":{").append(String.join(", ", value)).append("}"));
				LogService.getRoot().log(unauthorized ? Level.FINER : Level.FINEST, sb.toString());
			} catch (Throwable throwable) {
				LogService.getRoot().log(Level.FINEST, sb.toString(), throwable);
			}
		}
	}

	private Map<String, List<String>> getHeaderFields() {
		try {
			return headerFields.get();
		} catch (Throwable throwable) {
			return getStringListMap(throwable);
		}
	}

	private Map<String, List<String>> getRequestProperties() {
		return requestProperties;
	}

	private Map<String, List<String>> getStringListMap(Throwable throwable) {
		List<String> list = new ArrayList<>();
		list.add(throwable.getMessage());
		Arrays.stream(throwable.getStackTrace()).forEach(st -> list.add(st.toString()));
		return Collections.singletonMap("error", Collections.unmodifiableList(list));
	}
}
