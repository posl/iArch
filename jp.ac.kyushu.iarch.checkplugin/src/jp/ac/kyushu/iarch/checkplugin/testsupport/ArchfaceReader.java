package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.ArrayList;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Param;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;

import org.eclipse.core.resources.IFile;

/**
 * @author watanabeke
 */
public class ArchfaceReader implements IInfoGenerateReader {

	private static final ArchfaceReader instance = new ArchfaceReader();

	private ArchfaceReader() {}

	public static ArchfaceReader getInstance() {
		return instance;
	}

	@Override
	public SelectionInfo read(IFile file) {
		// 準備
		ArchModel archModel = new ArchModel(file);
		Model model = archModel.getModel();

		// 読み出し
		return readModelModel(model);
	}

	private SelectionInfo readModelModel(Model model) {
		ArrayList<InterfaceInfo> infos = new ArrayList<>();
		for (UncertainInterface childModel : model.getU_interfaces()) {
			infos.add(readUncertainInterfaceModel(childModel));
		}
		return new SelectionInfo(infos);
	}

	private InterfaceInfo readUncertainInterfaceModel(UncertainInterface model) {
		ArrayList<AbstractUncertaintyInfo> infos = new ArrayList<>();
		for (OptMethod childModel : model.getOptmethods()) {
			infos.add(readOptMethodModel(childModel));
		}
		for (AltMethod childModel : model.getAltmethods()) {
			infos.add(readAltMethodModel(childModel));
		}
		return new InterfaceInfo(infos, model.getSuperInterface().getName());
	}

	private OptionalInfo readOptMethodModel(OptMethod model) {
		return new OptionalInfo(readMethodModel(model.getMethod()));
	}

	private AlternativeInfo readAltMethodModel(AltMethod model) {
		ArrayList<MethodInfo> infos = new ArrayList<>();
		for (Method childModel : model.getMethods()) {
			infos.add(readMethodModel(childModel));
		}
		return new AlternativeInfo(infos);
	}

	private MethodInfo readMethodModel(Method model) {
		ArrayList<ParameterInfo> infos = new ArrayList<>();
		for (Param childModel : model.getParam()) {
			infos.add(readParamModel(childModel));
		}
		return new MethodInfo(infos, model.getType(), model.getName());
	}

	private ParameterInfo readParamModel(Param model) {
		return new ParameterInfo(model.getType(), model.getName());
	}

}
