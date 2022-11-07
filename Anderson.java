package pta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

class AssignConstraint {
	APointer from, to;

	AssignConstraint(APointer from, APointer to) {
		this.from = from;
		this.to = to;
	}
}

class NewConstraint {
	APointer to;
	APointer allocId;

	NewConstraint(APointer allocId, APointer to) {
		this.allocId = allocId;
		this.to = to;
	}
}

public class Anderson {
	private List<AssignConstraint> assignConstraintList = new ArrayList<AssignConstraint>();
	private List<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();
	Map<APointer, TreeSet<Integer>> pts = new HashMap<APointer, TreeSet<Integer>>();

	public boolean inPts(APointer thi) {
		return pts.containsKey(thi);
	}

	public boolean mergePts(APointer from, APointer to) {
		Set<Integer> pfrom = pts.get(from);
		Set<Integer> pto = pts.get(to);
		return pto.addAll(pfrom);
	}

	public boolean updateAssign(APointer from, APointer to) {
		// System.out.println("from: " + from + ", to:" + to + ", fromSet: " + pts.get(from) + ", toSet:" + pts.get(to)); // debug
		if (!inPts(from)) {
			return true;
		}
		if (!inPts(to)) {
			pts.put(to, new TreeSet<>());
		}
		if (to.field == null && from.field == null) {
			return mergePts(to, from);
		} else if (to.field != null) {
			boolean flag = false;
			for (Integer l : pts.get(to)) {
				APointer p = new APointer((int) l, to.field);
				if (!inPts(p))
					pts.put(p, new TreeSet<>());
				flag |= mergePts(from, p);
			}
			return flag;
		} else if (from.field != null) {
			boolean flag = false;
			for (Integer r : pts.get(from)) {
				APointer q = new APointer((int) r, from.field);
				assert inPts(q) : "Anderson::updateAssign  This pointer " + q + " is not in \"pts\" of Anderson";
				flag |= mergePts(q, to);
			}
			return flag;
		}
		assert false : "Anderson::updateAssign  Invalid Constraint, From = " + from + ", To = " + to;
		return false;
	}

	void addAssignConstraint(APointer from, APointer to) {
		assignConstraintList.add(new AssignConstraint(from, to));
	}

	void addNewConstraint(APointer alloc, APointer to) {
		newConstraintList.add(new NewConstraint(alloc, to));
	}

	void run() {
		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new TreeSet<Integer>());
			}
			pts.get(nc.to).add(nc.allocId.id);
			// System.out.println(pts); // debug
		}
		for (boolean flag = true; flag;) {
			flag = false;
			for (AssignConstraint ac : assignConstraintList) {
				flag |= updateAssign(ac.from, ac.to);
			}
		}
	}

	TreeSet<Integer> getPointsToSet(APointer str) {
		return pts.get(str);
	}

}
