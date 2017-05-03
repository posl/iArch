/**
 */
package umlClass;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Alternative Operation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link umlClass.AlternativeOperation#getOperations <em>Operations</em>}</li>
 * </ul>
 * </p>
 *
 * @see umlClass.UmlClassPackage#getAlternativeOperation()
 * @model
 * @generated
 */
public interface AlternativeOperation extends Operation {
	/**
	 * Returns the value of the '<em><b>Operations</b></em>' containment reference list.
	 * The list contents are of type {@link umlClass.Operation}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Operations</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Operations</em>' containment reference list.
	 * @see umlClass.UmlClassPackage#getAlternativeOperation_Operations()
	 * @model containment="true"
	 * @generated
	 */
	EList<Operation> getOperations();

} // AlternativeOperation
