/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.scoping.impl;

import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopedElement;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class SimpleScope extends AbstractScope {
	
	private final IScope outer;

	private final Iterable<IScopedElement> elements;

	public SimpleScope(final IScope outer, final Iterable<IScopedElement> elements) {
		this.outer = outer;
		this.elements = elements;
	}
	
	public SimpleScope(final Iterable<IScopedElement> elements) {
		this(IScope.NULLSCOPE, elements);
	}

	public IScope getOuterScope() {
		return outer;
	}

	public Iterable<IScopedElement> getContents() {
		return elements;
	}

}
