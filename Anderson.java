package pta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

class AssignConstraint {
	APointer from, to;

	AssignConstraint(APointer from, APointer to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public int hashCode() {
		return (from.hashString() + "?" + to.hashString()).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AssignConstraint that = (AssignConstraint) o;
		return from.equals(that.from) && to.equals(that.to);
	}
}

class NewConstraint {
	APointer to;
	APointer allocId;

	NewConstraint(APointer allocId, APointer to) {
		this.allocId = allocId;
		this.to = to;
	}

	@Override
	public int hashCode() {
		return (allocId.hashString() + "?" + to.hashString()).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		NewConstraint that = (NewConstraint) o;
		return allocId.equals(that.allocId) && to.equals(that.to);
	}
}

public class Anderson {
	public static boolean useContext = true;
	private ArrayList<AssignConstraint> assignConstraintList = new ArrayList<AssignConstraint>();
	private ArrayList<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();
	Map<APointer, HashSet<APointer>> pts = new HashMap<APointer, HashSet<APointer>>();
	Map<String, HashSet<String>> cons = new HashMap<String, HashSet<String>>();

	public boolean inPts(APointer thi) {
		return pts.containsKey(thi);
	}

	public boolean mergePts(APointer from, APointer to) {
		return pts.get(to).addAll(pts.get(from));
	}

	public boolean updateAssign(APointer from, APointer to) {
		// System.out.println("from: " + from + ", to:" + to +
		// ", fromSetDe: " + pts.get(from.deField()) +
		// ", toSetDe:" + pts.get(to.deField())); // debug
		if (!inPts(from.deField())) {
			pts.put(from.deField(), new HashSet<APointer>());
			return false;
		}
		if (!inPts(to.deField())) {
			pts.put(to.deField(), new HashSet<APointer>());
		}
		if (to.field == null && from.field == null) {
			return mergePts(from, to);
		} else if (to.field != null) {
			boolean flag = false;
			for (APointer l : pts.get(to.deField())) {
				APointer p = new APointer(l.id, to.field, l.indexlv, "heap");
				// System.out.println("from: " + from + ", p: " + p + ", fromSet: " +
				// pts.get(from) + ", pSet:" + pts.get(p)); // debug
				if (!inPts(p))
					pts.put(p, new HashSet<APointer>());
				flag |= mergePts(from, p);
			}
			return flag;
		} else if (from.field != null) {
			boolean flag = false;
			HashSet<APointer> tmpHS = (HashSet<APointer>) pts.get(from.deField()).clone();
			for (APointer r : tmpHS) {
				APointer q = new APointer((int) r.id, from.field, r.indexlv, "heap");
				// System.out.println("q: " + q + ", to: " + to + ", qSet: " + pts.get(q) + ",
				// toSet:" + pts.get(to)); // debug
				if (!inPts(q))
					pts.put(q, new HashSet<APointer>());
				flag |= mergePts(q, to);
			}
			return flag;
		}
		System.out.println(BashColor.ANSI_RED +
				"Anderson::updateAssign  Invalid Constraint, From = " + from +
				", To = " + to + BashColor.ANSI_RESET);
		return false;
	}

	void addAssignConstraint(APointer from, APointer to) {
		// if(from.name.contains("test.FieldSensitivity")||from.name.contains("benchmark.objects.A")
		// ||from.name.contains("benchmark.objects.B")) {
		// System.out.println(BashColor.ANSI_RED+
		// "Fvar: "+from.name +BashColor.ANSI_RESET);
		// System.out.println(BashColor.ANSI_GREEN+
		// "Fcontext: "+from.context+BashColor.ANSI_RESET);
		// }
		// if(to.name.contains("test.FieldSensitivity")||to.name.contains("benchmark.objects.A")
		// ||to.name.contains("benchmark.objects.B")) {
		// System.out.println(BashColor.ANSI_RED+
		// "Tvar: "+to.name +BashColor.ANSI_RESET);
		// System.out.println(BashColor.ANSI_GREEN+
		// "Tcontext: "+to.context+BashColor.ANSI_RESET);
		// }
		assignConstraintList.add(new AssignConstraint(from, to));
	}

	void addNewConstraint(APointer alloc, APointer to) {
		newConstraintList.add(new NewConstraint(alloc, to));
	}

	void run() {
		System.out.println(BashColor.ANSI_YELLOW +
				"Number of new constraints: " + newConstraintList.size() +
				BashColor.ANSI_RESET);

		System.out.println(BashColor.ANSI_YELLOW +
				"Number of assign constraints: " + assignConstraintList.size() +
				BashColor.ANSI_RESET);

		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new HashSet<APointer>());
			}
			pts.get(nc.to).add(nc.allocId);
		}
		for (boolean flag = true; flag;) {
			flag = false;
			for (AssignConstraint ac : assignConstraintList) {
				flag |= updateAssign(ac.from, ac.to);
			}
		}
	}

	HashSet<APointer> getPointsToSet(APointer str) {
		return pts.get(str);
	}

	boolean isEmptyCons(String s) {
		if (!cons.containsKey(s))
			return true;
		return cons.get(s).isEmpty();
	}

	void addCons(String s, String c) {
		if (!useContext)
			c = "qwq";
		if (!cons.containsKey(s)) {
			cons.put(s, new HashSet<String>());
		}
		cons.get(s).add(c);
	}

	HashSet<String> getCons(String s) {
		return cons.get(s);
	}

}
