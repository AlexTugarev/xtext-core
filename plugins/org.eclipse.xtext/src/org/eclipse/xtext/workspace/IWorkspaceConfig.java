/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.workspace;

import org.eclipse.emf.common.util.URI;

public interface IWorkspaceConfig {
	/**
	 * @return the project whose source folders physically contain this member or null if none was found
	 */
	IProjectConfig findProjectContaining(URI member);

	/**
	 * @return the project with the given name or null if none was found
	 */
	IProjectConfig findProjectByName(String name);
}
