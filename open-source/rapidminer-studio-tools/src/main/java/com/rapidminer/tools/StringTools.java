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
package com.rapidminer.tools;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * Utility methods for manipulating Strings (JSON, XML, ...).
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public enum StringTools {
	;

	/**
	 * Prefixes every occurrence of the escapeChar or any specialCharacter
	 *
	 * @param source            String with to be escaped characters
	 * @param escapeChar        single character to be used to escape special characters
	 * @param specialCharacters all the to be escaped characters
	 * @return a new String with escaped characters
	 * @since 9.8.0
	 */
	public static String escape(String source, char escapeChar, char[] specialCharacters) {
		if (source == null) {
			return null;
		}
		StringBuilder b = new StringBuilder();
		for (char c : source.toCharArray()) {
			if (c == escapeChar) {
				b.append(escapeChar); // escape escape character
			} else {
				for (char s : specialCharacters) {
					if (c == s) {
						// escape escape specials
						b.append(escapeChar);
						break;
					}
				}
			}
			b.append(c);
		}
		return b.toString();
	}


	/**
	 * Escapes the characters in a String using XML entities.
	 *
	 * @param string if {@code null}, will return 'null' as a string
	 * @return the string where xml entities have been escaped, never {@code null}
	 * @since 9.8.0
	 */
	public static String escapeXML(String string) {
		if (string == null) {
			return "null";
		}

		return StringEscapeUtils.escapeXml(string);
	}

	/**
	 * Basic escape for quotes, newlines, and backslashes.
	 *
	 * @param string if {@code null}, will return 'null' as a string
	 * @return the string which has been escaped, never {@code null}
	 * @since 9.8.0
	 */
	public static String escapeBasic(String string) {
		if (string == null) {
			return "null";
		}

		StringBuilder result = new StringBuilder();
		for (char c : string.toCharArray()) {
			switch (c) {
				case '"':
					result.append("\\\"");
					break;
				case '\\':
					result.append("\\\\");
					break;
				case '\n':
					result.append("\\n");
					break;
				default:
					result.append(c);
					break;
			}
		}
		return result.toString();
	}

	/**
	 * Splits the string at every split character unless escaped.
	 *
	 * @since 9.8.0
	 */
	public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter) {
		return unescape(source, escapeChar, specialCharacters, splitCharacter, -1);
	}

	/**
	 * Splits the string at every split character unless escaped. If the split limit is not -1, at most so many tokens
	 * will be returned. No more escaping is performed in the last token!
	 *
	 * @since 9.8.0
	 */
	public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter,
										int splitLimit) {
		List<String> result = new LinkedList<>();
		StringBuilder b = new StringBuilder();
		// was the last character read an escape character?
		boolean readEscape = false;
		int indexCount = -1;
		for (char c : source.toCharArray()) {
			indexCount++;
			// in escape mode -> just write special character, throw exception if not special?
			if (readEscape) {
				boolean found = false;
				if (c == splitCharacter) {
					found = true;
					b.append(c);
				} else if (c == escapeChar) {
					found = true;
					b.append(c);
				} else {
					for (char s : specialCharacters) {
						if (s == c) {
							found = true;
							b.append(c);
							break;
						}
					}
				}
				if (!found) {
					throw new IllegalArgumentException(
							"String '" + source + "' contains illegal escaped character '" + c + "'.");
				}
				// reset to regular mode
				readEscape = false;
			} else if (c == escapeChar) {
				// not in escape mode and read escape character -> go to escape mode
				readEscape = true;
			} else if (c == splitCharacter) {
				// not in escape mode and read split character -> split
				readEscape = false;
				result.add(b.toString());
				if (splitLimit != -1) {
					if (result.size() == splitLimit - 1) {
						// Only one left? Add to result and terminate.
						result.add(source.substring(indexCount + 1));
						return result;
					}
				}
				b = new StringBuilder();
			} else {
				// not in escape mode and read other character -> just write it
				readEscape = false;
				b.append(c);
			}
		}
		result.add(b.toString());
		return result;
	}

	/**
	 * Convert the given char array to a byte array, using {@link StandardCharsets#UTF_8}.
	 *
	 * @param chars the char array, must not be {@code null}
	 * @return the byte array, never {@code null}
	 * @since 9.8.0
	 */
	public static byte[] convertCharArrayToByteArray(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0);
		return bytes;
	}

	/**
	 * Convert the given byte array to a char array, using {@link StandardCharsets#UTF_8}.
	 *
	 * @param bytes the byte array, must not be {@code null}
	 * @return the char array, never {@code null}
	 * @since 9.8.0
	 */
	public static char[] convertByteArrayToCharArray(byte[] bytes) {
		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
		char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());
		Arrays.fill(charBuffer.array(), (char) 0);
		return chars;
	}

	/**
	 * Iterates over a string an replaces all occurrences of charToMask by '%'. Furthermore all appearing '%' will be
	 * escaped by '\' and all '\' will also be escaped by '\'. To unmask the resulting string again use {@link
	 * #unmask(char, String)}.<br> Examples (charToMask= '|'):<br> hello|mandy => hello%mandy<br> hel\lo|mandy =>
	 * hel\\lo%mandy<br> h%l\lo|mandy => h\%l\\lo%mandy<br>
	 *
	 * @param charToMask the character that should be masked. May not be '%' or '\\'
	 * @since 9.8.0
	 */
	public static String mask(char charToMask, String unmasked) {
		if (charToMask == '%' || charToMask == '\\') {
			throw new IllegalArgumentException("Parameter charToMask " + charToMask + " is not allowed!");
		}
		StringBuilder maskedStringBuilder = new StringBuilder();
		char maskChar = '%';
		char escapeChar = '\\'; // this means '\'
		for (char c : unmasked.toCharArray()) {
			if (c == charToMask) {
				maskedStringBuilder.append(maskChar);
			} else if (c == maskChar || c == escapeChar) {
				maskedStringBuilder.append(escapeChar);
				maskedStringBuilder.append(c);
			} else {
				maskedStringBuilder.append(c);
			}
		}

		return maskedStringBuilder.toString();
	}

	/**
	 * Unmaskes a masked string. Examples (charToUnmask= '|'):<br> hello%mandy => hello|mandy<br> hel\\lo%mandy =>
	 * hel\lo|mandy<br> h\%l\\lo%mandy => h%l\lo|mandy<br>
	 *
	 * @param charToUnmask the char that should be unmasked
	 * @since 9.8.0
	 */
	public static String unmask(char charToUnmask, String masked) {
		if (charToUnmask == '%' || charToUnmask == '\\') {
			throw new IllegalArgumentException("Parameter charToMask " + charToUnmask + " is not allowed!");
		}
		StringBuilder unmaskedStringBuilder = new StringBuilder();
		char maskChar = '%';
		char escapeChar = '\\';
		boolean escapeCharFound = false;
		for (char c : masked.toCharArray()) {
			if (c == maskChar) {
				if (escapeCharFound) {
					unmaskedStringBuilder.append(maskChar);
					escapeCharFound = false;
				} else {
					unmaskedStringBuilder.append(charToUnmask);
				}
			} else if (c == escapeChar) {
				if (escapeCharFound) {
					unmaskedStringBuilder.append(escapeChar);
					escapeCharFound = false;
				} else {
					escapeCharFound = true;
				}
			} else {
				unmaskedStringBuilder.append(c);
			}
		}

		return unmaskedStringBuilder.toString();
	}
}
