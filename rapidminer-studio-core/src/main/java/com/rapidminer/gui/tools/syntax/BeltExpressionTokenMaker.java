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
package com.rapidminer.gui.tools.syntax;

import static org.fife.ui.rsyntaxtextarea.TokenTypes.ANNOTATION;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.COMMENT_KEYWORD;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.COMMENT_MULTILINE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.IDENTIFIER;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_FLOAT;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.NULL;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.OPERATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.RESERVED_WORD;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.SEPARATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.VARIABLE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.WHITESPACE;

import java.util.LinkedList;
import java.util.List;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInput;


/**
 * A copy of the {@link ExpressionTokenMaker} but for the new expression parser.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class BeltExpressionTokenMaker extends AbstractTokenMaker {

	/** list of all existing functions that are highlighted */
	private static List<FunctionDescription> functions = new LinkedList<>();

	/** list of all given {@link FunctionInput}s */
	private static List<FunctionInput> functionInputs = new LinkedList<>();

	/**
	 * Adds the given {@link FunctionDescription}s to the list of functions that are highlighted
	 *
	 * @param functions
	 *            list of {@link FunctionDescription}s
	 */
	public static void addFunctions(List<FunctionDescription> functions) {
		BeltExpressionTokenMaker.functions.addAll(functions);
	}

	/**
	 * Adds the given {@link FunctionInput}s to the list of highlighted variables
	 *
	 * @param functionInputs
	 *            list of {@link FunctionInput}s
	 */
	public static void addFunctionInputs(List<FunctionInput> functionInputs) {
		BeltExpressionTokenMaker.functionInputs.addAll(functionInputs);
	}

	/**
	 * Removes all {@link FunctionInput}s from the list of highlighted variables
	 */
	public static void removeFunctionInputs() {
		BeltExpressionTokenMaker.functionInputs.clear();
	}

	@Override
	public Token getTokenList(Segment text, int startTokenType, int startOffset) {

		// reset tokens
		resetTokenList();

		// array containing all characters
		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;

		// Token starting offsets are always of the form:
		// 'startOffset + (currentTokenStart-offset)', but since startOffset and
		// offset are constant, tokens' starting positions become:
		// 'newStartOffset+currentTokenStart'.
		int newStartOffset = startOffset - offset;

		int currentTokenStart = offset;
		int currentTokenType = startTokenType;

		// go through the characters and find tokens
		for (int i = offset; i < end; i++) {

			// current character
			char c = array[i];

			// create tokens by considering the current token type
			switch (currentTokenType) {

				// if the token type is null, i.e. at the beginning of a line
				case NULL:

					// start a new token here
					currentTokenStart = i;

					switch (c) {

						case ' ':
						case '\t':
							// character is a whitespace, found current token type and go to the
							// next character
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// character is a double quote, found current token type and go to the
							// next character
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// character is a separator, found current token type and go to the
							// next character
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// character is a comment keyword (attribute) token, found current token
							// type and go to the next character
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// character is an operator, found current token type and go to the
							// next character
							currentTokenType = OPERATOR;
							break;
						default:
							// if the current character is a digit, found current token type and go
							// to the
							// next character
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = LITERAL_NUMBER_DECIMAL_INT;
								break;
								// else if the current character is a letter, found current token
								// type and go to the next character
							} else if (RSyntaxUtilities.isLetter(c)) {
								currentTokenType = IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as an identifier
							currentTokenType = IDENTIFIER;
							break;

					} // End of switch c
					break;

					// case that the character before was an operator
				case OPERATOR:

					switch (c) {

						case ' ':
						case '\t':
							// as this is no longer an operator, save the operator token, remember
							// that this is a whitespace token and go to the next character
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// as this is no longer an operator, save the operator token, remember
							// that this is a double quote token and go to the next character
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
							// if the opening curled bracket occurs after a per cent symbol, it's a
							// macro. Store the macro token in this case, otherwise a simple
							// separator token
							if (i - 1 >= 0 && text.array[i - 1] == '%') {
								currentTokenType = COMMENT_MULTILINE;
								break;
							}
						case '}':
						case '(':
						case ')':
						case ',':
							// as this is no longer an operator, save the operator token, remember
							// that this is a separator token and go to the next character
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// as this is no longer an operator, save the operator token, remember
							// that this is a comment keyword (attribute) token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '^':
						case '%':
							// also an operator, but those operators do not occur twice, so store
							// the previous operator to start a new operator token
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							break;
						case '<':
						case '>':
						case '!':
						case '=':
						case '&':
						case '|':
							// still operator
							break;

						default:   // Add the operator token and start anew.
							// as this is no longer an operator, save the operator token
							addToken(text, currentTokenStart, i - 1, OPERATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;

							// remember that this is an integer token and go to the next character
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = LITERAL_NUMBER_DECIMAL_INT;
								break;
							} else if (RSyntaxUtilities.isLetter(c)) {
								// remember that this is an identifier (word) token and go to the
								// next character
								currentTokenType = IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as identifier
							currentTokenType = IDENTIFIER;

					} // End of switch on c
					break;

					// case that the character before was a separator
				case SEPARATOR:

					switch (c) {

						case ' ':
						case '\t':
							// as this is no longer a separator, save the separator token, remember
							// that this is a whitespace token and go to the next character
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// as this is no longer a separator, save the separator token, remember
							// that this is a double quote token and go to the next character
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// this is still a separator, but save the separator token, because
							// separators appear alone and go to the next character
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							break;
						case '[':
							// as this is no longer a separator, save the separator token, remember
							// that this is a comment keyword (attribute) and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// as this is no longer a separator, save the separator token, remember
							// that this is an operator token and go to the next character
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = OPERATOR;
							break;

						default:   // Add the separator token and start anew.

							// as this is no longer an separator, save the separator token
							addToken(text, currentTokenStart, i - 1, SEPARATOR, newStartOffset + currentTokenStart);
							currentTokenStart = i;

							// remember that this is an integer token and go to the next character
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = LITERAL_NUMBER_DECIMAL_INT;
								break;
							} else if (RSyntaxUtilities.isLetter(c)) {
								// remember that this is an identifier (word) token and go to the
								// next character
								currentTokenType = IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as identifier
							currentTokenType = IDENTIFIER;

					} // End of switch c
					break;

					// case that the character before was a whitespace
				case WHITESPACE:

					switch (c) {

						case ' ':
						case '\t':
							// Still whitespace.
							break;

						case '"':
							// as this is no longer whitespace, save the whitespace token, remember
							// that this is a double quote token and go to the next character
							addToken(text, currentTokenStart, i - 1, WHITESPACE, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// as this is no longer whitespace, save the whitespace token, remember
							// that this is a separator token and go to the next character
							addToken(text, currentTokenStart, i - 1, WHITESPACE, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// as this is no longer whitespace, save the whitespace token, remember
							// that this is a comment keyword(attribute) and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, WHITESPACE, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// as this is no longer whitespace, save the whitespace token, remember
							// that this is an operator token and go to the next character
							addToken(text, currentTokenStart, i - 1, WHITESPACE, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = OPERATOR;
							break;

						default:   // Add the whitespace token and start anew.
							// as this is no longer whitespace, save the whitespace token
							addToken(text, currentTokenStart, i - 1, WHITESPACE, newStartOffset + currentTokenStart);
							currentTokenStart = i;

							// remember that this is an integer token and go to the next character
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = LITERAL_NUMBER_DECIMAL_INT;
								break;
							} else if (RSyntaxUtilities.isLetter(c)) {
								// remember that this is an identifier (word) token and go to the
								// next character
								currentTokenType = IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as identifier
							currentTokenType = IDENTIFIER;

					} // End of switch c
					break;

					// case that the character before was an identifier (part of function, attribute or
					// unknown word)
				case IDENTIFIER:

					switch (c) {

						case ' ':
						case '\t':
							// as this is no longer an identifier, save the identifier token,
							// remember that this is a whitespace token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, IDENTIFIER, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// as this is no longer an identifier, save the identifier token,
							// remember that this is a double quote token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, IDENTIFIER, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// as this is no longer an identifier, save the identifier token,
							// remember that this is a separator token and go to the next character
							addToken(text, currentTokenStart, i - 1, IDENTIFIER, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// as this is no longer an identifier, save the identifier token,
							// remember that this is a comment keyword (attribute) token and go to
							// the next character
							addToken(text, currentTokenStart, i - 1, IDENTIFIER, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// as this is no longer an identifier, save the identifier token,
							// remember that this is an operator token and go to the next character
							addToken(text, currentTokenStart, i - 1, IDENTIFIER, newStartOffset + currentTokenStart);
							currentTokenStart = i;
							currentTokenType = OPERATOR;
							break;

						default:
							// Still an identifier of some type.
							break;

					} // End of switch c
					break;

					// case that the character before was an integer
				case LITERAL_NUMBER_DECIMAL_INT:

					switch (c) {

						case ' ':
						case '\t':
							// as this is no longer an integer, save the integer token,
							// remember that this is a whitespace token and go to the next character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// as this is no longer an integer, save the integer token,
							// remember that this is a double quote token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// as this is no longer an integer, save the integer token,
							// remember that this is a separator token and go to the next character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// as this is no longer an integer, save the integer token,
							// remember that this is a comment keyword (attribute) token and go to
							// the next character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// as this is no longer an integer, save the integer token,
							// remember that this is an operator token and go to the next character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = OPERATOR;
							break;

						default:
							// Still a literal number
							if (RSyntaxUtilities.isDigit(c)) {
								break;

							} else if (c == '.') {
								// is a floating number if the current character is a dot and the
								// next character is a digit
								if (text.array.length > i + 1 && RSyntaxUtilities.isDigit(text.array[i + 1])) {
									currentTokenType = LITERAL_NUMBER_FLOAT;
									break;
								}
							} else if (c == 'e' || c == 'E') {
								// is a floating number if the current token contains a correct
								// scientific notation -> which means it contains one e or E with a
								// following number or it contains one e or E with a following plus
								// or minus symbol and a following number
								boolean numberIsFollowing = false;
								if (text.array.length > i + 1 && (RSyntaxUtilities.isDigit(text.array[i + 1]) || text.array.length > i + 2
										&& (text.array[i + 1] == '+' || text.array[i + 1] == '-')
										&& RSyntaxUtilities.isDigit(text.array[i + 2]))) {
									numberIsFollowing = true;
								}
								if (numberIsFollowing) {
									// is a floating number.
									currentTokenType = LITERAL_NUMBER_FLOAT;
									break;
								}
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_DECIMAL_INT, newStartOffset
									+ currentTokenStart);
							i--;
							currentTokenType = NULL;

					} // End of switch c
					break;

					// case that the character before was a float
				case LITERAL_NUMBER_FLOAT:

					switch (c) {

						case ' ':
						case '\t':
							// as this is no longer a float, save the float token,
							// remember that this is a whitespace token and go to the next character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = WHITESPACE;
							break;

						case '"':
							// as this is no longer a float, save the float token,
							// remember that this is a double quote token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = LITERAL_STRING_DOUBLE_QUOTE;
							break;
						case '{':
						case '(':
						case ')':
						case '}':
						case ',':
							// as this is no longer a float, save the float token,
							// remember that this is a separator token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = SEPARATOR;
							break;
						case '[':
							// as this is no longer a float, save the float token,
							// remember that this is a comment keyword (attribute) token and go to
							// the next
							// character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = COMMENT_KEYWORD;
							break;
						case '+':
						case '-':
							// - and + is allowed if the scientific notation is used and the
							// previous character was the e or E
							if (text.array[i - 1] == 'e' || text.array[i - 1] == 'E') {
								currentTokenType = LITERAL_NUMBER_FLOAT;
								break;
							}
						case '/':
						case '*':
						case '<':
						case '>':
						case '=':
						case '!':
						case '&':
						case '|':
						case '^':
						case '%':
							// as this is no longer a float, save the float token,
							// remember that this is an operator token and go to the next
							// character
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							currentTokenStart = i;
							currentTokenType = OPERATOR;
							break;

						default:

							// Still a literal number.
							if (RSyntaxUtilities.isDigit(c)) {
								break;

							} else if (c == 'e' || c == 'E') {
								// check whether this token already contains an e
								boolean containsE = false;
								for (int j = currentTokenStart; j < i - 1; j++) {
									if (text.array[j] == 'e' || text.array[j] == 'E') {
										containsE = true;
										break;
									}
								}
								// if the current number does not already contain an e for the
								// scientific notation, it is still a number
								if (!containsE) {
									// is a floating number if the current token contains a correct
									// scientific notation -> which means it contains one e or E
									// with a
									// following number or it contains one e or E with a following
									// plus
									// or minus symbol and a following number
									boolean numberIsFollowing = false;
									if (text.array.length > i + 1 && (RSyntaxUtilities.isDigit(text.array[i + 1]) || text.array.length > i + 2
											&& (text.array[i + 1] == '+' || text.array[i + 1] == '-')
											&& RSyntaxUtilities.isDigit(text.array[i + 2]))) {
										numberIsFollowing = true;
									}
									if (numberIsFollowing) {
										// is a floating number
										currentTokenType = LITERAL_NUMBER_FLOAT;
										break;
									}
								}
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart, i - 1, LITERAL_NUMBER_FLOAT, newStartOffset
									+ currentTokenStart);
							i--;
							currentTokenType = NULL;

					} // End of switch c
					break;

					// case that the character before was a double quote
				case LITERAL_STRING_DOUBLE_QUOTE:
					if (c == '"') {
						// check if the string really ends or if the " is escaped
						int numberBackslashes = 0;
						int j = i - 1;
						while (j > 0) {
							if (text.array[j] == '\\') {
								numberBackslashes++;
							} else {
								break;
							}
							j--;
						}
						// if the double quote is not escaped, add the string token
						if (numberBackslashes % 2 == 0) {
							addToken(text, currentTokenStart, i, LITERAL_STRING_DOUBLE_QUOTE, newStartOffset
									+ currentTokenStart);
							currentTokenType = NULL;
						}
					}
					break;

					// case that the character before was part of a macro
				case COMMENT_MULTILINE:

					if (c == '}') {
						// check if the macro really ends or if the } is escaped
						int numberBackslashes = 0;
						int j = i - 1;
						while (j > 0) {
							if (text.array[j] == '\\') {
								numberBackslashes++;
							} else {
								break;
							}
							j--;
						}
						// if the macro is not escaped, add the macro token
						if (numberBackslashes % 2 == 0) {
							addToken(text, currentTokenStart, i, COMMENT_MULTILINE, newStartOffset + currentTokenStart);
							currentTokenType = NULL;
						}
					}
					break;
					// case that the character before was part of an attribute
				case COMMENT_KEYWORD:

					if (c == ']') {
						// check if the attribute really ends or if the ] is escaped
						int numberBackslashes = 0;
						int j = i - 1;
						while (j > 0) {
							if (text.array[j] == '\\') {
								numberBackslashes++;
							} else {
								break;
							}
							j--;
						}
						// if the attribute is not escaped, add the attribute token
						if (numberBackslashes % 2 == 0) {
							addToken(text, currentTokenStart, i, COMMENT_KEYWORD, newStartOffset + currentTokenStart);
							currentTokenType = NULL;
						}
					}
					break;

				default: // Should never happen
			} // End of switch on currentTokenType
		} // End of for loop

		// The end of the line is reached. Check whether all tokens are closed.
		switch (currentTokenType) {

			// if there is an open double quote token, the next line begins with a double quote token
			case LITERAL_STRING_DOUBLE_QUOTE:
				addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
				break;

				// Do nothing if everything was okay.
			case NULL:
				addNullToken();
				break;

				// All other token types don't continue to the next line and are cut here
			default:
				addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
				addNullToken();
		}

		// Return the first token in our linked list.
		return firstToken;
	}

	@Override
	public TokenMap getWordsToHighlight() {

		// add all words that should be highlighted
		TokenMap tokenMap = new TokenMap();

		// add known functions
		if (functions != null) {
			for (FunctionDescription function : functions) {
				// add the function name without the opening bracket
				// Token.RESERVED_WORD does only say in which style the function names are
				// rendered
				tokenMap.put(function.getDisplayName().split("\\(")[0], RESERVED_WORD);
			}
		}
		// add regular attributes
		if (functionInputs != null) {
			for (FunctionInput input : functionInputs) {

				if (input.getCategory() == FunctionInput.Category.DYNAMIC) {
					// add the name of the dynamic variable
					// Token.VARIABLE does only say in which style the dynamic variables are
					// rendered
					String inputName = input.getName();
					if (inputName.matches("(^[A-Za-z])([A-Z_a-z\\d]*)")) {
						// check whether the attribute is alphanumerical without a number at the
						// front
						tokenMap.put(inputName, VARIABLE);
					} else {
						// if the attribute is not alphanumeric, add it with the brackets,
						// escape [ and ]
						inputName = inputName.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
						tokenMap.put("[" + inputName + "]", VARIABLE);
					}
				} else if (input.getCategory() == FunctionInput.Category.CONSTANT) {
					// add the constant name
					// Token.ANNOTATION does only say in which style the constants are rendered
					tokenMap.put(input.getName(), ANNOTATION);
				}
				// Category.SCOPE variables are recognized with the surrounding
				// brackets
			}
		}
		tokenMap.put("[", VARIABLE);
		tokenMap.put("]", VARIABLE);

		// return the complete map of known words that should be highlighted
		return tokenMap;
	}

	@Override
	public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
		// used to differ the style of known words and unknown words
		// we overwrite the tokenType for known words to render them differently

		// all keywords were parsed as "identifiers" (unknown words)
		// but if we know the word is contained in wordsToHighlight, we render it like we defined it
		// in wordsToHighlight
		if (tokenType == OPERATOR || tokenType == IDENTIFIER || tokenType == COMMENT_KEYWORD) {
			// operators such as + are functions that are placed between two keywords.
			// If the operator is contained in the functions, it will be rendered like a function
			int value = wordsToHighlight.get(segment, start, end);
			if (value != -1) {
				tokenType = value;
			}
		}
		// add the token with the overwritten type
		super.addToken(segment, start, end, tokenType, startOffset);
	}
}
