package jp.ac.kyushu.iarch.checkplugin.view;

import jp.ac.kyushu.iarch.checkplugin.CheckPluginPreference;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author nakazato
 *
 */
public class CheckPluginPreferencePage
extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CheckPluginPreference.getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new IntegerFieldEditor(CheckPluginPreference.PROP_ANALYSIS_DEPTH,
				"Analysis depth", parent));
	}

	@Override
	public boolean performOk() {
		IPreferenceStore ps = getPreferenceStore();
		System.out.println(ps);
		if (ps != null) {
			int ad = ps.getInt(CheckPluginPreference.PROP_ANALYSIS_DEPTH);
			System.out.println(ad);
		}
		return super.performOk();
	}
}
