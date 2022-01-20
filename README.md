# CDODebug

This is for you who miss the ease of debugging CDOObject instances just like you would plain EObjects in the Eclipse debugger. 

Installation and usage
* See under "Releases" for installable p2 update site, and usage information.


Implementation details:
* The class CDOLogicalStructureProvider is registered to the extension point "org.eclipse.debug.core.logicalStructureProviders", using the modelIdentifier="org.eclipse.jdt.debug"
* Internally, the registered provider will instantiate CdoLogicalStructureType (which implements org.eclipse.debug.core.ILogicalStructureType) for any EObject instance. Internally, the structure type impl will deliver so-called IJavaVariables for all things you as a developer is in need to see in the debugger (i.e. the "eContainer" of a currently regarded EObject, as well as all its EStructuralFeature values). 

