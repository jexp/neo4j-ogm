package org.neo4j.ogm.entityaccess;

import org.neo4j.ogm.strategy.simple.MethodCache;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public class SetterEntityAccess implements EntityAccess {

    private static final MethodCache methodCache = new MethodCache();
    private static final Map<Class, Class> primitives = new HashMap();

    static {
        primitives.put(Character.class, char.class);
        primitives.put(Byte.class, byte.class);
        primitives.put(Short.class, short.class);
        primitives.put(Integer.class, int.class);
        primitives.put(Long.class, long.class);
        primitives.put(Float.class, float.class);
        primitives.put(Double.class, double.class);
        primitives.put(Boolean.class, boolean.class);

    }
    private final String methodName;

    private SetterEntityAccess(String methodName) {
        this.methodName = methodName;
    }

    public static SetterEntityAccess forProperty(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("set");
        if (name != null && name.length() > 0) {
            sb.append(name.substring(0,1).toUpperCase());
            sb.append(name.substring(1));
            return new SetterEntityAccess(sb.toString());
        } else {
            return null;
        }
    }

    @Override
    public void set(Object instance, Object any) throws Exception {
        if (Iterable.class.isAssignableFrom(any.getClass())) {
            setIterable(instance, (Iterable<?>) any);
        } else {
            setValue(instance, any);
        }
    }

    @Override
    public void setValue(Object instance, Object parameter) throws Exception {
        Method method=findSetter(instance, parameter, methodName);
        method.invoke(instance, parameter);
    }

    @Override
    public void setIterable(Object instance, Iterable<?> collection) throws Exception {
        Object typeInstance = collection;
        if (Collection.class.isAssignableFrom(collection.getClass())) {
            typeInstance=collection.iterator().next();
        }
        Method method = findParameterisedSetter(instance, typeInstance, methodName);
        method.invoke(instance, cast(method, collection));
    }

    private static Method findSetter(Object instance, Object parameter, String methodName) throws NoSuchMethodException {
        //System.out.println("Looking for method " + methodName + "(" + parameter.getClass().getSimpleName() + ") in class " + instance.getClass().getName());
        Class<?> clazz = instance.getClass();
        Method m = methodCache.lookup(clazz, methodName);
        if (m == null) {
            for (Method method : clazz.getMethods()) {
                if( Modifier.isPublic(method.getModifiers()) &&
                        method.getReturnType().equals(void.class) &&
                        method.getName().startsWith(methodName) &&
                        method.getParameterTypes().length == 1 &&
                        (method.getParameterTypes()[0] == parameter.getClass() || method.getParameterTypes()[0].isAssignableFrom(primitive(parameter.getClass())))) {
                    return methodCache.insert(clazz, methodName, method);
                }
            }
        } else {
            return m;
        }
        throw new NoSuchMethodException("Cannot find method " + methodName + "(" + parameter.getClass().getSimpleName() + ") in class " + instance.getClass().getName());
    }

    private static Class primitive(Class clazz) {
        if (clazz == Integer.class) {
            return int.class;
        }
        if (clazz == Long.class) {
            return long.class;
        }
        if (clazz == Short.class) {
            return short.class;
        }
        if (clazz == Byte.class) {
            return byte.class;
        }
        if (clazz == Float.class) {
            return float.class;
        }
        if (clazz == Double.class) {
            return double.class;
        }
        if (clazz == Character.class) {
            return char.class;
        }
        if (clazz == Boolean.class) {
            return boolean.class;
        }
        return clazz;
    }

    private static Method findParameterisedSetter(Object instance, Object type, String methodName) throws NoSuchMethodException {
        //System.out.println("Looking for method " + methodName + "?(Iterable<T>) in class " + instance.getClass().getName());
        Class<?> clazz = instance.getClass();
        Method method = methodCache.lookup(clazz, methodName + "?"); // ? indicates a non-scalar
        if (method == null) {
            for (Method m : instance.getClass().getMethods()) {
                if (Modifier.isPublic(m.getModifiers()) &&
                        m.getReturnType().equals(void.class) &&
                        m.getName().startsWith(methodName) &&
                        m.getParameterTypes().length == 1 &&
                        m.getGenericParameterTypes().length == 1) {
                    Type t = m.getGenericParameterTypes()[0];
                    if (t.toString().contains(type.getClass().getName())) {
                        return methodCache.insert(clazz, methodName + "?", m);
                    }
                }
            }
        }  else {
            return method;
        }
        throw new NoSuchMethodException("Cannot find method " + methodName + "?(Iterable<T>) in class " + instance.getClass().getName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object cast(Method method, Iterable<?> collection) throws Exception {

        // basic "collection" types we will handle: List<T>, Set<T>, Vector<T>, T[]
        Class parameterType = method.getParameterTypes()[0];

        if (parameterType == List.class) {
            List<Object> list = new ArrayList<>();
            list.addAll((Collection)collection);
            return list;
        }

        else if (parameterType == Set.class) {
            Set<Object> set = new HashSet<>();
            set.addAll((Collection) collection);
            return set;
        }

        else if (parameterType == Vector.class) {
            Vector<Object> v = new Vector<>();
            v.addAll((Collection) collection);
            return v;
        }

        else if (parameterType.isArray()) {
            Class type = parameterType.getComponentType();
            Object array = Array.newInstance(type, ((Collection) collection).size());
            List<Object> objects = new ArrayList<>();
            objects.addAll((Collection) collection);
            for (int i = 0; i < objects.size(); i++) {
                Array.set(array, i, objects.get(i));
            }
            return array;
        }

        else {
            throw new RuntimeException("Unsupported: " + parameterType.getName());
        }
    }
}
