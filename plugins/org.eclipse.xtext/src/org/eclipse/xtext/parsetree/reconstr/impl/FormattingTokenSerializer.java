/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.parsetree.reconstr.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.parsetree.reconstr.IInstanceDescription;
import org.eclipse.xtext.parsetree.reconstr.IParseTreeConstructor.IAbstractToken;
import org.eclipse.xtext.parsetree.reconstr.impl.AbstractFormattingConfig.ConfigRunner;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public abstract class FormattingTokenSerializer extends SimpleTokenSerializer {

	protected FormattingConfig config;

	protected ConfigRunner formatter;

	public FormattingTokenSerializer() {
		super();
		config = createFormattingConfig();
		configureFormatting(config);
	}

	protected void afterToken(IAbstractToken token) throws IOException {
		if (token.getNext() != null)
			formatter.collectLocators(token.getGrammarElement(), token
					.getNext().getGrammarElement());
		super.afterToken(token);
	}

	protected void beforeElement(IInstanceDescription curr, AbstractElement ele)
			throws IOException {
		if (outputHasStarted)
			append(formatter.getSummarizedSpaces());
		formatter.startCollectingLocators();
	}

	protected abstract void configureFormatting(FormattingConfig config);

	protected FormattingConfig createFormattingConfig() {
		return new FormattingConfig();
	}

	public void serialize(IAbstractToken firstToken, OutputStream out)
			throws IOException {
		formatter = config.run();
		super.serialize(firstToken, out);
		formatter = null;
	}

}
