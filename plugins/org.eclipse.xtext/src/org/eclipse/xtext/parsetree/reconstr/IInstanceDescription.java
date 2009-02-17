package org.eclipse.xtext.parsetree.reconstr;

import java.util.Map;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * A wrapper for EObjects holding the information about what values have already
 * been consumed by a serialization process.
 *
 * @author Sven Efftinge - Initial contribution and API
 * @author Moritz Eysholdt
 */
public interface IInstanceDescription {

	public IInstanceDescription cloneAndConsume(String feature);

	public Object getConsumable(String feature, boolean allowDefault);

	/**
	 * @return the wrapped EObject
	 */
	public abstract EObject getDelegate();

	public Map<EStructuralFeature, Integer> getUnconsumed();

	public boolean isConsumed();

	public boolean isConsumedWithLastConsumtion(String feature);

	/**
	 * @param the
	 *            type name as it is used within the grammar of the given
	 *            language
	 * @return true if the delegate is a direct instance of the given type
	 */
	public abstract boolean isInstanceOf(EClassifier classifier);
}