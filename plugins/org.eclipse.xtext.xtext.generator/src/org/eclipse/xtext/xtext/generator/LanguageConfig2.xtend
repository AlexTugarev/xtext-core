/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext.generator

import com.google.common.base.Joiner
import com.google.common.collect.Lists
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Provider
import java.util.Collections
import java.util.HashMap
import java.util.List
import org.eclipse.emf.common.util.Diagnostic
import org.eclipse.emf.common.util.DiagnosticChain
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.common.util.WrappedException
import org.eclipse.emf.ecore.EValidator
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.GrammarUtil
import org.eclipse.xtext.ReferencedMetamodel
import org.eclipse.xtext.XtextPackage
import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.resource.impl.ResourceDescriptionsData
import org.eclipse.xtext.util.internal.Log
import org.eclipse.xtext.xtext.generator.model.GuiceModuleAccess
import org.eclipse.xtext.xtext.generator.model.JavaFileAccess

@Log
class LanguageConfig2 extends CompositeGeneratorFragment2 {
	
	@Inject Provider<ResourceSet> resourceSetProvider
	
	@Inject XtextGeneratorTemplates generatorTemplates
	
	@Accessors
	String uri
	
	@Accessors(PUBLIC_GETTER)
	Grammar grammar
	
	List<String> fileExtensions
	
	@Accessors
	val List<String> loadedResources = newArrayList
	
	JavaFileAccess runtimeSetupImpl
	
	GuiceModuleAccess runtimeModule
	
	GuiceModuleAccess eclipsePluginModule
	
	def void setFileExtensions(String fileExtensions) {
		this.fileExtensions = fileExtensions.trim.split("\\s*,\\s*").toList
	}
	
	def List<String> getFileExtensions() {
		if (fileExtensions === null || fileExtensions.empty) {
			val lowerCase = GrammarUtil.getName(grammar).toLowerCase
			if (LOG.infoEnabled)
				LOG.info("No explicit fileExtensions configured. Using '*." + lowerCase + "'.")
			return Collections.singletonList(lowerCase)
		}
		return fileExtensions
	}
	
	def void addLoadedResource(String uri) {
		this.loadedResources.add(uri)
	}
	
	def JavaFileAccess getRuntimeSetup() {
		if (runtimeSetupImpl === null)
			runtimeSetupImpl = generatorTemplates.startRuntimeGenSetup(this)
		return runtimeSetupImpl
	}
	
	def GuiceModuleAccess getRuntimeModule() {
		if (runtimeModule == null)
			runtimeModule = generatorTemplates.startRuntimeGenModule(this)
		return runtimeModule
	}
	
	def GuiceModuleAccess getEclipsePluginModule() {
		if (eclipsePluginModule == null)
			eclipsePluginModule = generatorTemplates.startEclipsePluginGenModule(this)
		return eclipsePluginModule
	}
	
	override initialize(Injector injector) {
		super.initialize(injector)
		val rs = resourceSetProvider.get()
		for (String loadedResource : loadedResources) {
			val loadedResourceUri = URI.createURI(loadedResource)
			switch (loadedResourceUri.fileExtension) {
				case 'genmodel': {
					val resourceServiceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri)
					if (resourceServiceProvider === null) {
						try {
							val genModelSupport = Class.forName('org.eclipse.emf.codegen.ecore.xtext.GenModelSupport')
							val instance = genModelSupport.newInstance()
							genModelSupport.getDeclaredMethod('createInjectorAndDoEMFRegistration').invoke(instance)
						} catch (ClassNotFoundException e) {
							LOG.error("Couldn't initialize GenModel support. Is it on the classpath?")
							LOG.debug(e.getMessage(), e)
						} catch (Exception e) {
							LOG.error("Couldn't initialize GenModel support.", e)
						}
					}
				}
				case 'ecore': {
					val resourceServiceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri)
					if (resourceServiceProvider === null) {
						EcoreSupportStandaloneSetup.setup()
					}
				}
				case 'xcore': {
					val resourceServiceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri)
					if (resourceServiceProvider === null) {
						try {
							val xcore = Class.forName('org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup')
							xcore.getDeclaredMethod('doSetup', #[]).invoke(null)
						} catch (ClassNotFoundException e) {
							LOG.error("Couldn't initialize Xcore support. Is it on the classpath?")
							LOG.debug(e.getMessage(), e)
						} catch (Exception e) {
							LOG.error("Couldn't initialize Xcore support.", e)
						}
					}
					val xcoreLangURI = URI.createPlatformResourceURI('/org.eclipse.emf.ecore.xcore.lib/model/XcoreLang.xcore', true)
					try {
						rs.getResource(xcoreLangURI, true)
					} catch (WrappedException e) {
						LOG.error("Could not load XcoreLang.xcore.", e)
						val brokenResource = rs.getResource(xcoreLangURI, false)
						rs.resources.remove(brokenResource)
					}
				}
			}
			rs.getResource(loadedResourceUri, true)
		}
		if (!rs.resources.isEmpty) {
			installIndex(rs)
			for (var i = 0, var size = rs.resources.size; i < size; i++) {
				val res = rs.resources.get(i)
				if (res.getContents().isEmpty())
					LOG.error("Error loading '" + res.getURI() + "'")
				else if (!res.getErrors().isEmpty())
					LOG.error("Error loading '" + res.getURI() + "':\n" + Joiner.on('\n').join(res.getErrors()))	
			}
			EcoreUtil.resolveAll(rs)
		}
		val resource = rs.getResource(URI.createURI(uri), true) as XtextResource
		if (resource.contents.isEmpty) {
			throw new IllegalArgumentException("Couldn't load grammar for '" + uri + "'.")
		}
		if (!resource.errors.isEmpty) {
			LOG.error(resource.errors)
			throw new IllegalStateException("Problem parsing '" + uri + "':\n" + Joiner.on('\n').join(resource.getErrors()))
		}

		val grammar = resource.getContents().get(0) as Grammar
		validateGrammar(grammar)
		this.grammar = grammar
	}
	
	private def void installIndex(ResourceSet resourceSet) {
		val index = new ResourceDescriptionsData(Collections.emptyList)
		val resources = Lists.newArrayList(resourceSet.resources)
		for (resource : resources) {
			index(resource, resource.URI, index)
		}
		ResourceDescriptionsData.ResourceSetAdapter.installResourceDescriptionsData(resourceSet, index)
	}

	private def void index(Resource resource, URI uri, ResourceDescriptionsData index) {
		val serviceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uri)
		if (serviceProvider != null) {
			val resourceDescription = serviceProvider.resourceDescriptionManager.getResourceDescription(resource)
			if (resourceDescription != null) {
				index.addDescription(uri, resourceDescription)
			}
		}
	}
	
	protected def void validateGrammar(Grammar grammar) {
		validateAllImports(grammar)
		val validator = EValidator.Registry.INSTANCE.getEValidator(XtextPackage.eINSTANCE)
		if (validator !== null) {
			val chain = new DiagnosticChain() {

				override add(Diagnostic diagnostic) {
					if (diagnostic.severity == Diagnostic.ERROR) {
						if (diagnostic.exception == null)
							throw new IllegalStateException(diagnostic.message)
						else
							throw new IllegalStateException(diagnostic.message, diagnostic.exception)
					}
				}

				override addAll(Diagnostic diagnostic) {
					add(diagnostic)
				}

				override merge(Diagnostic diagnostic) {
					throw new UnsupportedOperationException
				}
			}
			validator.validate(grammar, chain, null)
			val iterator = grammar.eAllContents
			while (iterator.hasNext) {
				validator.validate(iterator.next, chain, new HashMap);
			}
		}
	}
	
	protected def void validateAllImports(Grammar grammar) {
		for (amd : GrammarUtil.allMetamodelDeclarations(grammar)) {
			if (amd instanceof ReferencedMetamodel)
				validateReferencedMetamodel(amd)
		}
	}

	protected def void validateReferencedMetamodel(ReferencedMetamodel ref) {
		if (ref.EPackage != null && !ref.EPackage.eIsProxy) {
			return
		}
		val eref = XtextPackage.Literals.ABSTRACT_METAMODEL_DECLARATION__EPACKAGE
		val nodes = NodeModelUtils.findNodesForFeature(ref, eref)
		val refName = if (nodes.empty) '(unknown)' else NodeModelUtils.getTokenText(nodes.get(0))
		val grammarName = GrammarUtil.getGrammar(ref).name
		val msg = "The EPackage " + refName + " in grammar " + grammarName + " could not be found. "
			+ "You might want to register that EPackage in your workflow file."
		throw new IllegalStateException(msg)
	}
	
}