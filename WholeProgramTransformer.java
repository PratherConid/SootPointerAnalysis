package pta;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;

import soot.Value;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.ValueBox;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
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

	WholeProgramTransformer(String name) {
		classpath = name;
		String qualidIdent[] = classpath.split("\\.");
		System.out.println(classpath);
		classname = qualidIdent[qualidIdent.length - 1];
		System.out.println("Anderson Analysis for class: " +
							BashColor.ANSI_CYAN + classname + BashColor.ANSI_RESET +
		                    ", classpath: " +
							BashColor.ANSI_CYAN + classpath + BashColor.ANSI_RESET +
							"");
	}

	void processIdentityStmt(Anderson anderson, SootMethod sm, String ssm, Unit u, CallGraph cg) {
		Value l = ((IdentityStmt) u).getLeftOp();
		String sl = l.toString();
		int indexOfParameter = getIndexOfParameter(((IdentityStmt) u).getRightOp().toString());

		if (indexOfParameter <= -2) return;

		Iterator sources = new Units(cg.edgesInto(sm));
		Iterator methods = new Sources(cg.edgesInto(sm));

		while (sources.hasNext()) {
			Stmt src = (Stmt) sources.next();
			SootMethod callsm = (SootMethod) methods.next();
			String scallsm = callsm.toString();
			if (src == null)
				continue;
			if (!src.containsInvokeExpr())
				continue;
			if (indexOfParameter == -1 && src.getInvokeExpr() instanceof InstanceInvokeExpr) {
				Value r = ((InstanceInvokeExpr) (src.getInvokeExpr())).getBase();
				String sr = r.toString();
				anderson.addAssignConstraint(scallsm + "||" + sr, ssm + "||" + sl);
				anderson.addAssignConstraint(ssm + "||" + sl, scallsm + "||" + sr);
				if (sm.toString().contains(classname)) { // debug
					System.out.println("Unit: " + u); // debug
					System.out.println("Possible source: " + src);
					System.out.println("InvokeExpr @this: " + sr); // debug
				} // debug
			} else if (indexOfParameter != -1) {
				Value r = src.getInvokeExpr().getArg(indexOfParameter);
				String sr = r.toString();
				anderson.addAssignConstraint(scallsm + "||" + sr, ssm + "||" + sl);
				anderson.addAssignConstraint(ssm + "||" + sl, scallsm + "||" + sr);
				if (sm.toString().contains(classname)) { // debug
					System.out.println("Unit: " + u); // debug
					System.out.println("Possible source: " + src);
					System.out.println("Parameter" + indexOfParameter + ": " + sr); // debug
					System.out.println("LeftOp: " + sl); // debug
				} // debug
			}
		}
	}

	void processReturnStmt(Anderson anderson, SootMethod sm, String ssm, Unit u, CallGraph cg) {
		Value op = ((ReturnStmt) u).getOp(); String sop = op.toString();
		Iterator sources = new Units(cg.edgesInto(sm));
		Iterator methods = new Sources(cg.edgesInto(sm));
		while (sources.hasNext()) {
			Stmt src = (Stmt)sources.next();
			SootMethod callsm = (SootMethod)methods.next();
			String scallsm = callsm.toString();
			if (src == null) {
				continue;
			}
			if (!src.containsInvokeExpr()) {
				continue;
			}

			System.out.println("Src: " + src + "\n" + "Class: " + src.getClass());

			if (src instanceof AssignStmt) {
				Value l = ((AssignStmt) src).getLeftOp(); String sl = l.toString();
				anderson.addAssignConstraint(ssm + "||" + sop, scallsm + "||" + sl);
			}
		}
	}

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {

		CallGraph cg = Scene.v().getCallGraph();

		TreeMap<Integer, String> queries = new TreeMap<Integer, String>();
		Anderson anderson = new Anderson();

		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			String ssm = sm.toString();
			// if (ssm.contains(classname)) { // debug
				// System.out.println(sm); // debug
				int allocId = 0;
				// List<Value> inspect = new ArrayList<Value>(); // debug
				if (sm.hasActiveBody()) {
					if (ssm.contains(classname)) System.out.println(
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
							}
							if (ie.getMethod().toString()
									.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
								Value v = ie.getArgs().get(1);
								int id = ((IntConstant) ie.getArgs().get(0)).value;
								queries.put(id, ssm + "||" + v.toString());
							}
						}
						// DefinitionStmt -> IdentityStmt, AssignStmt
						if (u instanceof IdentityStmt) {
							// processIdentityStmt(anderson, sm, ssm, u, cg);
						}
						if (u instanceof AssignStmt) {
							AssignStmt du = (AssignStmt)u;
							Value l = du.getLeftOp();
							String sl = l.toString();
							Value r = du.getRightOp();
							String sr = r.toString();
							// if (sm.toString().contains(classname)) { // debug
							// System.out.println("Unit: " + u + ", Type: " + u.getClass()); // debug
							// System.out.println("LeftType: " + l.getClass() + // debug
							//                    ", RightType: " + r.getClass()); // debug
							// } // debug
							if (r instanceof NewExpr) {
								if (allocId != 0)
									System.out.println("Alloc: " + allocId + ", Lhs: " + l);
								anderson.addNewConstraint(allocId, ssm + "||" + sl);
							} else if ((l instanceof Local || l instanceof InstanceFieldRef) &&
									(r instanceof Local || r instanceof InstanceFieldRef)) {
								anderson.addAssignConstraint(ssm + "||" + sr, ssm + "||" + sl);
							} else if ((l instanceof Local && r instanceof StaticFieldRef) ||
									(r instanceof Local && l instanceof StaticFieldRef)) {
								anderson.addAssignConstraint(sr, sl);
							} else if (l instanceof Local && r instanceof ArrayRef) {
								// TODO: Global/Local ArrayRef??
								sr = ((ArrayRef) r).getBase().toString() + ".[]";
								anderson.addAssignConstraint(ssm + "||" + sr, ssm + "||" + sl);
							} else if (r instanceof Local && l instanceof ArrayRef) {
								sl = ((ArrayRef) l).getBase().toString() + ".[]";
								anderson.addAssignConstraint(ssm + "||" + sr, ssm + "||" + sl);
							}
						}
						if (u instanceof ReturnStmt) {
							// processReturnStmt(anderson, sm, ssm, u, cg);
						}
					}
				}
			// } // debug
		}

		anderson.run();
		String answer = "";
		for (Entry<Integer, String> q : queries.entrySet()) {
			TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (result != null) {
				for (Integer i : result) {
					answer += " " + i;
				}
			}
			answer += "\n";
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
