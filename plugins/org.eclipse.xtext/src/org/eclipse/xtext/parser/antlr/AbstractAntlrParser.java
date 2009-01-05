/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parser.antlr;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.LexerRule;
import org.eclipse.xtext.parser.IAstFactory;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.ParseException;
import org.eclipse.xtext.parser.ParseResult;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.LeafNode;
import org.eclipse.xtext.parsetree.NodeAdapter;
import org.eclipse.xtext.parsetree.NodeAdapterFactory;
import org.eclipse.xtext.parsetree.ParsetreeFactory;
import org.eclipse.xtext.parsetree.SyntaxError;
import org.eclipse.xtext.util.MultiMap;
import org.eclipse.xtext.util.Strings;

public abstract class AbstractAntlrParser extends Parser {

	protected CompositeNode currentNode;

	protected org.eclipse.xtext.Grammar grammar;

	protected IAstFactory factory;

	protected int lastConsumedIndex = -1;

	private MultiMap<Token, CompositeNode> deferredLookaheadMap = new MultiMap<Token, CompositeNode>();
	private Map<Token, LeafNode> token2NodeMap = new HashMap<Token, LeafNode>();

	protected AbstractAntlrParser(TokenStream input) {
		super(input);
	}

	public TokenStream getInput() {
		return input;
	}

	protected CompositeNode getCurrentNode() {
		return currentNode;
	}

	protected void associateNodeWithAstElement(CompositeNode node, EObject astElement) {
		if (astElement == null)
			throw new NullPointerException("passed astElement was null");
		if (node == null)
			throw new NullPointerException("passed node was null");
		if (node.getElement() != null && node.getElement() != astElement) {
			throw new ParseException("Reassignment of astElement in parse tree node");
		}
		if (node.getElement() != astElement) {
			node.setElement(astElement);
			NodeAdapter adapter = (NodeAdapter) NodeAdapterFactory.INSTANCE.adapt(astElement, AbstractNode.class);
			adapter.setParserNode(node);
		}
	}

	protected Object createLeafNode(String grammarElementID, String feature) {
		Token token = input.LT(-1);
		if (token != null && token.getTokenIndex() > lastConsumedIndex) {
			int indexOfTokenBefore = lastConsumedIndex;
			if (indexOfTokenBefore + 1 < token.getTokenIndex()) {
				for (int x = indexOfTokenBefore + 1; x < token.getTokenIndex(); x++) {
					Token hidden = input.get(x);
					LeafNode leafNode = createLeafNode(hidden, true);
					setLexerRule(leafNode, hidden);
				}
			}
			LeafNode leafNode = createLeafNode(token, false);
			leafNode.setGrammarElement(getGrammarElement(grammarElementID));
			leafNode.setFeature(feature);
			lastConsumedIndex = token.getTokenIndex();
			tokenConsumed(token, leafNode);
			return leafNode;
		}
		return null;
	}

	private EObject getGrammarElement(String grammarElementID) {
		URI uri = URI.createURI(grammarElementID);
//		URI resolved = new ClassloaderClasspathUriResolver().resolve(getClass().getClassLoader(), uri);
		return grammar.eResource().getResourceSet().getEObject(uri, true);
	}

	private Map<Integer, String> antlrTypeToLexerName = null;

	public void setTokenTypeMap(Map<Integer, String> tokenTypeMap) {
		antlrTypeToLexerName = new HashMap<Integer, String>();
		for(Entry<Integer, String> mapEntry: tokenTypeMap.entrySet()) {
			String value = mapEntry.getValue();
			if(TokenTool.isLexerRule(value)) {
				antlrTypeToLexerName.put(mapEntry.getKey(), TokenTool.getLexerRuleName(value));
			}
		}
	}

	protected void setLexerRule(LeafNode leafNode, Token hidden) {
		String ruleName = antlrTypeToLexerName.get(hidden.getType());
		AbstractRule rule = GrammarUtil.findRuleForName(grammar, ruleName);
		if (rule instanceof LexerRule) {
			leafNode.setGrammarElement(rule);
		}
	}

	protected CompositeNode createCompositeNode(String grammarElementID, CompositeNode parentNode) {
		CompositeNode compositeNode = ParsetreeFactory.eINSTANCE.createCompositeNode();
		if (parentNode != null)
			parentNode.getChildren().add(compositeNode);
		EObject grammarEle = getGrammarElement(grammarElementID);
		compositeNode.setGrammarElement(grammarEle);
		return compositeNode;
	}

	private void appendError(AbstractNode node) {
		if (currentError != null) {
			SyntaxError error = ParsetreeFactory.eINSTANCE.createSyntaxError();
			error.setMessage(currentError);
			node.setSyntaxError(error);
			currentError = null;
		}
	}

	private LeafNode createLeafNode(Token token, boolean isHidden) {
		LeafNode leafNode = ParsetreeFactory.eINSTANCE.createLeafNode();
		leafNode.setText(token.getText());
		leafNode.setHidden(isHidden);
		if (isSemanticChannel(token))
			appendError(leafNode);
		if (token.getType() == Token.INVALID_TOKEN_TYPE) {
			SyntaxError error = ParsetreeFactory.eINSTANCE.createSyntaxError();
			String lexerErrorMessage = ((XtextTokenStream) input).getLexerErrorMessage(token);
			error.setMessage(lexerErrorMessage);
			leafNode.setSyntaxError(error);
		}
		currentNode.getChildren().add(leafNode);
		return leafNode;
	}

	protected void appendAllTokens() {
		for (int x = lastConsumedIndex + 1; input.size() > x; input.consume(), x++) {
			Token hidden = input.get(x);
			LeafNode leafNode = createLeafNode(hidden, true);
			setLexerRule(leafNode, hidden);
		}
		if (currentError != null) {
			EList<LeafNode> leafNodes = currentNode.getLeafNodes();
			if (leafNodes.isEmpty()) {
				appendError(currentNode);
			} else {
				appendError(leafNodes.get(leafNodes.size() - 1));
			}
		}
	}

	private boolean isSemanticChannel(Token hidden) {
		return hidden.getChannel() != HIDDEN;
	}

	protected List<LeafNode> appendSkippedTokens() {
		List<LeafNode> skipped = new ArrayList<LeafNode>();
		Token currentToken = input.LT(-1);
		int currentTokenIndex = (currentToken == null) ? -1 : currentToken.getTokenIndex();
		Token tokenBefore = (lastConsumedIndex == -1) ? null : input.get(lastConsumedIndex);
		int indexOfTokenBefore = tokenBefore != null ? tokenBefore.getTokenIndex() : -1;
		if (indexOfTokenBefore + 1 < currentTokenIndex) {
			for (int x = indexOfTokenBefore + 1; x < currentTokenIndex; x++) {
				Token hidden = input.get(x);
				LeafNode leafNode = createLeafNode(hidden, true);
				setLexerRule(leafNode, hidden);
				skipped.add(leafNode);
			}
		}
		if (lastConsumedIndex < currentTokenIndex) {
			LeafNode leafNode = createLeafNode(currentToken, true);
			setLexerRule(leafNode, currentToken);
			skipped.add(leafNode);
			lastConsumedIndex = currentToken.getTokenIndex();
		}
		return skipped;
	}

	protected void appendTrailingHiddenTokens() {
		Token tokenBefore = input.LT(-1);
		int size = input.size();
		if (tokenBefore != null && tokenBefore.getTokenIndex() < size) {
			for (int x = tokenBefore.getTokenIndex() + 1; x < size; x++) {
				Token hidden = input.get(x);
				LeafNode leafNode = createLeafNode(hidden, true);
				setLexerRule(leafNode, hidden);
				lastConsumedIndex = hidden.getTokenIndex();
			}
		}
	}

	private String currentError = null;

	@Override
	public void recover(IntStream input, RecognitionException re) {
		if (currentError == null)
			currentError = getErrorMessage(re, getTokenNames());
		super.recover(input, re);
	}
	
	protected void handleValueConverterException(ValueConverterException vce) throws RecognitionException {
		Exception cause = (Exception) vce.getCause();
		if (vce != cause) {
			currentError = cause.getMessage();
			if (currentError == null)
				currentError = cause.getClass().getSimpleName();
			if (vce.getNode() == null) {
				final List<AbstractNode> children = currentNode.getChildren();
				if (children.isEmpty()) {
					appendError(currentNode);
				} else {
					appendError(children.get(children.size() - 1));
				}
			} else {
				appendError(vce.getNode());
			}
		} else {
			throw vce;
		}
	}

	@Override
	public void recoverFromMismatchedToken(IntStream in, RecognitionException re, int ttype, BitSet follow)
			throws RecognitionException {
		if (currentError == null)
			currentError = getErrorMessage(re, getTokenNames());
		super.recoverFromMismatchedToken(in, re, ttype, follow);
	}

	public final IParseResult parse() throws RecognitionException {
		return parse(getFirstRuleName());
	}

	public final IParseResult parse(String entryRuleName) throws RecognitionException {
		IParseResult result = null;
		EObject current = null;
		try {
			String antlrEntryRuleName = normalizeEntryRuleName(entryRuleName);
			try {
				Method method = this.getClass().getMethod(antlrEntryRuleName);
				current = (EObject) method.invoke(this);
			} catch (InvocationTargetException ite) {
				Throwable targetException = ite.getTargetException();
				if (targetException instanceof RecognitionException) {
					throw (RecognitionException) targetException;
				}
				if (targetException instanceof Exception) {
					throw new WrappedException((Exception) targetException);
				}
				throw new RuntimeException(targetException);
			} catch (Exception e) {
				throw new WrappedException(e);
			}
			appendTrailingHiddenTokens();
		} finally {
			try {
				appendAllTokens();
			} finally {
				result = new ParseResult(current, currentNode);
			}
		}
		return result;
	}

	private String normalizeEntryRuleName(String entryRuleName) {
		String antlrEntryRuleName;
		if (!entryRuleName.startsWith("entryRule")) {
			if (!entryRuleName.startsWith("rule")) {
				antlrEntryRuleName = "entryRule" + entryRuleName;
			} else {
				antlrEntryRuleName = "entry" + Strings.toFirstUpper(entryRuleName);
			}
		} else {
			antlrEntryRuleName = entryRuleName;
		}
		return antlrEntryRuleName;
	}

	private void tokenConsumed(Token token, LeafNode leafNode) {
		List<CompositeNode> nodesDecidingOnToken = deferredLookaheadMap.get(token);
		for (CompositeNode nodeDecidingOnToken : nodesDecidingOnToken) {
			nodeDecidingOnToken.getLookaheadLeafNodes().add(leafNode);
		}
		deferredLookaheadMap.remove(token);
		token2NodeMap.put(token, leafNode);
	}

	/**
	 * The current lookahead is the number of tokens that have been matched by
	 * the parent rule to decide that the current rule has to be called.
	 * 
	 * @return the currentLookahead
	 */
	protected void setCurrentLookahead() {
		XtextTokenStream xtextTokenStream = (XtextTokenStream) input;
		List<Token> lookaheadTokens = xtextTokenStream.getLookaheadTokens();
		for (Token lookaheadToken : lookaheadTokens) {
			LeafNode leafNode = token2NodeMap.get(lookaheadToken);
			if (leafNode == null) {
				deferredLookaheadMap.put(lookaheadToken, currentNode);
			} else {
				currentNode.getLookaheadLeafNodes().add(leafNode);
			}
		}
	}

	/**
	 * Sets the current lookahead to zero. See
	 * {@link AbstractAntlrParser#setCurrentLookahead()}
	 */
	protected void resetLookahead() {
		XtextTokenStream xtextTokenStream = (XtextTokenStream) input;
		xtextTokenStream.resetLookahead();
		token2NodeMap.clear();
	}

	protected void moveLookaheadInfo(CompositeNode source, CompositeNode target) {
		EList<LeafNode> sourceLookaheadLeafNodes = source.getLookaheadLeafNodes();
		target.getLookaheadLeafNodes().addAll(sourceLookaheadLeafNodes);
		sourceLookaheadLeafNodes.clear();

		for (Token deferredLookaheadToken : deferredLookaheadMap.keySet()) {
			List<CompositeNode> nodesDecidingOnToken = deferredLookaheadMap.get(deferredLookaheadToken);
			while (nodesDecidingOnToken.indexOf(source) != -1) {
				nodesDecidingOnToken.set(nodesDecidingOnToken.indexOf(source), target);
			}
		}
	}

	/**
	 * Match is called to consume unambiguous tokens. It calls input.LA() and
	 * therefore increases the currentLookahead. We need to compensate. See
	 * {@link AbstractAntlrParser#setCurrentLookahead()}
	 * 
	 * @see org.antlr.runtime.BaseRecognizer#match(org.antlr.runtime.IntStream,
	 *      int, org.antlr.runtime.BitSet)
	 */
	@Override
	public void match(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		XtextTokenStream xtextTokenStream = (XtextTokenStream) input;
		int numLookaheadBeforeMatch = xtextTokenStream.getLookaheadTokens().size();
		super.match(input, ttype, follow);
		if (xtextTokenStream.getLookaheadTokens().size() > numLookaheadBeforeMatch) {
			xtextTokenStream.removeLastLookaheadToken();
		}
	}

	protected abstract InputStream getTokenFile();

	/**
	 * @return
	 */
	protected abstract String getFirstRuleName();

}
