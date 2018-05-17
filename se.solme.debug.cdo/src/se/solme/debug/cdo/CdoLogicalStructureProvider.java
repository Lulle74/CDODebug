package se.solme.debug.cdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILogicalStructureProvider;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaInterfaceType;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaType;

public class CdoLogicalStructureProvider implements ILogicalStructureProvider
{

	@Override
	public ILogicalStructureType[] getLogicalStructureTypes( IValue value )
	{
		List<ILogicalStructureType> logicalStructures = new ArrayList<>();
		if ( value instanceof IJavaObject ) {
			IJavaObject javaValue = (IJavaObject)value;

			try {
				IJavaType type = javaValue.getJavaType();
				if ( type instanceof IJavaClassType ) {
					IJavaClassType classType = (IJavaClassType)type;

					boolean isEObject = false;
					//String className = classType.getName();
					IJavaClassType currentClass = classType;
					while ( currentClass != null ) {
						if ( Objects.equals( currentClass.getName(), "org.eclipse.emf.ecore.EObject" ) ) {
							isEObject = true;
							break;
						}
						currentClass = currentClass.getSuperclass();
					}

					if ( isEObject == false ) {
						IJavaInterfaceType[] superInterfaces = classType.getAllInterfaces();
						for ( IJavaInterfaceType superInterface : superInterfaces ) {
							if ( Objects.equals( superInterface.getName(), "org.eclipse.emf.ecore.EObject" ) ) {
								isEObject = true;
								break;
							}
						}
					}

					if ( isEObject ) {
						//System.out.println( "Class: " + className ); //$NON-NLS-1$
						CdoLogicalStructureType cdoLogicalStructureType = new CdoLogicalStructureType();
						logicalStructures.add( cdoLogicalStructureType );
					}
				}
				else {
					//System.out.println( "Not a IJavaClassType " + type );
				}
			}
			catch ( DebugException e ) {
				System.out.println( "Exception occurred!" );
				//return new ILogicalStructureType[0];
				logicalStructures = new ArrayList<>();
			}
		}
		else {
			//System.out.println( "Not a IJavaObject" + value );
		}

		return logicalStructures.toArray( new ILogicalStructureType[logicalStructures.size()] );
	}
}
