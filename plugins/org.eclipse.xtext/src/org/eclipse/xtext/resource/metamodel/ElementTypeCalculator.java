/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.resource.metamodel;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.TypeRef;
import org.eclipse.xtext.util.Function;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.util.XtextSwitch;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
final class ElementTypeCalculator extends XtextSwitch<EClassifier> implements Function<AbstractElement, EClassifier>{

	private final EClassifierInfos classifierInfos;

	/**
	 * @param classifierInfos
	 */
	ElementTypeCalculator(EClassifierInfos classifierInfos) {
		this.classifierInfos = classifierInfos;
	}

	@Override
	public EClassifier caseKeyword(Keyword object) {
		return EcorePackage.Literals.ESTRING;
	}

	@Override
	public EClassifier caseTypeRef(TypeRef object) {
		if (object.getType() == null) {
			if (object.getMetamodel() == null || object.getMetamodel().getEPackage() == null)
				return null;
			String name = GrammarUtil.getTypeRefName(object);
			if (Strings.isEmpty(name))
				return null;
			EClassifierInfo info = classifierInfos.getInfo(object.getMetamodel(), name);
			if (info != null)
				object.setType(info.getEClassifier());
		}
		return object.getType();
	}

	@Override
	public EClassifier caseAbstractRule(AbstractRule object) {
		if (object.getType() != null)
			return doSwitch(object.getType());
		return null;
	}

	@Override
	public EClassifier caseGroup(Group object) {
		//TODO Too strict. What about : foo(Bar|Foo ';')
		if (object.getAbstractTokens().size() != 1)
			return null;
		return doSwitch(object.getAbstractTokens().get(0));
	}

	@Override
	public EClassifier caseRuleCall(RuleCall ruleCall) {
		if (ruleCall.getRule() != null)
			return doSwitch(ruleCall.getRule());
		return null;
	}

	@Override
	public EClassifier caseAlternatives(Alternatives object) {
		final Set<EClassifier> types = new HashSet<EClassifier>();
		for (AbstractElement group : object.getGroups())
			types.add(doSwitch(group));
		try {
			return classifierInfos.getCompatibleTypeNameOf(types, true);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public EClassifier caseCrossReference(CrossReference object) {
		return doSwitch(object.getType());
	}

	public EClassifier exec(AbstractElement param) {
		return doSwitch(param);
	}
}