/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.index;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.index.EObjectDescriptor;
import org.eclipse.emf.index.IndexStore;
import org.eclipse.xtext.linking.impl.SimpleAttributeResolver;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopedElement;
import org.eclipse.xtext.scoping.impl.AbstractScopeProvider;
import org.eclipse.xtext.scoping.impl.ScopedElement;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.util.Strings;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.Inject;

/**
 * @author Sven Efftinge - Initial contribution and API
 * 
 */
public class DefaultIndexBasedScopeProvider extends AbstractScopeProvider {

	/**
	 * @author Sven Efftinge - Initial contribution and API
	 *
	 */
	private final class DefaultImportNormalizer implements Function<String, String> {
		private final List<String> elements;

		private DefaultImportNormalizer(List<String> elements) {
			this.elements = elements;
		}

		public String apply(String from) {
			List<String> split = Strings.split(from, nameProvider.getDelimiter());
			if (split.size()>=elements.size()) {
				Iterator<String> i1 = elements.iterator(), i2 = split.iterator();
				boolean proceed = i1.hasNext() && i2.hasNext();
				while (proceed) {
					String s1 = i1.next();
					String s2 = i2.next();
					if (!s1.equals(s2)) {
						if (!s1.equals(nameProvider.getWildcard())) {
							break;
						} else {
							StringBuffer result = new StringBuffer(s2);
							while (i2.hasNext())
								result.append(i2.next());
							return result.toString();
						}
					} else if (!i1.hasNext() && !i2.hasNext()) {
						return s2;
					}
				}
			}
			return null;
		}
	}

	@Inject
	private IQualifiedNameProvider nameProvider;

	public void setNameProvider(IQualifiedNameProvider nameProvider) {
		this.nameProvider = nameProvider;
	}
	
	@Inject
	private IndexStore indexStore;
	
	public void setIndexStore(IndexStore indexStore) {
		this.indexStore = indexStore;
	}

	public IScope getScope(EObject context, EReference reference) {
		return getScope(context, reference.getEReferenceType());
	}

	public IScope getScope(final EObject context, final EClass type) {
		if (context == null)
			return getGlobalScope(type);
		
		IScope parent = getScope(context.eContainer(), type);
		SimpleScope importScope = new SimpleScope(parent, getImportedElements(context, type));
		return new SimpleScope(importScope, getLocalElements(context, type));
	}

	@SuppressWarnings("unchecked")
	private Iterable<IScopedElement> getLocalElements(final EObject context, final EClass type) {
		final String commonPrefix = nameProvider.getQualifiedName(context)+nameProvider.getDelimiter();
		Iterable<? extends EObject> contents = new Iterable<? extends EObject>() {

			public Iterator<? extends EObject> iterator() {
				return (Iterator<? extends EObject>)EcoreUtil.getAllProperContents(context, true);
			}
			
		};
		// filter by type
		contents = filter(contents, new Predicate<EObject>(){

			public boolean apply(EObject input) {
				return type.isInstance(input);
			}
		});
		// transform to IScopedElements
		Iterable<IScopedElement> scopedElements = transform(contents, new Function<EObject, IScopedElement>(){

			public IScopedElement apply(EObject from) {
				String name = nameProvider.getQualifiedName(from);
				if (name!=null && name.startsWith(commonPrefix))
					return ScopedElement.create(name.substring(commonPrefix.length()), from);
				return null;
			}

		});
		// filter null values;
		return filter(scopedElements, Predicates.notNull());
	}

	protected Set<Function<String, String>> getImportNormalizer(EObject context) {
		Set<Function<String, String>> namespaceImports = new HashSet<Function<String, String>>();
		SimpleAttributeResolver<EObject, String> importResolver = SimpleAttributeResolver.newResolver(String.class, "importedNamespace");
		for (EObject child : context.eContents()) {
			String value = importResolver.getValue(child);
			if (value != null) {
				final List<String> elements = org.eclipse.xtext.util.Strings.split(value,nameProvider.getDelimiter());
				if (elements.get(elements.size()-1).equals(nameProvider.getWildcard())) {
					namespaceImports.add(new DefaultImportNormalizer(elements));
				}
			}
		}
		return namespaceImports;
	}

	protected Iterable<IScopedElement> getImportedElements(final EObject context, final EClass type) {
		final Set<Function<String,String>> normalizers = getImportNormalizer(context);
		Iterable<EObjectDescriptor> result = new Iterable<EObjectDescriptor>() {

			public Iterator<EObjectDescriptor> iterator() {
				return indexStore.eObjectDAO().createQueryEObjectsByType(type).executeListResult().iterator();
			}
		};
		
		Iterable<IScopedElement> transformed = transform(result,new Function<EObjectDescriptor, IScopedElement>(){

			public IScopedElement apply(EObjectDescriptor from) {
				for (Function<String,String> normalizer : normalizers) {
					String normalizedName = normalizer.apply(from.getName());
					if (normalizedName!=null) {
						return ScopedElement.create(normalizedName, createProxy(from));
					}
				}
				return null;
			}
		});
		return filter(transformed, Predicates.notNull());
	}

	protected EObject createProxy(EObjectDescriptor from) {
		EClass eClass = from.getEClassDescriptor().getEClass();
		EObject proxy = eClass.getEPackage().getEFactoryInstance().create(eClass);
		((InternalEObject) proxy).eSetProxyURI(from.getFragmentURI());
		return proxy;
	}

	protected IScope getGlobalScope(final EClass type) {
		Iterable<EObjectDescriptor> result = indexStore.eObjectDAO().createQueryEObjectsByType(type).executeListResult();
		return new SimpleScope(transform(result, new Function<EObjectDescriptor, IScopedElement>() {
			public IScopedElement apply(EObjectDescriptor from) {
				return new IndexBasedScopedElement(from);
			}
		}));
	}

}
