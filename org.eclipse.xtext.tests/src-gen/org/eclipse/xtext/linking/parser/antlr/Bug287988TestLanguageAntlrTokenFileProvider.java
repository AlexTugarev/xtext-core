/*
 * generated by Xtext
 */
package org.eclipse.xtext.linking.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class Bug287988TestLanguageAntlrTokenFileProvider implements IAntlrTokenFileProvider {
	
	@Override
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
    	return classLoader.getResourceAsStream("org/eclipse/xtext/linking/parser/antlr/internal/InternalBug287988TestLanguage.tokens");
	}
}
