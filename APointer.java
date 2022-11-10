package pta;

public class APointer {
	String name, field;
	// `indexlv` is used to distinguish generated allocId (for NewMultiArrayExpr)
	// We can't use "field" to store this because during the execution of
	// anderson, it is possible that there will be pointers with both nontrivial
	// `indexlv` and `field`
	int id, indexlv;
	String context;

	public APointer(String name_, String field_, String context_) {
		name = name_;
		field = field_;
		id = 0;
		indexlv = 0;
		context = context_;
	}

	public APointer(String name_, String field_, int indexlv_, String context_) {
		name = name_;
		field = field_;
		id = 0;
		indexlv = indexlv_;
		context = context_;
	}

	public APointer(int id_, String field_, String context_) {
		name = null;
		field = field_;
		id = id_;
		indexlv = 0;
		context = context_;
	}

	public APointer(int id_, String field_, int indexlv_, String context_) {
		name = null;
		field = field_;
		id = id_;
		indexlv = indexlv_;
		context = context_;
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
		return ret && id == that.id && indexlv == that.indexlv && context.equals(that.context);
	}

	public APointer deField() {
		if (name == null)
			return new APointer(id, null, indexlv, context);
		else
			return new APointer(name, null, indexlv, context);
	}

	public String hashString() {
		return id + "||" + name + "." + field + "[[" + indexlv + "]]" + "&" + context + "&";
	}

	@Override
	public int hashCode() {
		return hashString().hashCode();
	}

	@Override
	public String toString() {
		String indexlvexpr = "";
		if (indexlv != 0)
			indexlvexpr = "[[" + Integer.toString(indexlv) + "]]";
		if (name == null)
			return context + ": " + "Alloc_" + id + indexlvexpr;
		else if (field == null)
			return context + ": " + name + indexlvexpr;
		else
			return context + ": " + name + "." + field + indexlvexpr;
	}
}
