package pta;

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
		boolean ret = true;
		if (name != null && that.name != null)
			ret &= name.equals(that.name);
		else if (name == null ^ that.name == null)
			return false;
		if (field != null && that.field != null)
			ret &= field.equals(that.field);
		else if (field == null ^ that.field == null)
			return false;
		return ret && id == that.id;
	}

	@Override
	public int hashCode() {
		return (id + "||" + name + "." + field).hashCode();
	}

	@Override
	public String toString() {
		if (name == null) return "Alloc_" + id;
		else if (field == null) return name;
		else return name + "." + field;
	}
}
