/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.crossref;

import org.eclipse.emf.ecore.EObject;

/**
 * Represents an element of an {@link IScope}.
 * @author Sven Efftinge - Initial contribution and API
 */
public interface IScopedElement {
	
	/**
	 * @return the name, this element can be accessed within its scope. 
	 */
	String name();
	
	/**
	 * @return the actual element, this element is referring to.
	 */
	EObject element();
	
	/**
	 * Can be used to associate additional information.
	 * @return optional additional information. 
	 */
	Object additionalInformation();
}
