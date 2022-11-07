package pta;

import org.jf.dexlib2.dexbacked.raw.HeaderItem;

public class HeapPointer {
	int id;
	String field;

	public HeapPointer() {
	}

	public HeapPointer(int a, String s) {
		id = a;
		field = s;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HeapPointer that = (HeapPointer) o;
		return id == that.id && field.equals(that.field);
	}

	@Override
	public int hashCode() {
		return (id + "." + field).hashCode();
	}
}
