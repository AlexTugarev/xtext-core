/*
 * generated by Xtext
 */
grammar DebugInternalBug305397 ;

// Rule Model
ruleModel :
	ruleElement*
;

// Rule Element
ruleElement :
	'a'? 'element' RULE_ID? ruleElement* 'end'
;

RULE_ID :
	'^'? (
		'a' .. 'z' |
		'A' .. 'Z' |
		'_'
	) (
		'a' .. 'z' |
		'A' .. 'Z' |
		'_' |
		'0' .. '9'
	)*
;

RULE_INT :
	'0' .. '9'+
;

RULE_STRING :
	'"' (
		'\\' . |
		~ (
			'\\' |
			'"'
		)
	)* '"' |
	'\'' (
		'\\' . |
		~ (
			'\\' |
			'\''
		)
	)* '\''
;

RULE_ML_COMMENT :
	'/*' (
		options { greedy = false ; } : .
	)* '*/' { skip(); }
;

RULE_SL_COMMENT :
	'//' ~ (
		'\n' |
		'\r'
	)* (
		'\r'? '\n'
	)? { skip(); }
;

RULE_WS :
	(
		' ' |
		'\t' |
		'\r' |
		'\n'
	)+ { skip(); }
;

RULE_ANY_OTHER :
	.
;