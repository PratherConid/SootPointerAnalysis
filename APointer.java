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
		return name.equals(that.name) && field.equals(that.field) && id == that.id;
	}

	@Override
	public int hashCode() {
		return (id + "||" + name + "." + field).hashCode();
	}
}
