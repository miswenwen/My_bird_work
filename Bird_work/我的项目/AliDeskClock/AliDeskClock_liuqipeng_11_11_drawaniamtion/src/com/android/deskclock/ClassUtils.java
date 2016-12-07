/**
 * @(#)ClassProxyUtil.java    v1.0 2012-12-23
 *
 * Copyright (c) 2012-2012  yunos, Inc.
 * 2 zijinghua Road, HangZhou, C.N
 * All rights reserved.
 */
package com.android.deskclock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 */
public class ClassUtils {
    public static Object invokeMethod(Object object, String methodName) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName);
                return method.invoke(object);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static Object invokeMethod(Object object, String methodName,
            Class param0,
            Object arg0) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName, param0);
                return method.invoke(object, arg0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static Object invokeMethod(Object object, String methodName,
            Class param0, Class param1,
            Object arg0,Object arg1) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName, param0, param1);
                return method.invoke(object, arg0, arg1);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static Object invokeMethod(Object object, String methodName,
            Class param0, Class param1, Class param2,
            Object arg0,Object arg1, Object arg2) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName, param0, param1, param2);
                return method.invoke(object, arg0, arg1, arg2);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static Object invokeMethod(Object object, String methodName,
            Class param0, Class param1, Class param2, Class param3,
            Object arg0, Object arg1, Object arg2,Object arg3) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName, param0, param1, param2, param3);
                return method.invoke(object, arg0, arg1, arg2, arg3);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static Object invokeMethod(Object object, String methodName,
            Class param0, Class param1, Class param2, Class param3, Class param4,
            Object arg0,Object arg1, Object arg2,Object arg3, Object arg4) {
        if (null != object) {
            try {
                Class clz = (object instanceof Class ? (Class) object : object.getClass());
                Method method = getMethod(clz, methodName, param0, param1, param2, param3, param4);
                return method.invoke(object, arg0, arg1, arg2, arg3, arg4);
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * @param clzName class name
     * @return the Class object if the class is exists, else null
     */
    public static Class loadClass(String clzName) {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(clzName);
        } catch (Exception e) {
        }
        return null;
    }

    public static final int TYPE_OBJECT = 0;
    public static final int TYPE_INT = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_BYTE = 3;
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_FLOAT = 5;
    public static final int TYPE_LONG = 6;
    public static final int TYPE_DOUBLE = 7;

    public static Object getStaticField(Class clz, String fieldName, int type) {
        if (null != clz) {
            try {
                Field field = clz.getField(fieldName);
                switch (type) {
                case TYPE_OBJECT:
                    return field.get(clz);
                case TYPE_INT:
                    return field.getInt(clz);
                case TYPE_SHORT:
                    return field.getShort(clz);
                case TYPE_BYTE:
                    return field.getByte(clz);
                case TYPE_BOOLEAN:
                    return field.getBoolean(clz);
                case TYPE_FLOAT:
                    return field.getFloat(clz);
                case TYPE_LONG:
                    return field.getLong(clz);
                case TYPE_DOUBLE:
                    return field.getDouble(clz);
                default:
                    return field.get(clz);
                }
            } catch (Exception e) {
            }
            return (clz == Object.class ? getDefault(type) : getStaticField(
                    clz.getSuperclass(), fieldName, type));
        }
        return getDefault(type);
    }

    private static Object getDefault(int type) {
        switch (type) {
        case TYPE_OBJECT:
            return "";
        case TYPE_INT:
        case TYPE_SHORT:
        case TYPE_BYTE:
        case TYPE_BOOLEAN:
        case TYPE_FLOAT:
        case TYPE_LONG:
        case TYPE_DOUBLE:
            return 0;
        default:
            return "";
        }
    }

    /**
     * @param clz class name
     * @param fieldName field name
     * @return the Field object if the class is exists, else null
     */
    public static Field getField(Class clz, String fieldName) {
        if (null != clz) {
            try {
                return clz.getField(fieldName);
            } catch (Exception e) {
            }
            return (clz == Object.class ? null : getField(clz.getSuperclass(), fieldName));
        }
        return null;
    }
    /**
     * @param clz class name
     * @param methodName method name
     * @return true if the method is in the class, else false
     */
    public static Method getMethod(Class clz, String methodName, Class ...parameterTypes) {
        if (null != clz) {
            try {
                return clz.getMethod(methodName, parameterTypes);
            } catch (Exception e) {
            }
            return (clz == Object.class ? null : getMethod(clz.getSuperclass(), methodName, parameterTypes));
        }
        return null;
    }

    /**
     * @param clz class name
     * @param fieldName field name
     * @return true if the field is in the class, else false
     */
    public static boolean existsField(Class clz, String fieldName) {
        if (null != clz) {
            try {
                return clz.getDeclaredField(fieldName) != null;
            } catch (Exception e) {
            }
            if (clz != Object.class) {
                return existsField(clz.getSuperclass(), fieldName);
            }
        }
        return false;
    }

    /**
     * @param clz class name
     * @param funcName function name
     * @return true if the function is in the class, else false
     */
    public static boolean existsFunc(Class clz, String funcName, Class parameterTypes) {
        if (null != clz) {
            try {
                return clz.getDeclaredMethod(funcName, parameterTypes) != null;
            } catch (Exception e) {
            }
            if (clz != Object.class) {
                return existsFunc(clz.getSuperclass(), funcName, parameterTypes);
            }
        }
        return false;
    }
}
