package pta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

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
	Map<APointer, HashSet<APointer>> pts = new HashMap<APointer, HashSet<APointer>>();

	public boolean inPts(APointer thi) {
		return pts.containsKey(thi);
	}

	public boolean mergePts(APointer from, APointer to) {
		return pts.get(to).addAll(pts.get(from));
	}

	public boolean updateAssign(APointer from, APointer to) {
		System.out.println("from: " + from + ", to:" + to +
		                   ", fromSetDe: " + pts.get(from.deField()) +
						   ", toSetDe:" + pts.get(to.deField())); // debug
		if (!inPts(from.deField())) {
			return false; // under discussion
		}
		if (!inPts(to.deField())) {
			pts.put(to.deField(), new HashSet<APointer>());
		}
		if (to.field == null && from.field == null) {
			return mergePts(from, to);
		} else if (to.field != null) {
			boolean flag = false;
			for (APointer l : pts.get(to.deField())) {
				APointer p = new APointer(l.id, to.field, l.indexlv);
				System.out.println("from: " + from + ", p:" + p + ", fromSet: " + pts.get(from) + ", pSet:" + pts.get(p)); // debug
				if (!inPts(p))
					pts.put(p, new HashSet<APointer>());
				flag |= mergePts(from, p);
			}
			return flag;
		} else if (from.field != null) {
			boolean flag = false;
			for (APointer r : pts.get(from.deField())) {
				APointer q = new APointer((int) r.id, from.field, r.indexlv);
				System.out.println("q: " + q + ", to:" + to + ", qSet: " + pts.get(q) + ", toSet:" + pts.get(to)); // debug
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

}
