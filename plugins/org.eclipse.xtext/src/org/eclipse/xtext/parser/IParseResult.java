/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parser;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.CompositeNode;

/**
 * Represents the result of the parsing. 
 * Via the ParseResult the semantic root AST element,
 * the root node model element and the list of syntax errors can be retrieved.
 */
public interface IParseResult {

    EObject getRootASTElement();

    @Deprecated
    CompositeNode getRootNode();
    
    ICompositeNode getRootNode2();
    
    @Deprecated
    List<AbstractNode> getParseErrors();
    
    Iterable<INode> getSyntaxErrors();
    
    boolean hasSyntaxErrors();
}
