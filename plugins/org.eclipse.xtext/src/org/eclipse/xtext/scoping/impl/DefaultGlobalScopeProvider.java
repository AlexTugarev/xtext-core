/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.scoping.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescription.Event.Source;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.containers.FilterUriContainer;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.util.OnChangeEvictingCache;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class DefaultGlobalScopeProvider extends AbstractGlobalScopeProvider {

	@Inject
	private IContainer.Manager containerManager;

	@Inject
	private IResourceDescription.Manager descriptionManager;
	
	public IScope getScope(final EObject context, EReference reference) {
		IScope result = IScope.NULLSCOPE;
		List<IContainer> containers = Lists.newArrayList(getVisibleContainers(context));
		Collections.reverse(containers);
		Iterator<IContainer> iter = containers.iterator();
		while (iter.hasNext()) {
			IContainer container = iter.next();
			result = createContainerScopeWithContext(context, result, container, reference);
		}
		return result;
	}

	protected List<IContainer> getVisibleContainers(EObject context) {
		IResourceDescription description = descriptionManager.getResourceDescription(context.eResource());
		IResourceDescriptions resourceDescriptions = getResourceDescriptions(context);
		String cacheKey = getCacheKey("VisibleContainers", context.eResource().getResourceSet());
		OnChangeEvictingCache.CacheAdapter cache = new OnChangeEvictingCache().getOrCreate(context);
		List<IContainer> result = null;
		result = cache.get(cacheKey);
		if (result == null) {
			result = containerManager.getVisibleContainers(description,	resourceDescriptions);
			// SZ: I'ld like this dependency to be moved to the implementation of the
			// container manager, but it is not aware of a CacheAdapter
			if (resourceDescriptions instanceof IResourceDescription.Event.Source) {
				IResourceDescription.Event.Source eventSource = (Source) resourceDescriptions;
				DelegatingEventSource delegatingEventSource = new DelegatingEventSource(eventSource);
				delegatingEventSource.addListeners(Lists.newArrayList(Iterables.filter(result, IResourceDescription.Event.Listener.class)));
				delegatingEventSource.initialize();
				cache.addCacheListener(delegatingEventSource);
			}
			cache.set(cacheKey, result);
		}
		return result;
	}
	
	protected String getCacheKey(String base, ResourceSet context) {
		Map<Object, Object> loadOptions = context.getLoadOptions();
		if (loadOptions.containsKey(NAMED_BUILDER_SCOPE)) {
			return base + "@" + NAMED_BUILDER_SCOPE;
		} 
		return base + "@DEFAULT_SCOPE"; 
	}

	protected IScope createContainerScopeWithContext(EObject context, IScope result, IContainer container,
			EReference reference) {
		Resource eResource = context.eResource();
		if (eResource != null) {
			URI uriToFilter = eResource.getURI();
			if (container.getResourceDescription(uriToFilter) != null)
				container = new FilterUriContainer(uriToFilter, container);
		}
		return createContainerScope(result, container, reference);
	}

	protected IScope createContainerScope(IScope parent, IContainer container, EReference reference) {
		Iterable<IResourceDescription> content = container.getResourceDescriptions();
		if (Iterables.isEmpty(content))
			return parent;
		return new ContainerBasedScope(parent, reference, container);
	}

}
