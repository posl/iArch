package jp.ac.kyushu.iarch.checkplugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CheckPluginPreferenceInitializer extends AbstractPreferenceInitializer {
	private static final int DEFAULT_ANALYSIS_DEPTH = 1;

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore ps = CheckPluginPreference.getPreferenceStore();
		ps.setDefault(CheckPluginPreference.PROP_ANALYSIS_DEPTH, DEFAULT_ANALYSIS_DEPTH);
	}
}
