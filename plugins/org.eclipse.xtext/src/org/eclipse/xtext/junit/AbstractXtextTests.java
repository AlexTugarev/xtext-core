package org.eclipse.xtext.junit;

import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.m2t.type.emf.EmfRegistryMetaModel;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.service.ILanguageDescriptor;
import org.eclipse.xtext.service.InjectedService;
import org.eclipse.xtext.service.ServiceRegistry;
import org.eclipse.xtext.util.StringInputStream;
import org.openarchitectureware.expression.ExecutionContextImpl;
import org.openarchitectureware.xtend.XtendFacade;

public abstract class AbstractXtextTests extends TestCase {
	
	public AbstractXtextTests() {
		super();
	}

	public AbstractXtextTests(String name) {
		super(name);
	}
	
	@InjectedService
	private IParser parser;
	public IParser getParser() {
		return parser;
	}
	
	protected void setCurrentLanguage(ILanguageDescriptor desc) {
		ServiceRegistry.injectServices(desc, this);
	}
	
	public IParseResult parse(InputStream model) throws Exception {
		return getParser().parse(model);
	}
	
	public IParseResult parse(String model) throws Exception {
        return getParser().parse(new StringInputStream(model));
    }
	
	// Xtend helper methods

	protected void assertWithXtend(String left, String right, Object _this) {
		assertWithXtend(left + " != " + right, left, right, _this);
	}

	protected Object invokeWithXtend(String expression, Object _this) {
		XtendFacade f = getXtendFacade();
		f = f.cloneWithExtensions(getImportDeclarations()+"invoke(Object this) : " + expression + ";");
		return f.call("invoke", _this);
	}

	protected String[] importedExtensions() {
		return new String[0];
	}

	protected void assertWithXtend(String message, String left, String right, Object _this) {
		XtendFacade f = getXtendFacade();
		StringBuffer code = getImportDeclarations();
		code.append("__compare(Object this) : __left(this) == __right(this);__left(Object this) : " + left
				+ "; __right(Object this) :" + right + ";");
		f = f.cloneWithExtensions(code.toString());
		Boolean result = (Boolean) f.call("__compare", _this);
		if (!result) {
			Object leftResult = f.call("__left", _this);
			Object rightResult = f.call("__right", _this);
			fail(message + " was : " + leftResult + "("
					+ (leftResult != null ? leftResult.getClass().getSimpleName() : "") + ") != " + rightResult + "("
					+ (leftResult != null ? leftResult.getClass().getSimpleName() : "") + ")");
		}
	}

	private StringBuffer getImportDeclarations() {
		StringBuffer code = new StringBuffer();
		for (String _import : importedExtensions()) {
			code.append("extension ").append(_import).append(";");
		}
		return code;
	}

	protected XtendFacade getXtendFacade() {
		ExecutionContextImpl ctx = new ExecutionContextImpl();
		ctx.registerMetaModel(new EmfRegistryMetaModel());
		return XtendFacade.create(ctx);
	}
	
}
