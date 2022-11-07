package pta;

public class FieldPointer {
	String name, field;

	public FieldPointer() {
	}

	public FieldPointer(String a, String b) {
		name = a;
		field = b;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FieldPointer that = (FieldPointer) o;
		return name.equals(that.name) && field.equals(that.field);
	}

	@Override
	public int hashCode() {
		return (name + "." + field).hashCode();
	}
}
