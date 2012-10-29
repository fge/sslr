/*
 * SonarSource Language Recognizer
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.sslr.tests;

import com.sonar.sslr.api.Rule;
import org.fest.assertions.GenericAssert;
import org.junit.ComparisonFailure;
import org.sonar.sslr.internal.matchers.GrammarElementMatcher;
import org.sonar.sslr.matchers.Matchers;
import org.sonar.sslr.matchers.ParseErrorFormatter;
import org.sonar.sslr.matchers.ParseRunner;
import org.sonar.sslr.matchers.ParsingResult;

/**
 * <p>This class is not intended to be instantiated or sub-classed by clients.</p>
 */
public class RuleAssert extends GenericAssert<RuleAssert, Rule> {

  private final ParseRunner parseRunner;

  public RuleAssert(Rule actual) {
    super(RuleAssert.class, actual);
    isNotNull();
    GrammarElementMatcher matcher = new GrammarElementMatcher(getRuleName() + " with end of input")
        .is(actual, Matchers.endOfInput());
    this.parseRunner = new ParseRunner(matcher);
  }

  /**
   * Verifies that the actual <code>{@link Rule}</code> fully matches a given input.
   * @return this assertion object.
   */
  public RuleAssert matches(String input) {
    ParsingResult parsingResult = parseRunner.parse(input.toCharArray());
    if (!parsingResult.isMatched()) {
      String expected = "Rule '" + getRuleName() + "' should match:\n" + input;
      String actual = new ParseErrorFormatter().format(parsingResult.getParseError());
      final String message = expected + '\n' + actual;
      throw new ComparisonFailure(message, expected, actual) {
        @Override
        public String getMessage() {
          return message;
        }
      };
    }
    return this;
  }

  /**
   * Verifies that the actual <code>{@link Rule}</code> not matches a given input.
   * @return this assertion object.
   */
  public RuleAssert notMatches(String input) {
    ParsingResult parsingResult = parseRunner.parse(input.toCharArray());
    if (parsingResult.isMatched()) {
      throw new AssertionError("Rule '" + getRuleName() + "' should not match:\n" + input);
    }
    return this;
  }

  private String getRuleName() {
    return ((GrammarElementMatcher) actual).getName();
  }

}