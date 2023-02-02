/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.plugin.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.checker.Nullable;

public class Reflection {

    private static final Logger logger = Logger.getLogger(Reflection.class);

    private Reflection() {}

    public static Class<?> getClass(String name, ClassLoader loader) {
        return getClass(name, loader, false);
    }

    public static Class<?> getClassWithWarnIfNotFound(String name,
            ClassLoader loader) {
        return getClass(name, loader, true);
    }

    public static Method getMethod(Class<?> clazz, String methodName,
            Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            logger.debug(e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            return null;
        }
    }

    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            logger.debug(e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> /*@Nullable*/ T invoke(Method method, Object obj,
            Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return (T) method.invoke(obj, args);
        } catch (Throwable t) {
            logger.warn("error calling {}.{}()", method.getDeclaringClass().getName(),
                    method.getName(), t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeWithDefault(Method method, Object obj, T defaultValue,
            Object... args) {
        if (method == null) {
            return defaultValue;
        }
        try {
            Object value = method.invoke(obj, args);
            if (value == null) {
                return defaultValue;
            }
            return (T) value;
        } catch (Throwable t) {
            logger.warn("error calling {}.{}()", method.getDeclaringClass().getName(),
                    method.getName(), t);
            return defaultValue;
        }
    }

    public static Object getFieldValue(Field field, Object obj) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(obj);
        } catch (Throwable t) {
            logger.warn("error getting {}.{}()", field.getDeclaringClass().getName(),
                    field.getName(), t);
            return null;
        }
    }

    private static Class<?> getClass(String name, ClassLoader loader,
            boolean warnIfNotFound) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            if (warnIfNotFound) {
                logger.warn(e.getMessage(), e);
            } else {
                logger.debug(e.getMessage(), e);
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return null;
    }
}
