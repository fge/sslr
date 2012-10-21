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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Rule;
import org.sonar.sslr.internal.matchers.*;

import java.util.List;

/**
 * <p>This class is not intended to be subclassed by clients.</p>
 */
public class ParseRunner {

  private final Matcher rootMatcher;

  public ParseRunner(Rule rule) {
    this.rootMatcher = (Matcher) rule;
  }

  public ParsingResult parse(char[] input) {
    Memoizer memoizer = new Memoizer(input.length);
    ErrorLocatingHandler errorLocatingHandler = new ErrorLocatingHandler(memoizer);
    MatcherContext matcherContext = new BasicMatcherContext(input, errorLocatingHandler, rootMatcher);
    boolean matched = matcherContext.runMatcher();
    if (matched) {
      return new ParsingResult(matched, matcherContext.getNode(), null);
    } else {
      StringBuilder sb = new StringBuilder("expected");
      if (errorLocatingHandler.failedMatchers.size() > 1) {
        sb.append(" one of");
      }
      sb.append(':');
      for (Matcher failedMatcher : errorLocatingHandler.failedMatchers) {
        sb.append(' ').append(((GrammarElementMatcher) failedMatcher).getName());
      }
      ParseError parseError = new ParseError(new InputBuffer(input), errorLocatingHandler.errorIndex, sb.toString());
      return new ParsingResult(matched, null, parseError);
    }
  }

  // TODO Godin: To increase performance we might use two runs in case of error.
  // One to locate error position and another one to construct report.
  private static class ErrorLocatingHandler implements MatchHandler {

    private final MatchHandler delegate;
    private int errorIndex = -1;
    private final List<Matcher> failedMatchers = Lists.newArrayList();

    public ErrorLocatingHandler(MatchHandler delegate) {
      this.delegate = delegate;
    }

    public boolean match(MatcherContext context) {
      return delegate.match(context);
    }

    public void onMatch(MatcherContext context) {
      delegate.onMatch(context);
    }

    public void onMissmatch(MatcherContext context) {
      // We are interested in errors, which occur only on terminals:
      if (((GrammarElementMatcher) context.getMatcher()).getTokenType() != null) {
        // FIXME Godin: for the moment we assume that error cannot occur inside of predicate
        if (errorIndex < context.getCurrentIndex()) {
          errorIndex = context.getCurrentIndex();
          failedMatchers.clear();
          failedMatchers.add(context.getMatcher());
        } else if (errorIndex == context.getCurrentIndex()) {
          failedMatchers.add(context.getMatcher());
        }
      }
    }

  }

}