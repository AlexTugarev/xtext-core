/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parser;

import org.eclipse.xtext.parsetree.LeafNode;

/**
 * @author Sven Efftinge - Initial contribution and API
 *
 */
public interface IParseErrorHandler {
	public void handleParserError(LeafNode skippedNodes, String message, Object context);
}
