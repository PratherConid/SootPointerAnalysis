package pta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
		}
		for (boolean flag = true; flag; ) {
			flag = false;
			for (AssignConstraint ac : assignConstraintList) {
				if (!pts.containsKey(ac.from)) {
					continue;
				}	
				if (!pts.containsKey(ac.to)) {
					pts.put(ac.to, new TreeSet<Integer>());
				}
				if (pts.get(ac.to).addAll(pts.get(ac.from))) {
					flag = true;
				}
			}
		}
	}
	TreeSet<Integer> getPointsToSet(APointer str) {
		return pts.get(str);
	}
	
}
