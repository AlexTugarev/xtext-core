/*
 * generated by Xtext
 */
grammar InternalPartialSerializationTestLanguage;

options {
	superClass=AbstractInternalAntlrParser;
	
}

@lexer::header {
package org.eclipse.xtext.parsetree.reconstr.parser.antlr.internal;

// Hack: Use our own Lexer superclass by means of import. 
// Currently there is no other way to specify the superclass for the lexer.
import org.eclipse.xtext.parser.antlr.Lexer;
}

@parser::header {
package org.eclipse.xtext.parsetree.reconstr.parser.antlr.internal; 

import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.AbstractInternalAntlrParser;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.parser.antlr.AntlrDatatypeRuleToken;
import org.eclipse.xtext.parsetree.reconstr.services.PartialSerializationTestLanguageGrammarAccess;

}

@parser::members {

 	private PartialSerializationTestLanguageGrammarAccess grammarAccess;
 	
    public InternalPartialSerializationTestLanguageParser(TokenStream input, PartialSerializationTestLanguageGrammarAccess grammarAccess) {
        this(input);
        this.grammarAccess = grammarAccess;
        registerRules(grammarAccess.getGrammar());
    }
    
    @Override
    protected String getFirstRuleName() {
    	return "Model";	
   	}
   	
   	@Override
   	protected PartialSerializationTestLanguageGrammarAccess getGrammarAccess() {
   		return grammarAccess;
   	}
}

@rulecatch { 
    catch (RecognitionException re) { 
        recover(input,re); 
        appendSkippedTokens();
    } 
}




// Entry rule entryRuleModel
entryRuleModel returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getModelRule()); }
	 iv_ruleModel=ruleModel 
	 { $current=$iv_ruleModel.current; } 
	 EOF 
;

// Rule Model
ruleModel returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:

    { 
        newCompositeNode(grammarAccess.getModelAccess().getNodeRootParserRuleCall()); 
    }
    this_NodeRoot_0=ruleNodeRoot
    { 
        $current = $this_NodeRoot_0.current; 
        afterParserOrEnumRuleCall();
    }

;





// Entry rule entryRuleNodeRoot
entryRuleNodeRoot returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getNodeRootRule()); }
	 iv_ruleNodeRoot=ruleNodeRoot 
	 { $current=$iv_ruleNodeRoot.current; } 
	 EOF 
;

// Rule NodeRoot
ruleNodeRoot returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(	otherlv_0='#1' 
    {
    	newLeafNode(otherlv_0, grammarAccess.getNodeRootAccess().getNumberSignDigitOneKeyword_0());
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getNodeRootAccess().getNodeNodeParserRuleCall_1_0()); 
	    }
		lv_node_1_0=ruleNode		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getNodeRootRule());
	        }
       		set(
       			$current, 
       			"node",
        		lv_node_1_0, 
        		"org.eclipse.xtext.parsetree.reconstr.PartialSerializationTestLanguage.Node");
	        afterParserOrEnumRuleCall();
	    }

)
))
;





// Entry rule entryRuleNode
entryRuleNode returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getNodeRule()); }
	 iv_ruleNode=ruleNode 
	 { $current=$iv_ruleNode.current; } 
	 EOF 
;

// Rule Node
ruleNode returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(	otherlv_0='node' 
    {
    	newLeafNode(otherlv_0, grammarAccess.getNodeAccess().getNodeKeyword_0());
    }
(
(
		lv_name_1_0=RULE_ID
		{
			newLeafNode(lv_name_1_0, grammarAccess.getNodeAccess().getNameIDTerminalRuleCall_1_0()); 
		}
		{
	        if ($current==null) {
	            $current = createModelElement(grammarAccess.getNodeRule());
	        }
       		setWithLastConsumed(
       			$current, 
       			"name",
        		lv_name_1_0, 
        		"org.eclipse.xtext.common.Terminals.ID");
	    }

)
)(	otherlv_2='(' 
    {
    	newLeafNode(otherlv_2, grammarAccess.getNodeAccess().getLeftParenthesisKeyword_2_0());
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getNodeAccess().getChildrenNodeParserRuleCall_2_1_0()); 
	    }
		lv_children_3_0=ruleNode		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getNodeRule());
	        }
       		add(
       			$current, 
       			"children",
        		lv_children_3_0, 
        		"org.eclipse.xtext.parsetree.reconstr.PartialSerializationTestLanguage.Node");
	        afterParserOrEnumRuleCall();
	    }

)
)+	otherlv_4=')' 
    {
    	newLeafNode(otherlv_4, grammarAccess.getNodeAccess().getRightParenthesisKeyword_2_2());
    }
)?)
;





RULE_ID : '^'? ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;

RULE_INT : ('0'..'9')+;

RULE_STRING : ('"' ('\\' .|~(('\\'|'"')))* '"'|'\'' ('\\' .|~(('\\'|'\'')))* '\'');

RULE_ML_COMMENT : '/*' ( options {greedy=false;} : . )*'*/';

RULE_SL_COMMENT : '//' ~(('\n'|'\r'))* ('\r'? '\n')?;

RULE_WS : (' '|'\t'|'\r'|'\n')+;

RULE_ANY_OTHER : .;


