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
package org.sonar.sslr.matchers;

import org.junit.Test;
import org.sonar.sslr.internal.matchers.InputBuffer;

import static org.fest.assertions.Assertions.assertThat;

public class ParseErrorFormatterTest {

  @Test
  public void test() {
    InputBuffer inputBuffer = new InputBuffer("foo\nbar\r\nbaz".toCharArray());
    ParseErrorFormatter formatter = new ParseErrorFormatter();
    String result = formatter.format(new ParseError(inputBuffer, 5, "expected: IDENTIFIER"));
    System.out.print(result);
    String expected = new StringBuilder()
        .append("At line 2 column 2 expected: IDENTIFIER\n")
        .append("1: foo\n")
        .append("2: bar\n")
        .append("    ^\n")
        .append("3: baz\n")
        .toString();
    assertThat(result).isEqualTo(expected);
  }

}
