package jp.ac.kyushu.iarch.basefunction.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ProblemViewManager {
	// Instance holder.
	private static Map<String, ProblemViewManager> managers = new HashMap<String, ProblemViewManager>();

	public static ProblemViewManager getInstance(String type) {
		if (managers.containsKey(type)) {
			return managers.get(type);
		} else {
			ProblemViewManager manager = new ProblemViewManager(type);
			managers.put(type, manager);
			return manager;
		}
	}

	// Marker type
	private String type;

	private ProblemViewManager(String type) {
		this.type = type;
	}

	public void removeAllProblems(IProject project, boolean includeSubtypes) {
		try {
			project.deleteMarkers(type, includeSubtypes, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public void removeProblems(IResource resource, boolean includeSubtypes) {
		try {
			resource.deleteMarkers(type, includeSubtypes, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public IMarker createMarker(IResource resource,
			int severity, String message, String location) {
		try {
			IMarker marker = resource.createMarker(type);
			if (marker.exists()) {
				marker.setAttribute(IMarker.SEVERITY, severity);
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.LOCATION, location);
				return marker;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	public IMarker setAttribute(IMarker marker, String attribute, Object value) {
		if (marker != null) {
			try {
				marker.setAttribute(attribute, value);
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public IMarker setAttribute(IMarker marker, String attribute, int value) {
		if (marker != null) {
			try {
				marker.setAttribute(attribute, value);
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public IMarker createErrorMarker(IResource resource, String message, String location) {
		return createMarker(resource, IMarker.SEVERITY_ERROR, message, location);
	}
	public IMarker createErrorMarker(IResource resource, String message, String location, int lineNumber) {
		return setAttribute(createMarker(resource, IMarker.SEVERITY_ERROR, message, location),
				IMarker.LINE_NUMBER, lineNumber);
	}
	public IMarker createWarningMarker(IResource resource, String message, String location) {
		return createMarker(resource, IMarker.SEVERITY_WARNING, message, location);
	}
	public IMarker createWarningMarker(IResource resource, String message, String location, int lineNumber) {
		return setAttribute(createMarker(resource, IMarker.SEVERITY_WARNING, message, location),
				IMarker.LINE_NUMBER, lineNumber);
	}
	public IMarker createInfoMarker(IResource resource, String message, String location) {
		return createMarker(resource, IMarker.SEVERITY_INFO, message, location);
	}
	public IMarker createInfoMarker(IResource resource, String message, String location, int lineNumber) {
		return setAttribute(createMarker(resource, IMarker.SEVERITY_INFO, message, location),
				IMarker.LINE_NUMBER, lineNumber);
	}

	//
	// for compatibility.
	//

	// TODO: This class should not have marker ID defined in checkplugin project.
	private static final String OPTIONAL_MARKER_ID = "jp.ac.kyushu.iarch.checkplugin.OptionalMarker";

	private static ProblemViewManager problemviewmanager = getInstance(IMarker.PROBLEM);

	public static ProblemViewManager getInstance(){
		return problemviewmanager;
	}

	public static void removeAllProblems(IProject project){
		problemviewmanager.removeAllProblems(project, true);
		getInstance(OPTIONAL_MARKER_ID).removeAllProblems(project, false);
	}

	public static void addError(IResource resource, String message, String location) {
		problemviewmanager.createErrorMarker(resource, message, location, 11);
	}
	public static IMarker addError1(IResource resource, String message, String location, int lineNumber) {
		return problemviewmanager.createErrorMarker(resource, message, location, lineNumber);
	}
	public static void addWarning1(IResource resource, String message, String location, int lineNumber) {
		problemviewmanager.createWarningMarker(resource, message, location, lineNumber);
	}
	public static void addInfo(IResource resource, String message, String location) {
		problemviewmanager.createInfoMarker(resource, message, location);
	}
	public static void addInfo1(IResource resource, String message, String location, int lineNumber) {
		problemviewmanager.createInfoMarker(resource, message, location, lineNumber);
	}

	public static void addOptionalInfo(IResource resource, String message, String location, int lineNumber) {
		ProblemViewManager manager = getInstance(OPTIONAL_MARKER_ID);
		IMarker marker = manager.createInfoMarker(resource, message, location, lineNumber);
		// delete severity and location.
		manager.setAttribute(marker, IMarker.SEVERITY, null);
		manager.setAttribute(marker, IMarker.LOCATION, null);
	}

	// Test function.
	public static void printMarkers(IResource resource, String type) {
		System.out.println("[" + resource + " / " + type + "]");
		try {
			for (IMarker m : resource.findMarkers(type, true, 2)) {
				System.out.println(m.getType());
				System.out.println(m.getResource());
				for (java.util.Map.Entry<String, Object> entry : m.getAttributes().entrySet()) {
					System.out.println("\t" + entry.getKey() + ": " + entry.getValue().toString());
				}
//				System.out.println(m.isSubtypeOf("org.eclipse.core.resources.marker"));
//				System.out.println(m.isSubtypeOf("org.eclipse.core.resources.problemmarker"));
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
