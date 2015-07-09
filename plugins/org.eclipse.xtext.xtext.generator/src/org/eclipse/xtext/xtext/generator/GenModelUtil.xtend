/** 
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator

import org.eclipse.emf.codegen.ecore.genmodel.GenClass
import org.eclipse.emf.codegen.ecore.genmodel.GenClassifier
import org.eclipse.emf.codegen.ecore.genmodel.GenDataType
import org.eclipse.emf.codegen.ecore.genmodel.GenEnum
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature
import org.eclipse.emf.codegen.ecore.genmodel.GenModel
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.plugin.EcorePlugin
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet

class GenModelUtil {
	
	def static GenClass getGenClass(EClass cls, ResourceSet resourceSet) {
		return getGenClassifier(cls, resourceSet) as GenClass
	}

	def static GenClassifier getGenClassifier(EClassifier cls, ResourceSet resourceSet) {
		val genPackage = getGenPackage(cls.EPackage, resourceSet)
		for (genCls : genPackage.genClassifiers) {
			if (cls.name == genCls.ecoreClassifier.name) {
				return genCls
			}
		}
		throw new RuntimeException('''No GenClassifier named '�cls.name�' found in GenModel �genPackage.eResource.URI�''')
	}

	def static GenDataType getGenDataType(EDataType dt, ResourceSet resourceSet) {
		return getGenClassifier(dt, resourceSet) as GenDataType
	}

	def static GenEnum getGenEnum(EEnum en, ResourceSet resourceSet) {
		return getGenClassifier(en, resourceSet) as GenEnum
	}

	def static GenFeature getGenFeature(EStructuralFeature feature, ResourceSet resourceSet) {
		val genCls = getGenClassifier(feature.EContainingClass, resourceSet) as GenClass
		for (genFeat : genCls.genFeatures) {
			if (feature.name == genFeat.ecoreFeature.name) {
				return genFeat
			}
		}
		throw new RuntimeException('''No GenFeature named '�feature.name�' found in GenClass '�genCls�' from GenModel�genCls.eResource.URI�''')
	}

	def static String getGenIntLiteral(EClass clazz, EStructuralFeature feature, ResourceSet resourceSet) {
		val genFeature = getGenFeature(feature, resourceSet)
		val genClass = getGenClass(clazz, resourceSet)
		return genClass.genPackage.packageInterfaceName + '.' + genClass.getFeatureID(genFeature)
	}

	def static String getGenIntLiteral(EClassifier classifier, ResourceSet resourceSet) {
		val genClassifier = getGenClassifier(classifier, resourceSet)
		return genClassifier.genPackage.packageInterfaceName + '.' + genClassifier.classifierID
	}

	def static GenPackage getGenPackage(EPackage pkg, ResourceSet resourceSet) {
		val nsURI = pkg.nsURI
		var String location
		if (pkg.eResource?.URI !== null)
			location = pkg.eResource.URI.toString
		val genModelResource = getGenModelResource(location, nsURI, resourceSet)
		if (genModelResource !== null) {
			for (model : genModelResource.contents) {
				if (model instanceof GenModel) {
					val genPkg = model.findGenPackage(pkg)
					if (genPkg !== null) {
						genPkg.getEcorePackage.getEClassifiers()
						return genPkg
					}
				}
			}
			throw new RuntimeException('''No GenPackage for NsURI �nsURI� found in �genModelResource.URI�''')
		}
		throw new RuntimeException('''No GenPackage for NsURI �nsURI�.''')
	}

	def static Resource getGenModelResource(String locationInfo, String nsURI, ResourceSet resourceSet) {
		val genModelURI = EcorePlugin.getEPackageNsURIToGenModelLocationMap(false).get(nsURI)
		if (genModelURI === null) {
			if (EcorePackage.eNS_URI.equals(nsURI)) // If we really want to use the registered ecore ...
				return null // look into the resource set to find a genpackage for the given URI
			for (res : resourceSet.resources) {
				// We only look into the first level, as genmodels are usually among the top level elements.
				// This is just to avoid traversing all eobjects in the resource set.
				for (obj : res.contents) {
					if (obj instanceof GenModel) {
						for (genPackage : obj.genPackages) {
							if (genPackage.NSURI.equals(nsURI)) {
								return genPackage.eResource
							}
						}
					}
				}
			}
			val buf = new StringBuilder
			var loc = locationInfo
			if (loc !== null && loc.length > 0)
				loc = ' from ' + loc
			else
				loc = ''
			buf.append("Could not find a GenModel for EPackage '").append(nsURI).append("'").append(loc).append("\n")
			// TODO replace with references to new fragments
			buf.append('''If the missing GenModel has been generated via EMFGeneratorFragment.class.getSimpleName() or org.eclipse.xtext.generator.ecore.EcoreGeneratorFragment.class.getSimpleName()''')
			buf.append(" make sure to run it first in the workflow.\n")
			buf.append('''If you have a *.genmodel-file, make sure to register it via StandaloneSetup.registerGenModelFile(String)''')
			throw new RuntimeException(buf.toString)
		}
		if (resourceSet === null)
			throw new RuntimeException('''There is no ResourceSet for EPackage '�nsURI�'. Please make sure the EPackage has been loaded from a .ecore file and not from the generated Java file.''')
		val genModelResource = resourceSet.getResource(genModelURI, true)
		if (genModelResource === null)
			throw new RuntimeException('''Error loading GenModel �genModelURI�''')
		for (EObject content : genModelResource.contents) {
			if (content instanceof GenModel)
				content.reconcile()
		}
		return genModelResource
	}

	def static String getGenTypeLiteral(EClassifier classifier, ResourceSet resourceSet) {
		// inspired by org.eclipse.emf.codegen.ecore.genmodel.impl.GenClassifierImpl.getQualifiedClassifierAccessor()
		val genClassifier = getGenClassifier(classifier, resourceSet)
		var String pkg = genClassifier.genPackage.packageInterfaceName
		if (genClassifier.genPackage.isLiteralsInterface)
			return pkg + '.Literals.' + genClassifier.classifierID
		else
			return pkg + '.eINSTANCE.get' + genClassifier.classifierAccessorName + '()'
	}

	def static String getGenTypeLiteral(EPackage pkg, ResourceSet resourceSet) {
		return getGenPackage(pkg, resourceSet).packageInterfaceName + '.eINSTANCE'
	}

	def static String getGenTypeLiteral(EStructuralFeature feature, ResourceSet resourceSet) {
		// inspired by org.eclipse.emf.codegen.ecore.genmodel.impl.GenFeatureImpl.getQualifiedFeatureAccessor()
		val genFeature = getGenFeature(feature, resourceSet)
		val pkg = genFeature.genPackage.packageInterfaceName
		if (genFeature.genPackage.isLiteralsInterface)
			return pkg + '.Literals.' + genFeature.genClass.getFeatureID(genFeature)
		else
			return pkg + '.eINSTANCE.get' + genFeature.featureAccessorName + '()'
	}

	def static String getJavaTypeName(EClassifier classifier, ResourceSet resourceSet) {
		val genClassifier = getGenClassifier(classifier, resourceSet)
		if (genClassifier instanceof GenClass)
			return genClassifier.qualifiedInterfaceName
		else
			return (genClassifier as GenDataType).qualifiedInstanceClassName
	}

}
