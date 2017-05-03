package jp.ac.kyushu.iarch.checkplugin;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ac.kyushu.iarch.checkplugin.handler.DeleteJavaCode;
import jp.ac.kyushu.iarch.checkplugin.handler.InsertJavaCode;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class ArchfaceMarkerResolutionGenerator implements IMarkerResolutionGenerator {
	private static final String behaviorRegex = "(Behavior)\\s*:\\s*([A-Za-z_0-9]*)\\s*:\\s*([A-Za-z_0-9]*).([A-Za-z_0-9]*)\\s*:\\s*([A-zA-Z_0-9]*)\\s*is not defined";
	private static final String interfaceRegex = "(Interface-)\\s*([a-zA-Z_0-9]*)\\s*is not defined";
	private static final String javaCodeRegex = "(JavaCode-)\\s*([A-Za-z_0-9]*)\\s*:\\s*([a-zA-Z_0-9]*)\\s*is not in the Archface";
	private static final Pattern behaviorPattern = Pattern.compile(behaviorRegex);
	private static final Pattern interfacePattern = Pattern.compile(interfaceRegex);
	private static final Pattern javaCodePattern = Pattern.compile(javaCodeRegex);

	private class InsertingResolution implements IMarkerResolution {
		private InsertingResolution() {}

		@Override
		public void run(IMarker marker) {
			try {
				String Message = (String) marker.getAttribute(IMarker.MESSAGE);
				String Path = (String) marker.getAttribute(IMarker.LOCATION);
				System.out.println(Message);
				Message = Message.trim();

				Matcher behaviorMatcher = behaviorPattern.matcher(Message);
				while (behaviorMatcher.find()) {
					//String Message1 = behaviorMatcher.group(3).toString();
					//Path = "C:/Users/Liyuning/Desktop/iArch/ObserverPattern/src/" + Path + ".java";
					String MethodName = behaviorMatcher.group(4).toString();
					String Insertcode = behaviorMatcher.group(5).toString();
					InsertJavaCode.insert(Path, MethodName, Insertcode);
				}

				Matcher interfaceMatcher = interfacePattern.matcher(Message);
				while (interfaceMatcher.find()) {
					//Path = "C:/Users/Liyuning/Desktop/iArch/ObserverPattern/src/" + Path + ".java";
					String MethodName = interfaceMatcher.group(2).toString();
					System.out.println(MethodName);
					InsertJavaCode.insert2(Path, MethodName);
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public String getLabel() {
			return "Fix Archface Error by Inserting ";
		}
	}

	private class DeletingResolution implements IMarkerResolution {
		private DeletingResolution() {}

		@Override
		public void run(IMarker marker) {
			try {
				String Message = (String) marker.getAttribute(IMarker.MESSAGE);
				String Path = (String) marker.getAttribute(IMarker.LOCATION);
				System.out.println(Path);
				Message = Message.trim();

				Matcher javaCodeMatcher = javaCodePattern.matcher(Message);
				while (javaCodeMatcher.find()) {
					String MethodName = javaCodeMatcher.group(3).toString();
					DeleteJavaCode.delete(Path, MethodName);
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public String getLabel() {
			return "Fix Archface Warning by deleting";
		}
	};

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		//IMarkerResolution resolution = new InsertingResolution();
		//IMarkerResolution resolution2 = new DeletingResolution();
		//return new IMarkerResolution[] {resolution, resolution2};
		ArrayList<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
		try {
			String Message = ((String) marker.getAttribute(IMarker.MESSAGE)).trim();
			if (behaviorPattern.matcher(Message).lookingAt()
					|| interfacePattern.matcher(Message).lookingAt()) {
				resolutions.add(new InsertingResolution());
			}
			if (javaCodePattern.matcher(Message).lookingAt()) {
				resolutions.add(new DeletingResolution());
			}
		} catch (CoreException e) {
		}
		return resolutions.toArray(new IMarkerResolution[resolutions.size()]);
	}
}
