package jp.ac.kyushu_u.iarch.basefunction.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCallChoice;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;

/**
 * A class of Archface-U connector for type check.
 */
public class ConnectorTypeCheckModel {
	public static abstract class CallModel {
		protected String className = null;
		protected List<String> methodNames = new ArrayList<String>();

		public String getClassName() {
			return className;
		}
		public List<String> getMethodNames() {
			return methodNames;
		}

		abstract public boolean isCertain();
		abstract public boolean isOptional();
		abstract public boolean isAlternative();

		public boolean sameCall(CallModel cm) {
			if (isCertain() != cm.isCertain()) {
				return false;
			}
			if (isOptional() != cm.isOptional()) {
				return false;
			}
			if (isAlternative() != cm.isAlternative()) {
				return false;
			}
			if (className == null) {
				if (cm.className != null) {
					return false;
				}
			} else {
				if (!className.equals(cm.className)) {
					return false;
				}
			}
			return new HashSet<String>(methodNames).equals(new HashSet<String>(cm.methodNames));
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (isCertain()) {
				sb.append("Certain ");
			}
			if (isOptional()) {
				sb.append("Optional ");
			}
			if (isAlternative()) {
				sb.append("Alternative ");
			}
			sb.append(className).append(".");
			for (String methodName : methodNames) {
				sb.append(methodName).append("/");
			}
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}

	// TODO: Copied from checkplugin project. Should integrate later.
	private static String _getClassName(Method method, boolean allowUncertain) {
		EObject container = method.eContainer();
		if (container instanceof Interface) {
			return ((Interface) container).getName();
		} else if (allowUncertain
				&& (container instanceof OptMethod || container instanceof AltMethod)) {
			EObject uContainer = container.eContainer();
			if (uContainer instanceof UncertainInterface) {
				return ((UncertainInterface) uContainer).getSuperInterface().getName();
			}
		}
		return null;
	}

	private static class MethodCallModel extends CallModel {
//		private Method method;

		private MethodCallModel(Method method) {
//			this.method = method;
			this.className = _getClassName(method, true);
			this.methodNames.add(method.getName());
		}

		@Override
		public boolean isCertain() {
			return true;
		}
		@Override
		public boolean isOptional() {
			return false;
		}
		@Override
		public boolean isAlternative() {
			return false;
		}
	}

	private static class SuperCallModel extends CallModel {
//		private SuperCall superCall;
//		private ArrayList<Method> methods;
		private boolean certain;
		private boolean optional;
		private boolean alternative;

		private SuperCallModel(SuperCall superCall) {
//			this.superCall = superCall;
//			this.methods = new ArrayList<Method>();
			this.certain = false;
			this.optional = false;
			this.alternative = false;

			if (superCall instanceof CertainCall) {
				this.certain = true;
				setSuperMethod(((CertainCall) superCall).getName(), true);
			} else if (superCall instanceof OptCall) {
				this.optional = true;
				setSuperMethod(((OptCall) superCall).getName(), true);
			} else if (superCall instanceof AltCall) {
				this.alternative = true;
				AltCall ac = (AltCall) superCall;
				setSuperMethod(ac.getName().getName(), true);
				for (AltCallChoice choice : ac.getA_name()) {
					setSuperMethod(choice.getName(), false);
				}
			}
		}
		private boolean setSuperMethod(SuperMethod sm, boolean setClassName) {
			if (sm instanceof Method) {
				Method m = (Method) sm;
//				methods.add(m);
				methodNames.add(m.getName());
				if (setClassName) {
					className = _getClassName(m, true);
				}
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean isCertain() {
			return certain;
		}
		@Override
		public boolean isOptional() {
			return optional;
		}
		@Override
		public boolean isAlternative() {
			return alternative;
		}
	}

	public static class BehaviorModel {
		private ArrayList<CallModel> callModels;

		public BehaviorModel() {
			this.callModels = new ArrayList<CallModel>();
		}
		public BehaviorModel(BehaviorModel bm) {
			this.callModels = new ArrayList<CallModel>(bm.callModels);
		}

		public BehaviorModel add(CallModel cm) {
			callModels.add(cm);
			return this;
		}

		public boolean sameBehavior(BehaviorModel bm) {
			if (callModels.size() != bm.callModels.size()) {
				return false;
			}
			for (int i = 0; i < callModels.size(); ++i) {
				if (!callModels.get(i).sameCall(bm.callModels.get(i))) {
					return false;
				}
			}
			return true;
		}
	}

	private String connectorName;
	private ArrayList<BehaviorModel> behaviorModels;

	private ConnectorTypeCheckModel(String connectorName,
			List<Behavior> behaviors) {
		this.connectorName = connectorName;
		this.behaviorModels = new ArrayList<BehaviorModel>();
		for (Behavior behavior : behaviors) {
			BehaviorModel bm = new BehaviorModel();
			for (Method m : behavior.getCall()) {
				bm.add(new MethodCallModel(m));
			}
			this.behaviorModels.add(bm);
		}
	}
	private ConnectorTypeCheckModel(String connectorName,
			List<UncertainBehavior> uBehaviors, boolean convertAsCertain) {
		this.connectorName = connectorName;
		this.behaviorModels = new ArrayList<BehaviorModel>();
		// TODO: convertAsCertain is ignored for now.
		for (UncertainBehavior uBehavior : uBehaviors) {
			BehaviorModel bm = new BehaviorModel();
			for (SuperCall sc : uBehavior.getCall()) {
				bm.add(new SuperCallModel(sc));
			}
			this.behaviorModels.add(bm);
		}
	}

	public String getConnectorName() {
		return connectorName;
	}
	public List<BehaviorModel> getBehaviorModels() {
		return behaviorModels;
	}

	public static List<ConnectorTypeCheckModel> getTypeCheckModel(Model model, boolean convertToCertain) {
		ArrayList<ConnectorTypeCheckModel> ctcModels = new ArrayList<ConnectorTypeCheckModel>();

		for (Connector connector : model.getConnectors()) {
			ctcModels.add(new ConnectorTypeCheckModel(connector.getName(),
					connector.getBehaviors()));
		}
		for (UncertainConnector uConnector : model.getU_connectors()) {
			ctcModels.add(new ConnectorTypeCheckModel(uConnector.getName(),
					uConnector.getU_behaviors(), convertToCertain));
		}
		ctcModels.add(new ConnectorTypeCheckModel("[DIRECT]", model.getBehaviors()));

		return ctcModels;
	}
}
