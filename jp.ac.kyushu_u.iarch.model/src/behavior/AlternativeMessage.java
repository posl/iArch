/**
 */
package behavior;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Alternative Message</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link behavior.AlternativeMessage#getMessages <em>Messages</em>}</li>
 * </ul>
 * </p>
 *
 * @see behavior.BehaviorPackage#getAlternativeMessage()
 * @model
 * @generated
 */
public interface AlternativeMessage extends Message {
	/**
	 * Returns the value of the '<em><b>Messages</b></em>' reference list.
	 * The list contents are of type {@link behavior.Message}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Messages</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Messages</em>' reference list.
	 * @see behavior.BehaviorPackage#getAlternativeMessage_Messages()
	 * @model
	 * @generated
	 */
	EList<Message> getMessages();

} // AlternativeMessage
