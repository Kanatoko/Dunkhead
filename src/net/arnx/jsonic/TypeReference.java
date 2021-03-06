package net.arnx.jsonic;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeReference<T> implements Type {
	public Type getType() {
		Type type = this.getClass().getGenericSuperclass();
		if (type instanceof ParameterizedType) {
			Type[] args = ((ParameterizedType)type).getActualTypeArguments();
			if (args != null && args.length == 1) {
				return args[0];
			}
		}
		throw new IllegalStateException("Reference must be specified actual type.");
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append("[").append(getType()).append("]");
		return sb.toString();
	}
}
