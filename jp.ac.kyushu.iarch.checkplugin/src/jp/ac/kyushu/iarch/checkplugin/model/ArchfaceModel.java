package jp.ac.kyushu.iarch.checkplugin.model;

import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;

import org.eclipse.core.resources.IResource;

public class ArchfaceModel {
	public static Model getArchfaceModel(IResource archfile) {
		return new ArchModel(archfile).getModel();
	}
}
