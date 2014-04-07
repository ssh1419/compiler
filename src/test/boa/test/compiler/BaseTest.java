/*
 * Copyright 2014, Hridesh Rajan, Robert Dyer, 
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.test.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.tools.ToolProvider;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;

import org.junit.BeforeClass;

import org.stringtemplate.v4.ST;

import boa.compiler.SymbolTable;
import boa.compiler.ast.Start;
import boa.compiler.transforms.LocalAggregationTransformer;
import boa.compiler.transforms.VisitorOptimizingTransformer;
import boa.compiler.visitors.AbstractCodeGeneratingVisitor;
import boa.compiler.visitors.CodeGeneratingVisitor;
import boa.compiler.visitors.TypeCheckingVisitor;

import boa.parser.BoaLexer;
import boa.parser.BoaParser;
import boa.parser.BoaParser.StartContext;

/**
 * @author rdyer
 */
public abstract class BaseTest {
	protected static boolean DEBUG = false;

	@BeforeClass
	public static void setUp() throws IOException {
		SymbolTable.initialize(new ArrayList<URL>());
	}


	//
	// lexing
	//

	protected CommonTokenStream lex(final String input) throws IOException {
		return lex(input, new int[0], new String[0]);
	}

	protected CommonTokenStream lex(final String input, final int[] ids, final String[] strings) throws IOException {
		return lex(input, ids, strings, new String[0]);
	}

	protected CommonTokenStream lex(final String input, final int[] ids, final String[] strings, final String[] errors) throws IOException {
		final List<String> foundErr = new ArrayList<String>();
		final BoaLexer lexer = new BoaLexer(new ANTLRInputStream(new StringReader(input)));
		lexer.removeErrorListeners();
		lexer.addErrorListener(new BaseErrorListener () {
			@Override
			public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line, final int charPositionInLine, final String msg, final RecognitionException e) {
				foundErr.add(line + "," + charPositionInLine + ": " + msg);
			}
		});

		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();

		if (ids.length > 0 && strings.length > 0)
			assertEquals("ids != strings", ids.length, strings.length);

		if (ids.length > 0) {
			final List<Token> t = tokens.getTokens();
			if (DEBUG) {
				for (int i = 0; i < t.size(); i++) {
					final Token token = t.get(i);
					System.out.print(token.getType() + ", ");
				}
				System.out.println();
				for (int i = 0; i < t.size(); i++) {
					final Token token = t.get(i);
					System.out.print(token.getText() + ", ");
				}
				System.out.println();
				System.out.println();
			}
			assertEquals("wrong number of tokens", ids.length, t.size());
			for (int i = 0; i < t.size(); i++)
				assertEquals("wrong token type", ids[i], t.get(i).getType());
		}

		if (strings.length > 0) {
			final List<Token> t = tokens.getTokens();
			assertEquals("wrong number of tokens", strings.length, t.size());
			for (int i = 0; i < t.size(); i++)
				assertEquals("wrong token type", strings[i], t.get(i).getText());
		}

		assertEquals("wrong number of errors: " + input, errors.length, foundErr.size());
		for (int i = 0; i < foundErr.size(); i++)
			assertEquals("wrong error", errors[i], foundErr.get(i));

		return tokens;
	}


	//
	// parsing
	//

	protected StartContext parse(final String input) throws IOException {
		return parse(input, new String[0]);
	}

	protected StartContext parse(final String input, final String[] errors) throws IOException {
		final BoaParser parser = new BoaParser(lex(input));

		final List<String> foundErr = new ArrayList<String>();
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener () {
			@Override
			public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line, final int charPositionInLine, final String msg, final RecognitionException e) {
				foundErr.add(line + "," + charPositionInLine + ": " + msg);
			}
		});

		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		final StartContext p = parser.start();

		assertEquals("wrong number of errors", errors.length, foundErr.size());
		for (int i = 0; i < foundErr.size(); i++)
			assertEquals("wrong error", errors[i], foundErr.get(i));

		return p;
	}


	//
	// type checking
	//

	protected void typecheck(final String input, final String error) throws IOException {
		final Start p = parse(input).ast;

		try {
			new TypeCheckingVisitor().start(p, new SymbolTable());
			if (error != null)
				fail("expected error: " + error);
		} catch (final Exception e) {
			if (error == null)
				fail("found unexpected error: " + e.getMessage());
			else
				assertEquals(error, e.getMessage());
		}
	}

	
	//
	// code generation
	//

	protected void codegen(final String input) throws IOException {
		codegen(input, null);
	}

	protected void codegen(final String input, final String error) throws IOException {
		final Start p = parse(input).ast;

		final File outputRoot = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString());
		final File outputSrcDir = new File(outputRoot, "boa");
		if (!outputSrcDir.mkdirs())
			throw new IOException("unable to mkdir " + outputSrcDir);
		final File outputFile = new File(outputSrcDir, "Test.java");

		CodeGeneratingVisitor.combineTableStrings.clear();
		CodeGeneratingVisitor.reduceTableStrings.clear();

		final List<String> jobnames = new ArrayList<String>();
		final List<String> jobs = new ArrayList<String>();

		try {
			new TypeCheckingVisitor().start(p, new SymbolTable());
			new LocalAggregationTransformer().start(p);
			new VisitorOptimizingTransformer().start(p);

			final CodeGeneratingVisitor cg = new CodeGeneratingVisitor("1");
			cg.start(p);
			jobs.add(cg.getCode());
			jobnames.add("1");

			final ST st = AbstractCodeGeneratingVisitor.stg.getInstanceOf("Program");

			st.add("name", "Test");
			st.add("numreducers", 1);
			st.add("jobs", jobs);
			st.add("jobnames", jobnames);
			st.add("combineTables", CodeGeneratingVisitor.combineTableStrings);
			st.add("reduceTables", CodeGeneratingVisitor.reduceTableStrings);
			st.add("splitsize", 64 * 1024 * 1024);

			final BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(outputFile));
			try {
				o.write(st.render().getBytes());
			} finally {
				o.close();
			}

			if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-cp", System.getProperty("java.class.path"), outputFile.toString()) != 0)
				throw new RuntimeException("compile failed");

			if (error != null)
				fail("expected error: " + error);
		} catch (final Exception e) {
			if (error == null)
				fail("found unexpected error: " + e.getMessage());
			else
				assertEquals(error, e.getMessage());
		}

		delete(outputSrcDir);
	}


	//
	// misc utils
	//

	protected String load(final String fileName) throws IOException {
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fileName));
			final byte[] bytes = new byte[(int) new File(fileName).length()];
			in.read(bytes);
			return new String(bytes);
		} finally {
			if (in != null)
				in.close();
		}
	}

	protected final void delete(final File f) throws IOException {
		if (f.isDirectory())
			for (final File g : f.listFiles())
				delete(g);

		if (!f.delete())
			throw new IOException("unable to delete file " + f);
	}
}
