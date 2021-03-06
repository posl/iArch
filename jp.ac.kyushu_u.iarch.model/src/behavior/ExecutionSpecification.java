/**
 */
package behavior;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Execution Specification</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link behavior.ExecutionSpecification#getStart <em>Start</em>}</li>
 *   <li>{@link behavior.ExecutionSpecification#getFinish <em>Finish</em>}</li>
 * </ul>
 * </p>
 *
 * @see behavior.BehaviorPackage#getExecutionSpecification()
 * @model abstract="true"
 * @generated
 */
public interface ExecutionSpecification extends InteractionFragment {
	/**
	 * Returns the value of the '<em><b>Start</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Start</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Start</em>' reference.
	 * @see #setStart(OccurrenceSpecification)
	 * @see behavior.BehaviorPackage#getExecutionSpecification_Start()
	 * @model required="true"
	 * @generated
	 */
	OccurrenceSpecification getStart();

	/**
	 * Sets the value of the '{@link behavior.ExecutionSpecification#getStart <em>Start</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Start</em>' reference.
	 * @see #getStart()
	 * @generated
	 */
	void setStart(OccurrenceSpecification value);

	/**
	 * Returns the value of the '<em><b>Finish</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Finish</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Finish</em>' reference.
	 * @see #setFinish(OccurrenceSpecification)
	 * @see behavior.BehaviorPackage#getExecutionSpecification_Finish()
	 * @model required="true"
	 * @generated
	 */
	OccurrenceSpecification getFinish();

	/**
	 * Sets the value of the '{@link behavior.ExecutionSpecification#getFinish <em>Finish</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Finish</em>' reference.
	 * @see #getFinish()
	 * @generated
	 */
	void setFinish(OccurrenceSpecification value);

} // ExecutionSpecification
