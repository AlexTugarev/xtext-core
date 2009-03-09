/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.validator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.xtext.crossref.IScope;
import org.eclipse.xtext.crossref.IScopedElement;
import org.eclipse.xtext.util.Function;
import org.eclipse.xtext.util.SimpleCache;

/**
 * @author Sven Efftinge - Initial contribution and API
 * 
 * 
 *         Allows subclasses to specify invariants in a declarative manner using
 *         {@link Check} annotation.
 * 
 *         Example:
 * 
 *         <pre>
 * &#064;Check
 * void checkName(ParserRule rule) {
 * 	if (!toFirstUpper(rule.getName()).equals(rule.getName())) {
 * 		warning(&quot;Name should start with a capital.&quot;, XtextPackage.ABSTRACT_RULE__NAME);
 * 	}
 * }
 * </pre>
 */
public abstract class AbstractDeclarativeValidator extends EObjectValidator {

	public static final Logger log = Logger.getLogger(AbstractDeclarativeValidator.class);

	static class MethodWrapper {
		public final Method method;
		private final String s;
		private final AbstractDeclarativeValidator instance;

		public MethodWrapper(AbstractDeclarativeValidator instance, Method m) {
			this.instance = instance;
			this.method = m;
			this.s = m.getName() + ":" + m.getParameterTypes()[0].getName();
		}

		@Override
		public int hashCode() {
			return s.hashCode() ^ instance.hashCode();
		}

		public boolean isMatching(Class<?> param) {
			return method.getParameterTypes()[0].isAssignableFrom(param);
		}

		public void invoke(State state) {
			if (instance.state != null && instance.state != state)
				throw new IllegalStateException("State is already assigned.");
			boolean wasNull = instance.state == null;
			if (wasNull)
				instance.state = state;
			try {
				Check annotation = method.getAnnotation(Check.class);
				if (!state.checkMode.shouldCheck(annotation.value()))
					return;
				boolean wasAccessible = method.isAccessible();
				try {
					state.currentMethod = method;
					method.setAccessible(true);
					method.invoke(instance, state.currentObject);
				}
				catch (IllegalArgumentException e) {
					log.error(e.getMessage(), e);
				}
				catch (IllegalAccessException e) {
					log.error(e.getMessage(), e);
				}
				catch (InvocationTargetException e) {
					// ignore GuardException, check is just not evaluated if
					// guard is false
					// ignore NullPointerException, as not having to check for
					// NPEs all the time is a convenience feature
					Throwable targetException = e.getTargetException();
					if (!(targetException instanceof GuardException)
							&& !(targetException instanceof NullPointerException))
						throw new RuntimeException(targetException);
				}
				finally {
					method.setAccessible(wasAccessible);
				}
			}
			finally {
				if (wasNull)
					instance.state = null;
			}
		}

		@Override
		public boolean equals(Object obj) {
			MethodWrapper mw = (MethodWrapper) obj;
			return s.equals(mw.s) && instance == mw.instance;
		}
	}

	private HashSet<MethodWrapper> checkMethods = null;

	public AbstractDeclarativeValidator() {
	}

	private List<MethodWrapper> collectMethods(Class<? extends AbstractDeclarativeValidator> clazz) {
		List<MethodWrapper> checkMethods = new ArrayList<MethodWrapper>();
		Set<Class<?>> visitedClasses = new HashSet<Class<?>>(4);
		collectMethods(this, clazz, visitedClasses, checkMethods);
		return checkMethods;
	}

	private void collectMethods(AbstractDeclarativeValidator instance,
			Class<? extends AbstractDeclarativeValidator> clazz, Collection<Class<?>> visitedClasses,
			Collection<MethodWrapper> result) {
		if (visitedClasses.contains(clazz))
			return;
		collectMethodsImpl(instance, clazz, visitedClasses, result);
		Class<? extends AbstractDeclarativeValidator> k = clazz;
		while (k != null) {
			ComposedChecks checks = k.getAnnotation(ComposedChecks.class);
			if (checks != null) {
				for (Class<? extends AbstractDeclarativeValidator> external : checks.validators())
					collectMethods(null, external, visitedClasses, result);
			}
			k = getSuperClass(k);
		}
	}

	private Class<? extends AbstractDeclarativeValidator> getSuperClass(
			Class<? extends AbstractDeclarativeValidator> clazz) {
		try {
			Class<? extends AbstractDeclarativeValidator> superClass = clazz.getSuperclass().asSubclass(
					AbstractDeclarativeValidator.class);
			if (AbstractDeclarativeValidator.class.equals(superClass))
				return null;
			return superClass;
		}
		catch (ClassCastException e) {
			return null;
		}
	}

	private void collectMethodsImpl(AbstractDeclarativeValidator instance,
			Class<? extends AbstractDeclarativeValidator> clazz, Collection<Class<?>> visitedClasses,
			Collection<MethodWrapper> result) {
		if (!visitedClasses.add(clazz))
			return;
		AbstractDeclarativeValidator instanceToUse;
		try {
			instanceToUse = instance == null ? clazz.newInstance() : instance;
		}
		catch (Exception e) {
			Constructor<? extends AbstractDeclarativeValidator> constr = null;
			boolean wasAccessible = false;
			try {
				constr = clazz.getDeclaredConstructor();
				wasAccessible = constr.isAccessible();
				constr.setAccessible(true);
				instanceToUse = constr.newInstance();
			}
			catch (Exception ex) {
				return;
			}
			finally {
				if (constr != null)
					constr.setAccessible(wasAccessible);
			}
		}
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getAnnotation(Check.class) != null && method.getParameterTypes().length == 1) {
				result.add(new MethodWrapper(instanceToUse, method));
			}
		}
		Class<? extends AbstractDeclarativeValidator> superClass = getSuperClass(clazz);
		if (superClass != null)
			collectMethodsImpl(instanceToUse, superClass, visitedClasses, result);
	}

	private final SimpleCache<Class<?>, List<MethodWrapper>> methodsForType = new SimpleCache<Class<?>, List<MethodWrapper>>(
			new Function<Class<?>, List<MethodWrapper>>() {
				public List<MethodWrapper> exec(Class<?> param) {
					List<MethodWrapper> result = new ArrayList<MethodWrapper>();
					for (MethodWrapper mw : checkMethods) {
						if (mw.isMatching(param))
							result.add(mw);
					}
					return result;
				}
			});

	public static class State {
		private DiagnosticChain chain = null;
		private EObject currentObject = null;
		private Method currentMethod = null;
		private CheckMode checkMode = null;
		private boolean hasErrors = false;
	}

	private State state;

	protected EObject getCurrentObject() {
		return state.currentObject;
	}

	protected Method getCurrentMethod() {
		return state.currentMethod;
	}

	protected DiagnosticChain getChain() {
		return state.chain;
	}

	protected CheckMode getCheckMode() {
		return state.checkMode;
	}

	@Override
	public final boolean validate(EClass class1, EObject object, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		if (checkMethods == null) {
			checkMethods = new HashSet<MethodWrapper>();
			checkMethods.addAll(collectMethods(getClass()));
		}
		boolean isValid = validate_EveryDefaultConstraint(object, diagnostics, context);

		CheckMode checkMode = CheckMode.ALL;
		if (context != null) {
			Object object2 = context.get(CheckMode.KEY);
			if (object2 instanceof CheckMode) {
				checkMode = (CheckMode) object2;
			}
			else if (object2 != null) {
				throw new IllegalArgumentException("Context object for key " + CheckMode.KEY + " should be of Type "
						+ CheckMode.class.getName() + " but was " + object2.getClass().getName());
			}
		}

		State state = new State();
		state.chain = diagnostics;
		state.currentObject = object;
		state.checkMode = checkMode;

		for (MethodWrapper method : methodsForType.get(object.getClass())) {
			method.invoke(state);
		}

		return isValid && !state.hasErrors;
	}

	protected void warning(String string, int feature) {
		state.chain.add(new DiagnosticImpl(Diagnostic.WARNING, string, state.currentObject, feature));
	}

	protected void error(String string, int feature) {
		this.state.hasErrors = true;
		state.chain.add(new DiagnosticImpl(Diagnostic.ERROR, string, state.currentObject, feature));
	}

	protected void assertTrue(String message, int feature, boolean executedPredicate) {
		if (!executedPredicate)
			error(message, feature);
	}

	protected void assertFalse(String message, int feature, boolean executedPredicate) {
		if (executedPredicate)
			error(message, feature);
	}

	protected void assertNull(String message, int feature, Object object) {
		if (object != null)
			error(message, feature);
	}

	protected void assertNotNull(String message, int feature, Object object) {
		if (object == null)
			error(message, feature);
	}

	protected void assertEquals(String message, int feature, Object expected, Object actual) {
		if (!expected.equals(actual))
			error(message, feature);
	}

	protected void assertNotEquals(String message, int feature, Object expected, Object actual) {
		if (expected.equals(actual))
			error(message, feature);
	}

	protected void assertEmpty(String message, int feature, String string) {
		if (string != null && string.trim().length() > 0)
			error(message, feature);
	}

	protected void assertNotEmpty(String message, int feature, String string) {
		if (string == null || string.trim().length() == 0)
			error(message, feature);
	}

	/**
	 * 
	 * @deprecated Since the contract of a scope is, that for all
	 *             IScopedElements returned by {@link IScope#getContents()} the
	 *             name feature is unique it doesn't make sense to use a scope
	 *             to find duplicate names (as they shouldn't be returned at
	 *             all)
	 */
	@Deprecated
	protected void assertNameIsUniqueInScope(String message, int feature, EObject object, String name, IScope scope) {
		for (Iterator<IScopedElement> i = scope.getContents().iterator(); i.hasNext();) {
			IScopedElement scopedElement = i.next();
			if (!object.equals(scopedElement.element()) && name.equals(scopedElement.name())) {
				error(message, feature);
			}
		}
	}

	protected void guard(boolean guardExpression) {
		if (!guardExpression) {
			throw new GuardException();
		}
	}

	static class DiagnosticImpl implements Diagnostic {

		private DiagnosticImpl(int severity, String message, EObject source, int feature) {
			super();
			this.severity = severity;
			this.message = message;
			this.source = source;
			this.feature = feature;
		}

		private final String message;
		private final EObject source;
		private final Integer feature;
		private final int severity;

		public List<Diagnostic> getChildren() {
			return null;
		}

		public int getCode() {
			return 0;
		}

		public List<?> getData() {
			List<Object> result = new ArrayList<Object>(2);
			result.add(source);
			if (feature != null)
				result.add(feature);
			return result;
		}

		public Throwable getException() {
			return null;
		}

		public String getMessage() {
			return message;
		}

		public int getSeverity() {
			return severity;
		}

		public String getSource() {
			return source.toString();
		}

		// partially copied from BasicDiagnostic#toString()
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("Diagnostic ");
			switch (severity) {
				case OK: {
					result.append("OK");
					break;
				}
				case INFO: {
					result.append("INFO");
					break;
				}
				case WARNING: {
					result.append("WARNING");
					break;
				}
				case ERROR: {
					result.append("ERROR");
					break;
				}
				case CANCEL: {
					result.append("CANCEL");
					break;
				}
				default: {
					result.append(Integer.toHexString(severity));
					break;
				}
			}

			result.append(" source=");
			result.append(source);

			result.append(' ');
			result.append(message);

			result.append(" feature=");
			result.append(feature);

			return result.toString();
		}

	}
}
