package pta;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import soot.Value;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.StaticFieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ArrayRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.jimple.toolkits.callgraph.Units;
import soot.jimple.InstanceInvokeExpr;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {

	String classpath;
	String classname;
	Anderson anderson;
	CallGraph cg;
	Set<SootMethod> visited = new HashSet<SootMethod>();
	Map<SootMethod, ArrayList<SootMethod>> edges = new HashMap<SootMethod, ArrayList<SootMethod>>();
	Map<SootMethod, ArrayList<SootMethod>> rvedges = new HashMap<SootMethod, ArrayList<SootMethod>>();
	ArrayList<Integer> allId = new ArrayList<Integer>();

	WholeProgramTransformer(String name) {
		classpath = name;
		String qualidIdent[] = classpath.split("\\.");
		System.out.println(classpath);
		classname = qualidIdent[qualidIdent.length - 1];
		System.out.println("Anderson Analysis for class: " +
				BashColor.ANSI_CYAN + classname + BashColor.ANSI_RESET +
				", classpath: " +
				BashColor.ANSI_CYAN + classpath + BashColor.ANSI_RESET);
	}

	private void processIdentityStmt(SootMethod sm, String ssm, Unit u) {
		Value l = ((IdentityStmt) u).getLeftOp();
		String sl = l.toString();
		int indexOfParameter = getIndexOfParameter(((IdentityStmt) u).getRightOp().toString());

		if (indexOfParameter <= -2)
			return;

		Iterator sources = new Units(cg.edgesInto(sm));
		Iterator methods = new Sources(cg.edgesInto(sm));

		while (sources.hasNext()) {
			Stmt src = (Stmt) sources.next();
			SootMethod callsm = (SootMethod) methods.next();
			String scallsm = callsm.toString();
			if (!visited.contains(callsm))
				continue;
			if (src == null)
				continue;
			if (!src.containsInvokeExpr())
				continue;
			String sSrc = src.toString();
			if (indexOfParameter == -1 && src.getInvokeExpr() instanceof InstanceInvokeExpr) {
				Value r = ((InstanceInvokeExpr) (src.getInvokeExpr())).getBase();
				String sr = r.toString();
				if (ssm.contains(classname) || ssm.contains("benchmark.objects.A")
						|| ssm.contains("benchmark.objects.B")) {
					System.out.println(BashColor.ANSI_BLUE +
							"1var: " + ssm + "||" + sl + BashColor.ANSI_RESET);
					System.out.println(BashColor.ANSI_YELLOW +
							"1context: " + scallsm + "||" + sSrc + BashColor.ANSI_RESET);
				}
				System.out
						.println(BashColor.ANSI_PURPLE + "ssm: " + ssm + " Callsm: " + scallsm + BashColor.ANSI_RESET);
				for (String con : anderson.getCons(scallsm)) {
					if (ssm.contains(classname) || ssm.contains("benchmark.objects.A")
							|| ssm.contains("benchmark.objects.B")) {
						System.out.println(BashColor.ANSI_BLUE +
								"2var: " + scallsm + "||" + sr + BashColor.ANSI_RESET);
						System.out.println(BashColor.ANSI_YELLOW +
								"2context: " + con + BashColor.ANSI_RESET);
					}
					anderson.addAssignConstraint(new APointer(scallsm + "||" + sr, null, con),
							new APointer(ssm + "||" + sl, null, scallsm + "||" + sSrc));
				}
				if (sm.toString().contains(classname)) { // debug
					System.out.println("Unit: " + u); // debug
					System.out.println("Possible source: " + src); // debug
					System.out.println("InvokeExpr @this: " + sr); // debug
				} // debug
			} else if (indexOfParameter != -1) {
				Value r = src.getInvokeExpr().getArg(indexOfParameter);
				String sr = r.toString();
				if (ssm.contains(classname) || ssm.contains("benchmark.objects.A")
						|| ssm.contains("benchmark.objects.B")) {
					System.out.println(BashColor.ANSI_BLUE +
							"3var: " + ssm + "||" + sl + BashColor.ANSI_RESET);
					System.out.println(BashColor.ANSI_YELLOW +
							"3context: " + scallsm + "||" + sSrc + BashColor.ANSI_RESET);
				}
				for (String con : anderson.getCons(scallsm)) {
					if (ssm.contains(classname) || ssm.contains("benchmark.objects.A")
							|| ssm.contains("benchmark.objects.B")) {
						System.out.println(BashColor.ANSI_BLUE +
								"4var: " + scallsm + "||" + sr + BashColor.ANSI_RESET);
						System.out.println(BashColor.ANSI_YELLOW +
								"4context: " + con + BashColor.ANSI_RESET);
					}
					anderson.addAssignConstraint(new APointer(scallsm + "||" + sr, null, con),
							new APointer(ssm + "||" + sl, null, scallsm + "||" + sSrc));
				}
				if (sm.toString().contains(classname)) { // debug
					System.out.println("Unit: " + u); // debug
					System.out.println("Possible source: " + src);
					System.out.println("Parameter" + indexOfParameter + ": " + sr); // debug
					System.out.println("LeftOp: " + sl); // debug
				} // debug
			}
		}
	}

	private void processReturnStmt(SootMethod sm, String ssm, Unit u) {
		Value op = ((ReturnStmt) u).getOp();
		String sop = op.toString();
		Iterator sources = new Units(cg.edgesInto(sm));
		Iterator methods = new Sources(cg.edgesInto(sm));
		while (sources.hasNext()) {
			Stmt src = (Stmt) sources.next();
			SootMethod callsm = (SootMethod) methods.next();
			String scallsm = callsm.toString();
			if (src == null) {
				continue;
			}
			if (!src.containsInvokeExpr()) {
				continue;
			}

			if (!visited.contains(callsm))
				continue;

			if (sm.toString().contains(classname))
				System.out.println("Src: " + src + "\n" + "Class: " + src.getClass());

			if (src instanceof AssignStmt) {
				String sSrc = src.toString();
				for (String con : anderson.getCons(scallsm)) {
					Value l = ((AssignStmt) src).getLeftOp();
					String sl = l.toString();
					anderson.addAssignConstraint(new APointer(ssm + "||" + sop, null, scallsm + "||" + sSrc),
							new APointer(scallsm + "||" + sl, null, con));
				}
			}
		}
	}

	private void processNewStmt(int allocId, String ssm, String sl, String context) {
		if (allocId != 0)
			System.out.println("Alloc: " + allocId + ", Lhs: " + sl);
		anderson.addNewConstraint(
				new APointer(allocId, null, "heap"),
				new APointer(ssm + "||" + sl, null, context));
	}

	private void processNewMultiStmt(int allocId, String ssm, String sl, Value r, String context) {
		if (allocId != 0)
			System.out.println("Alloc: " + allocId + ", Lhs: " + sl);
		anderson.addNewConstraint(
				new APointer(allocId, null, context),
				new APointer(ssm + "||" + sl, null, context));
		NewMultiArrayExpr nmr = (NewMultiArrayExpr) r;
		int dim = nmr.getSizeCount();
		for (int i = 1; i < dim; ++i) {
			// BenchmarkN.alloc(<allocId>[[i]]);
			// <lhs>[[i]] = new ...;
			anderson.addNewConstraint(
					new APointer(allocId, null, i, "heap"),
					new APointer(ssm + "||" + sl, null, i, context));
			// <lhs>[[i-1]][] = <lhs>[[i]];
			anderson.addAssignConstraint(
					new APointer(ssm + "||" + sl, null, i, context),
					new APointer(ssm + "||" + sl, "[]", i - 1, context));
		}
	}

	public void dfsMethods(SootMethod ssm) {
		if (visited.contains(ssm))
			return;
		visited.add(ssm);
		if (!edges.containsKey(ssm))
			return;
		ArrayList<SootMethod> arr = edges.get(ssm);
		for (SootMethod s : arr)
			dfsMethods(s);
	}

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {

		cg = Scene.v().getCallGraph();

		TreeMap<Integer, APointer> queries = new TreeMap<Integer, APointer>();
		anderson = new Anderson();

		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();

		SootMethod smain = null;
		// add edges to construct call graph
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			String ssm = sm.toString();

			if (ssm.contains("void main("))
				smain = sm;

			if (sm.hasActiveBody()) {
				Iterator methods = new Sources(cg.edgesInto(sm));
				while (methods.hasNext()) {
					SootMethod callsm = (SootMethod) methods.next();
					String scallsm = callsm.toString();
					if (!edges.containsKey(callsm))
						edges.put(callsm, new ArrayList<SootMethod>());
					if (scallsm.contains("void main(")) {
						System.out.println(BashColor.ANSI_RED + scallsm + BashColor.ANSI_RESET);
						System.out.println(BashColor.ANSI_YELLOW + ssm + BashColor.ANSI_RESET);
					}
					edges.get(callsm).add(sm);
				}
			}
		}
		// DFS to find all reachable methods
		dfsMethods(smain);
		for (SootMethod s : visited) {
			System.out.println(BashColor.ANSI_WHITE + s + BashColor.ANSI_RESET);
		}

		Map<SootMethod, ArrayList<SootMethod>> tmpedges = new HashMap<SootMethod, ArrayList<SootMethod>>();
		for (Entry<SootMethod, ArrayList<SootMethod>> ent : edges.entrySet()) {
			SootMethod caller = ent.getKey();
			if (!visited.contains(caller))
				continue;
			for (SootMethod callee : ent.getValue()) {
				if (!visited.contains(callee))
					continue;
				if (!tmpedges.containsKey(caller))
					tmpedges.put(caller, new ArrayList<SootMethod>());
				tmpedges.get(caller).add(callee);
				if (!rvedges.containsKey(callee))
					rvedges.put(callee, new ArrayList<SootMethod>());
				rvedges.get(callee).add(caller);
			}
		}
		edges = tmpedges;

		// Compute the possible context of all methods
		qr = reachableMethods.listener();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			String ssm = sm.toString();
			if (!visited.contains(sm))
				continue;
			if (sm.hasActiveBody()) {
				HashSet<String> hs = new HashSet<String>();
				anderson.cons.put(ssm, hs);
				Iterator sources = new Units(cg.edgesInto(sm));
				Iterator methods = new Sources(cg.edgesInto(sm));
				while (sources.hasNext()) {
					Stmt src = (Stmt) sources.next();
					SootMethod callsm = (SootMethod) methods.next();
					if (!visited.contains(callsm))
						continue;
					if (src == null)
						continue;
					if (!src.containsInvokeExpr())
						continue;
					hs.add(callsm.toString() + "||" + src.toString());
				}
				// hs.add("qwq");
				if (hs.isEmpty())
					hs.add("root");
			}
		}
		// Do the real calculation
		qr = reachableMethods.listener();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			String ssm = sm.toString();
			if (!visited.contains(sm))
				continue;
			// if (ssm.contains(classname)) { // debug
			// System.out.println(sm); // debug
			int allocId = 0;
			// List<Value> inspect = new ArrayList<Value>(); // debug
			if (sm.hasActiveBody()) {
				if (ssm.contains(classname))
					System.out.println(
							"Method: " + BashColor.ANSI_PURPLE + ssm + BashColor.ANSI_RESET);
				for (Unit u : sm.getActiveBody().getUnits()) {
					if (ssm.contains(classname)) {
						System.out.println("Statement: " +
								BashColor.ANSI_YELLOW + u + BashColor.ANSI_RESET +
								", " + u.getClass());
					}
					if (u instanceof InvokeStmt) {
						InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
						if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
							allocId = ((IntConstant) ie.getArgs().get(0)).value;
							allId.add(allocId);
						}
						if (ie.getMethod().toString()
								.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
							Value v = ie.getArgs().get(1);
							int id = ((IntConstant) ie.getArgs().get(0)).value;
							for (String con : anderson.getCons(ssm)) {
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + v.toString(), null, con),
										new APointer(ssm + "||" + v.toString(), null, "all"));
							}
							queries.put(id, new APointer(ssm + "||" + v.toString(), null, "all"));
						}
					}
					// DefinitionStmt -> IdentityStmt, AssignStmt
					if (u instanceof IdentityStmt) {
						processIdentityStmt(sm, ssm, u);
					}
					if (u instanceof AssignStmt) {
						AssignStmt du = (AssignStmt) u;
						Value l = du.getLeftOp();
						String sl = l.toString();
						Value r = du.getRightOp();
						String sr = r.toString();
						// if (sm.toString().contains(classname)) { // debug
						// System.out.println("Unit: " + u + ", Type: " + u.getClass()); // debug
						// System.out.println("LeftType: " + l.getClass() + // debug
						// ", RightType: " + r.getClass()); // debug
						// } // debug

						for (String con : anderson.getCons(ssm)) {
							if (r instanceof NewExpr || r instanceof NewArrayExpr) {
								processNewStmt(allocId, ssm, sl, con);
							} else if (r instanceof NewMultiArrayExpr) {
								processNewMultiStmt(allocId, ssm, sl, r, con);
							} else if (l instanceof Local && r instanceof Local) {
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + sr, null, con),
										new APointer(ssm + "||" + sl, null, con));
							} else if (l instanceof Local && r instanceof InstanceFieldRef) {
								InstanceFieldRef ir = (InstanceFieldRef) r;
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + ir.getBase().toString(), ir.getField().toString(),
												con),
										new APointer(ssm + "||" + sl, null, con));
							} else if (r instanceof Local && l instanceof InstanceFieldRef) {
								InstanceFieldRef il = (InstanceFieldRef) l;
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + sr, null, con),
										new APointer(ssm + "||" + il.getBase().toString(), il.getField().toString(),
												con));
							} else if (l instanceof Local && r instanceof StaticFieldRef) {
								StaticFieldRef ir = (StaticFieldRef) r;
								anderson.addAssignConstraint(
										new APointer(ir.getClass().toString() + "||" + ir.getField().toString(), null,
												"static"),
										new APointer(ssm + "||" + sl, null, con));
							} else if (r instanceof Local && l instanceof StaticFieldRef) {
								StaticFieldRef il = (StaticFieldRef) l;
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + sr, null, con),
										new APointer(il.getClass().toString() + "||" + il.getField().toString(), null,
												"static"));
							} else if (l instanceof Local && r instanceof ArrayRef) {
								// TODO: Global/Local ArrayRef??
								sr = ((ArrayRef) r).getBase().toString();
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + sr, "[]", con),
										new APointer(ssm + "||" + sl, null, con));
							} else if (r instanceof Local && l instanceof ArrayRef) {
								sl = ((ArrayRef) l).getBase().toString();
								anderson.addAssignConstraint(
										new APointer(ssm + "||" + sr, null, con),
										new APointer(ssm + "||" + sl, "[]", con));
							}
						}

					}
					if (u instanceof ReturnStmt) {
						processReturnStmt(sm, ssm, u);
					}
				}
			}
			// } // debug
		}

		anderson.run();
		String answer = "";
		for (Entry<Integer, APointer> q : queries.entrySet()) {
			HashSet<APointer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (result != null) {
				ArrayList<Integer> al = new ArrayList<>();
				for (APointer i : result) {
					if (i.id != 0)
						al.add(i.id);
				}
				if (al.isEmpty())
					al = allId;
				Collections.sort(al);
				for (Integer i : al)
					answer += " " + i;
				answer += "\n";
			}
		}
		AnswerPrinter.printAnswer(answer);

	}

	protected static int getIndexOfParameter(String expression) {
		if ((expression.substring(0, 5)).contains("@this")) {
			return -1;
		}
		if (!((expression.substring(0, 10)).contains("@parameter"))) {
			return -2;
		}
		int indexOfColon = expression.indexOf(":");
		assert (indexOfColon != -1);
		String index = expression.substring(10, indexOfColon);
		return Integer.parseInt(index);
	}

}
