package se.solme.debug.cdo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaInterfaceType;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.core.logicalstructures.JDIPlaceholderVariable;
import org.eclipse.jdt.internal.debug.core.logicalstructures.LogicalObjectStructureValue;

import se.solme.debug.cdo.internal.Methods;

@SuppressWarnings("restriction")
public class CdoLogicalStructureType implements ILogicalStructureType {

	@Override
	public boolean providesLogicalStructure(IValue value) {
		if (!(value instanceof IJavaObject)) {
			return false;
		}
		return getType((IJavaObject) value) != null;
	}

	@Override
	public IValue getLogicalStructure(IValue value) throws CoreException {

		System.out.println("Provide structure for: " + value);
		if (!(value instanceof IJavaObject)) {
			return value;
		}

		IJavaObject javaValue = (IJavaObject) value;
		try {
			IJavaReferenceType type = getType(javaValue);
			if (type == null) {
				return value;
			}
			IJavaStackFrame stackFrame = getStackFrame(javaValue);
			if (stackFrame == null) {
				return value;
			}
			IJavaProject project = JavaDebugUtils.resolveJavaProject(stackFrame);
			if (project == null) {
				return value;
			}

			IJavaThread javaThread = this.getJavaThread(this.getStackFrame(javaValue));
			if (javaThread == null) {
				return value;
			}

			List<IJavaVariable> variables = new ArrayList<>();

			// The "eContainer" implicit EReference.
			IJavaValue eContainer = javaValue.sendMessage(Methods.EObject_eContainer, Methods.EObject_eContainer_Sign,
					new IJavaValue[0], javaThread, null);
			variables.add(new JDIPlaceholderVariable(Methods.EObject_eContainer, eContainer, javaValue));

			// The EClass of the EObject:
			IJavaValue eClassValue = javaValue.sendMessage(Methods.EObject_eClass, Methods.EObject_eClass_Sign,
					new IJavaValue[0], javaThread, null);
			if (eClassValue instanceof IJavaObject) {
				// All structural features of the EClass (as an EList):
				IJavaValue eAllStructuralFeatures = ((IJavaObject) eClassValue).sendMessage(
						Methods.EClass_getEAllStructuralFeatures, Methods.EClass_getEAllStructuralFeatures_Sign,
						new IJavaValue[0], javaThread, null);
				if (eAllStructuralFeatures instanceof IJavaObject) {
					// Get the structural features as an array:
					IJavaArray eSFsArray = (IJavaArray) ((IJavaObject) eAllStructuralFeatures).sendMessage(
							Methods.List_toArray, Methods.List_toArray_Sign, new IJavaValue[0], javaThread, null);
					for (IJavaValue element : eSFsArray.getValues()) {
						// Name of the EStructuralFeature:
						IJavaValue name = ((IJavaObject) element).sendMessage(Methods.ENamedElement_getName,
								Methods.ENamedElement_getName_Sign, new IJavaValue[0], javaThread, null);

						// Value of the EStructuralFeature:
						IJavaValue actualAttribValue = javaValue.sendMessage(Methods.EObject_eGet,
								Methods.EObject_eGet_Sign, new IJavaValue[] { element }, javaThread, null);
						//Finally, add as Variable:
						variables.add(new JDIPlaceholderVariable(name.getValueString(), actualAttribValue, javaValue));
					}
				}

			}
			//What we return as "logical children" for the EObject are the variables.
			return new LogicalObjectStructureValue(javaValue, variables.toArray(new JDIPlaceholderVariable[0]));

		} catch (CoreException e) {
			if (e.getStatus().getCode() == IJavaThread.ERR_THREAD_NOT_SUSPENDED) {
				throw e;
			}
			JDIDebugPlugin.log(e);
		}
		return value;
	}

	@Override
	public String getDescription(IValue value) {
		return getDescription();
		// if ( value != null ) {
		// try {
		// return "CDO: " + value.getValueString();
		// } catch (DebugException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// return null;
	}

	@Override
	public String getDescription() {
		return "CDO Object [logical struct]";
	}

	@Override
	public String getId() {
		return "se.solme.debug.cdo" + this.fType + this.getDescription();
	}

	boolean fSubtypes = true;
	/**
	 * Fully qualified type name.
	 */
	private String fType = "org.eclipse.emf.cdo.CDOObject";

	/**
	 * Returns the <code>IJavaReferenceType</code> from the specified
	 * <code>IJavaObject</code>
	 *
	 * @param value
	 * @return the <code>IJavaReferenceType</code> from the specified
	 *         <code>IJavaObject</code>
	 */
	private IJavaReferenceType getType(IJavaObject value) {
		try {
			IJavaType type = value.getJavaType();
			if (!(type instanceof IJavaClassType)) {
				return null;
			}
			IJavaClassType classType = (IJavaClassType) type;
			if (classType.getName().equals(fType)) {
				// found the type
				return classType;
			}
			if (!fSubtypes) {
				// if not checking the subtypes, stop here
				return null;
			}
			IJavaClassType superClass = classType.getSuperclass();
			while (superClass != null) {
				if (superClass.getName().equals(fType)) {
					// found the type, it's a super class
					return superClass;
				}
				superClass = superClass.getSuperclass();
			}
			IJavaInterfaceType[] superInterfaces = classType.getAllInterfaces();
			for (IJavaInterfaceType superInterface : superInterfaces) {
				if (superInterface.getName().equals(fType)) {
					// found the type, it's a super interface
					return superInterface;
				}
			}
		} catch (DebugException e) {
			System.out.println("error " + e.getMessage());
			JDIDebugPlugin.log(e);
			return null;
		}
		return null;
	}

	/**
	 * Return the current stack frame context, or a valid stack frame for the given
	 * value.
	 *
	 * @param value
	 * @return the current stack frame context, or a valid stack frame for the given
	 *         value.
	 * @throws CoreException
	 */
	private IJavaStackFrame getStackFrame(IValue value) throws CoreException {
		IStatusHandler handler = getStackFrameProvider();
		if (handler != null) {
			IJavaStackFrame stackFrame = (IJavaStackFrame) handler
					.handleStatus(JDIDebugPlugin.STATUS_GET_EVALUATION_FRAME, value);
			if (stackFrame != null) {
				return stackFrame;
			}
		}
		IDebugTarget target = value.getDebugTarget();
		IJavaDebugTarget javaTarget = target.getAdapter(IJavaDebugTarget.class);
		if (javaTarget != null) {
			IThread[] threads = javaTarget.getThreads();
			for (IThread thread : threads) {
				if (thread.isSuspended()) {
					return (IJavaStackFrame) thread.getTopStackFrame();
				}
			}
		}
		return null;
	}

	private IJavaThread getJavaThread(IJavaStackFrame stackFrame) {
		return stackFrame != null ? (IJavaThread) stackFrame.getThread() : null;
	}

	private static IStatusHandler fgStackFrameProvider;

	/**
	 * Returns the singleton stackframe provider
	 *
	 * @return the singleton stackframe provider
	 */
	private static IStatusHandler getStackFrameProvider() {
		if (fgStackFrameProvider == null) {
			fgStackFrameProvider = DebugPlugin.getDefault()
					.getStatusHandler(JDIDebugPlugin.STATUS_GET_EVALUATION_FRAME);
		}
		return fgStackFrameProvider;
	}

}
