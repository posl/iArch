package jp.ac.kyushu.iarch.checkplugin;

import org.eclipse.jface.preference.IPreferenceStore;

public class CheckPluginPreference {
	public static final String PROP_ANALYSIS_DEPTH = "ANALYSIS_DEPTH";

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public int getAnalysisDepth() {
		return getPreferenceStore().getInt(PROP_ANALYSIS_DEPTH);
	}
}
