/*
 * generated by Xtext
 */
package org.eclipse.xtext.linking.lazy.serializer;

import com.google.inject.Inject;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Parameter;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.linking.lazy.lazyLinking.LazyLinkingPackage;
import org.eclipse.xtext.linking.lazy.lazyLinking.Model;
import org.eclipse.xtext.linking.lazy.lazyLinking.Property;
import org.eclipse.xtext.linking.lazy.lazyLinking.Type;
import org.eclipse.xtext.linking.lazy.lazyLinking.UnresolvedProxyProperty;
import org.eclipse.xtext.linking.lazy.services.LazyLinkingTestLanguageGrammarAccess;
import org.eclipse.xtext.serializer.ISerializationContext;
import org.eclipse.xtext.serializer.sequencer.AbstractDelegatingSemanticSequencer;

@SuppressWarnings("all")
public class LazyLinkingTestLanguageSemanticSequencer extends AbstractDelegatingSemanticSequencer {

	@Inject
	private LazyLinkingTestLanguageGrammarAccess grammarAccess;
	
	@Override
	public void sequence(ISerializationContext context, EObject semanticObject) {
		EPackage epackage = semanticObject.eClass().getEPackage();
		ParserRule rule = context.getParserRule();
		Action action = context.getAssignedAction();
		Set<Parameter> parameters = context.getEnabledBooleanParameters();
		if (epackage == LazyLinkingPackage.eINSTANCE)
			switch (semanticObject.eClass().getClassifierID()) {
			case LazyLinkingPackage.MODEL:
				sequence_Model(context, (Model) semanticObject); 
				return; 
			case LazyLinkingPackage.PROPERTY:
				sequence_Property(context, (Property) semanticObject); 
				return; 
			case LazyLinkingPackage.TYPE:
				sequence_Type(context, (Type) semanticObject); 
				return; 
			case LazyLinkingPackage.UNRESOLVED_PROXY_PROPERTY:
				sequence_UnresolvedProxyProperty(context, (UnresolvedProxyProperty) semanticObject); 
				return; 
			}
		if (errorAcceptor != null)
			errorAcceptor.accept(diagnosticProvider.createInvalidContextOrTypeDiagnostic(semanticObject, context));
	}
	
	/**
	 * Contexts:
	 *     Model returns Model
	 *
	 * Constraint:
	 *     types+=Type+
	 */
	protected void sequence_Model(ISerializationContext context, Model semanticObject) {
		genericSequencer.createSequence(context, semanticObject);
	}
	
	
	/**
	 * Contexts:
	 *     Property returns Property
	 *
	 * Constraint:
	 *     (type+=[Type|ID]+ name=ID)
	 */
	protected void sequence_Property(ISerializationContext context, Property semanticObject) {
		genericSequencer.createSequence(context, semanticObject);
	}
	
	
	/**
	 * Contexts:
	 *     Type returns Type
	 *
	 * Constraint:
	 *     (
	 *         name=ID 
	 *         (extends=[Type|ID] parentId=[Property|ID])? 
	 *         (parentId=[Property|ID] extends=[Type|ID])? 
	 *         properties+=Property* 
	 *         unresolvedProxyProperty+=UnresolvedProxyProperty*
	 *     )
	 */
	protected void sequence_Type(ISerializationContext context, Type semanticObject) {
		genericSequencer.createSequence(context, semanticObject);
	}
	
	
	/**
	 * Contexts:
	 *     UnresolvedProxyProperty returns UnresolvedProxyProperty
	 *
	 * Constraint:
	 *     (type+=[Type|ID]+ name=ID)
	 */
	protected void sequence_UnresolvedProxyProperty(ISerializationContext context, UnresolvedProxyProperty semanticObject) {
		genericSequencer.createSequence(context, semanticObject);
	}
	
	
}
