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
package com.sonar.sslr.impl;

import com.sonar.sslr.api.*;
import com.sonar.sslr.impl.events.ExtendedStackTrace;
import com.sonar.sslr.impl.events.ParsingEventListener;
import com.sonar.sslr.impl.matcher.GrammarFunctions;
import com.sonar.sslr.impl.matcher.RuleDefinition;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Parser<G extends Grammar> {

  private RuleDefinition rootRule;
  private ParsingState parsingState;
  private Lexer lexer;
  protected G grammar;
  private Set<RecognitionExceptionListener> listeners = new HashSet<RecognitionExceptionListener>();
  private ParsingEventListener[] parsingEventListeners;
  private ExtendedStackTrace extendedStackTrace;

  protected Parser() {
  }

  private Parser(Builder<G> builder) {
    this.lexer = builder.lexer;
    this.grammar = builder.grammar;
    this.listeners = builder.listeners;

    this.extendedStackTrace = builder.extendedStackTrace;
    if (this.extendedStackTrace != null) {
      this.parsingEventListeners = builder.parsingEventListeners
          .toArray(new ParsingEventListener[builder.parsingEventListeners.size() + 1]);
      this.parsingEventListeners[this.parsingEventListeners.length - 1] = this.extendedStackTrace;
    } else {
      this.parsingEventListeners = builder.parsingEventListeners.toArray(new ParsingEventListener[builder.parsingEventListeners.size()]);
    }

    GrammarFunctions.resetCache();
    this.rootRule = (RuleDefinition) this.grammar.getRootRule();
  }

  public void printStackTrace(PrintStream stream) {
    stream.append(ParsingStackTrace.generateFullStackTrace(getParsingState()));
  }

  public void addListener(RecognitionExceptionListener listerner) {
    listeners.add(listerner);
  }

  public AstNode parse(File file) {
    fireBeginLexEvent();
    try {
      lexer.lex(file);
    } catch (LexerException e) {
      throw new RecognitionException(e);
    } finally {
      fireEndLexEvent();
    }

    return parse(lexer.getTokens());
  }

  public AstNode parse(String source) {
    fireBeginLexEvent();
    try {
      lexer.lex(source);
    } catch (LexerException e) {
      throw new RecognitionException(e);
    } finally {
      fireEndLexEvent();
    }

    return parse(lexer.getTokens());
  }

  public AstNode parse(List<Token> tokens) {
    fireBeginParseEvent();

    try {
      parsingState = new ParsingState(tokens);
      parsingState.addListeners(listeners.toArray(new RecognitionExceptionListener[listeners.size()]));
      parsingState.parsingEventListeners = parsingEventListeners;
      parsingState.extendedStackTrace = extendedStackTrace;
      return rootRule.getRule().match(parsingState);
    } catch (BacktrackingEvent e) {
      throw extendedStackTrace == null ? new RecognitionException(parsingState, true) : new RecognitionException(extendedStackTrace, true);
    } finally {
      fireEndParseEvent();
    }
  }

  private void fireBeginLexEvent() {
    if (parsingEventListeners != null) {
      for (ParsingEventListener listener : this.parsingEventListeners) {
        listener.beginLex();
      }
    }
  }

  private void fireEndLexEvent() {
    if (parsingEventListeners != null) {
      for (ParsingEventListener listener : this.parsingEventListeners) {
        listener.endLex();
      }
    }
  }

  private void fireEndParseEvent() {
    if (parsingEventListeners != null) {
      for (ParsingEventListener listener : this.parsingEventListeners) {
        listener.endParse();
      }
    }
  }

  private void fireBeginParseEvent() {
    if (parsingEventListeners != null) {
      for (ParsingEventListener listener : this.parsingEventListeners) {
        listener.beginParse();
      }
    }
  }

  public ParsingState getParsingState() {
    return parsingState;
  }

  public G getGrammar() {
    return grammar;
  }

  public RuleDefinition getRootRule() {
    return rootRule;
  }

  public void setRootRule(Rule rootRule) {
    this.rootRule = (RuleDefinition) rootRule;
  }

  public static <G extends Grammar> Builder<G> builder(G grammar) {
    return new Builder<G>(grammar);
  }

  public static <G extends Grammar> Builder<G> builder(Parser<G> parser) {
    return new Builder<G>(parser);
  }

  public static final class Builder<G extends Grammar> {

    private Lexer lexer;
    private final G grammar;
    private final Set<ParsingEventListener> parsingEventListeners = new HashSet<ParsingEventListener>();
    private final Set<RecognitionExceptionListener> listeners = new HashSet<RecognitionExceptionListener>();
    private ExtendedStackTrace extendedStackTrace;

    private Builder(G grammar) {
      this.grammar = grammar;
    }

    private Builder(Parser<G> parser) {
      this.lexer = parser.lexer;
      this.grammar = parser.grammar;
      setParsingEventListeners(parser.parsingEventListeners);
      setRecognictionExceptionListener(parser.listeners.toArray(new RecognitionExceptionListener[parser.listeners.size()]));
      this.extendedStackTrace = parser.extendedStackTrace;
    }

    public Parser<G> build() {
      return new Parser<G>(this);
    }

    public Builder<G> withLexer(Lexer lexer) {
      this.lexer = lexer;
      return this;
    }

    public Builder<G> setParsingEventListeners(ParsingEventListener... parsingEventListeners) {
      this.parsingEventListeners.clear();
      addParsingEventListeners(parsingEventListeners);
      return this;
    }

    public Builder<G> addParsingEventListeners(ParsingEventListener... parsingEventListeners) {
      for (ParsingEventListener parsingEventListener : parsingEventListeners) {
        this.parsingEventListeners.add(parsingEventListener);
      }
      return this;
    }

    public Builder<G> setRecognictionExceptionListener(RecognitionExceptionListener... listeners) {
      this.listeners.clear();
      addRecognictionExceptionListeners(listeners);
      return this;
    }

    public Builder<G> addRecognictionExceptionListeners(RecognitionExceptionListener... listeners) {
      for (RecognitionExceptionListener listener : listeners) {
        this.listeners.add(listener);
      }
      return this;
    }

    public Builder<G> setExtendedStackTrace(ExtendedStackTrace extendedStackTrace) {
      this.extendedStackTrace = extendedStackTrace;
      return this;
    }

  }

}
