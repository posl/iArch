package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.checkplugin.model.Counterexample;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;

public class AnalyzeCounterexample {

	public class DubiousPoint implements Comparable<DubiousPoint> {
		public EObject element; // Behavior|UncertainBehavior
		public float score;
		public Set<EObject> points; // Method

		public DubiousPoint(EObject element) {
			this.element = element;
			score = 0f;
			points = new HashSet<EObject>();
		}

		public void add(EObject point) {
			score += 1f;
			points.add(point);
		}

		@Override
		public int compareTo(DubiousPoint arg0) {
			float diff = score - arg0.score;
			// Descending order.
			return diff == 0f ? 0 : (diff > 0f ? -1 : 1);
		}
	}

	public List<DubiousPoint> analyze(Model model, String output) {
		Counterexample counterexample = Counterexample.create(output);
		if (counterexample == null) {
			return Collections.emptyList();
		}
		return findDubiousPoints(model, counterexample);
	}

	private List<DubiousPoint> findDubiousPoints(Model model, Counterexample counterexample) {
		Map<EObject, DubiousPoint> pointMap = new HashMap<EObject, DubiousPoint>();

		for (String[] action : counterexample.getActions()) {
			if (action.length == 0) {
				continue;
			}

			// 1. find Interface/UncertainInterface which may contain the action as Method.
			List<Interface> cInterfaces = null;
			List<UncertainInterface> uInterfaces = null;
			if (action.length > 1) {
				// filter by name
				String ifName = action[action.length - 2];
				// variable name starts with "_".
				if (ifName.startsWith("_")) {
					ifName = ifName.substring(1);
				}

				Interface cInterface = ArchModelUtils.findInterfaceByName(model, ifName);
				if (cInterface != null) {
					cInterfaces = Arrays.asList(cInterface);
					uInterfaces = ArchModelUtils.searchUncertainInterfaceBySuperName(model, ifName);
				} else {
					UncertainInterface uInterface = ArchModelUtils.findUncertainInterfaceByName(model, ifName);
					if (uInterface != null) {
						uInterfaces = Arrays.asList(uInterface);
						Interface superInterface = uInterface.getSuperInterface();
						if (superInterface != null) {
							cInterfaces = Arrays.asList(superInterface);
						} else {
							cInterfaces = Collections.emptyList();
						}
					}
				}
			}
			// otherwise, search all Interface/UncertainInterface
			if (cInterfaces == null) {
				cInterfaces = model.getInterfaces();
			}
			if (uInterfaces == null) {
				uInterfaces = model.getU_interfaces();
			}

			// 2. find a Method(s) which may correspond to the action.
			String methodName = action[action.length - 1];
			List<Method> actionMethods = new ArrayList<Method>();
			for (Interface cInterface : cInterfaces) {
				Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
				if (method != null) {
					actionMethods.add(method);
				}
			}
			for (UncertainInterface uInterface : uInterfaces) {
				Method method = ArchModelUtils.findMethodByName(uInterface, methodName);
				if (method != null) {
					actionMethods.add(method);
				}
			}
			if (actionMethods.isEmpty()) {
				// Something wrong but continue.
				continue;
			}

			// 3. find a Behavior/UncertainBehavior which uses this Method.
			for (Connector connector : model.getConnectors()) {
				for (Behavior behavior : connector.getBehaviors()) {
					for (Method method : actionMethods) {
						if (ArchModelUtils.containsMethod(behavior, method)) {
							DubiousPoint dp = pointMap.get(behavior);
							if (dp == null) {
								dp = new DubiousPoint(behavior);
								pointMap.put(behavior, dp);
							}
							dp.add(method);
							break;
						}
					}
				}
			}
			for (UncertainConnector uConnector : model.getU_connectors()) {
				for (UncertainBehavior uBehavior : uConnector.getU_behaviors()) {
					for (Method method : actionMethods) {
						if (ArchModelUtils.containsMethod(uBehavior, method)) {
							DubiousPoint dp = pointMap.get(uBehavior);
							if (dp == null) {
								dp = new DubiousPoint(uBehavior);
								pointMap.put(uBehavior, dp);
							}
							dp.add(method);
							break;
						}
					}
				}
			}
		}

		List<DubiousPoint> points = new ArrayList<DubiousPoint>(pointMap.values());
		Collections.sort(points);
		return points;
	}

}
