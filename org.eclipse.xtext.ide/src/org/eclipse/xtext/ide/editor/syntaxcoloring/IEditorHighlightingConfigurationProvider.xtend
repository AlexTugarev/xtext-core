/*******************************************************************************
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ide.editor.syntaxcoloring

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import org.eclipse.xtext.GrammarUtil
import org.eclipse.xtext.IGrammarAccess
import org.eclipse.xtext.LanguageInfo

/**
 * @author Sven Efftinge - Initial contribution and API
 * @since 2.11
 */
@ImplementedBy(DefaultImpl)
interface IEditorHighlightingConfigurationProvider {

    /**
     * provides an editor specific highlighting configuration or null.
     */
    def String getConfiguration(String editorName)

    static class DefaultImpl implements IEditorHighlightingConfigurationProvider {

        @Inject IGrammarAccess grammarAccess
        @Inject LanguageInfo languageInfo

        override getConfiguration(String editorName) {
            if (editorName == 'EclipseChe' || editorName == 'EclipseOrion') {
                return '''
                    [
                      «getStandardPatterns»
                      {
                        match: "\\b(?:«keywords.join('|')»)\\b", 
                        name: "keyword.«languageInfo.shortName»" 
                      }
                    ]
                '''
            } else {
                return '''
                '''
            }
        }

        def getGetStandardPatterns() '''
            {include: "orion.c-like#comment_singleLine"},
            {include: "orion.c-like#comment_block"},
            {include: "orion.lib#string_doubleQuote"},
            {include: "orion.lib#string_singleQuote"},
            {include: "orion.lib#number_decimal"},
            {include: "orion.lib#number_hex"},
            {include: "orion.lib#brace_open"},
            {include: "orion.lib#brace_close"},
            {include: "orion.lib#bracket_open"},
            {include: "orion.lib#bracket_close"},
            {include: "orion.lib#parenthesis_open"},
            {include: "orion.lib#parenthesis_close"},
            {include: "orion.lib#operator"},
            {include: "orion.lib#doc_block"},
        '''

        def Iterable<String> getKeywords() {
            GrammarUtil.containedKeywords(grammarAccess.grammar).filter [
                Character.isLetter(it.value.charAt(0))
            ].map[value].toSet.sort
        }

    }
}
