package pta;
import java.util.Set;
import java.util.TreeSet;

public class APointer {
	String name, field;
	int id;

	public APointer(String name_, String field_) {
		name = name_;
		field = field_;
		id = 0;
	}

	public APointer(int id_, String field_) {
		name = null;
		field = field_;
		id = id_;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		APointer that = (APointer) o;
		return name.equals(that.name) && field.equals(that.field) && id == that.id;
	}

	@Override
	public int hashCode() {
		return (id + "||" + name + "." + field).hashCode();
	}

	public boolean mergePts(APointer o) {
		Set<Integer> pa=Anderson.pts.get(this);
		Set<Integer> pb=Anderson.pts.get(o);
		return pa.addAll(pb);
	}

	public boolean inPts() {
		return Anderson.pts.containsKey(this);
	}

	public boolean updateAssign(APointer o) {
		if(!o.inPts()) {
			return true;
		}
		if(!this.inPts()) {
			Anderson.pts.put(this,new TreeSet<>());
		}
		if(this.field==null&&o.field==null) {
			return this.mergePts(o);
		} else if(this.field!=null) {
			boolean flag=false;
			for(Integer l:Anderson.pts.get(this))
			{
				APointer p=new APointer((int)l,this.field);
				if(!p.inPts())
					Anderson.pts.put(p,new TreeSet<>());
				flag|=p.mergePts(o);
			}
			return flag;
		} else if(o.field!=null) {
			boolean flag=false;
			for(Integer r:Anderson.pts.get(o)) {
				APointer q=new APointer((int)r,o.field);
				if(!q.inPts()) {
					flag=true;
					continue;
				}
				flag|=this.mergePts(q);
			}
			return flag;
		}
		assert(false);// fuck !
		return false;
	}
}
