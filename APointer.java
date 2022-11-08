package pta;

public class APointer {
	String name, field;
	// `indexlv` is used to distinguish generated allocId (for NewMultiArrayExpr)
	// We can't use "field" to store this because during the execution of
	// anderson, it is possible that there will be pointers with both nontrivial
	// `indexlv` and `field`
	int id, indexlv;

	public APointer(String name_, String field_) {
		name = name_;
		field = field_;
		id = 0;
		indexlv = 0;
	}

	public APointer(String name_, String field_, int indexlv_) {
		name = name_;
		field = field_;
		id = 0;
		indexlv = indexlv_;
	}

	public APointer(int id_, String field_) {
		name = null;
		field = field_;
		id = id_;
		indexlv = 0;
	}

	public APointer(int id_, String field_, int indexlv_) {
		name = null;
		field = field_;
		id = id_;
		indexlv = indexlv_;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		APointer that = (APointer) o;
		boolean ret = true;
		if (name != null && that.name != null)
			ret &= name.equals(that.name);
		else if (name == null ^ that.name == null)
			return false;
		if (field != null && that.field != null)
			ret &= field.equals(that.field);
		else if (field == null ^ that.field == null)
			return false;
		return ret && id == that.id && indexlv == that.indexlv;
	}

	public APointer deField() {
		if (name == null) return new APointer(id, null, indexlv);
		else return new APointer(name, null, indexlv);
	}

	@Override
	public int hashCode() {
		return (id + "||" + name + "." + field + "[[" + indexlv + "]]").hashCode();
	}

	@Override
	public String toString() {
		String indexlvexpr = "";
		if (indexlv != 0) indexlvexpr = "[[" + Integer.toString(indexlv) + "]]";
		if (name == null) return "Alloc_" + id + indexlvexpr;
		else if (field == null) return name + indexlvexpr;
		else return name + "." + field + indexlvexpr;
	}
}
