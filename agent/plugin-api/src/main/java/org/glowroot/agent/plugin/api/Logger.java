/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, @Nullable Version 2.0 (@Nullable the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, @Nullable software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, @Nullable either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.plugin.api;

import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.internal.LoggerFactory;

/**
 * Very thin wrapper around SLF4J so plugins don't have to worry about SLF4J shading.
 */
public abstract class Logger {

    private static final LoggerFactory loggerFactory;

    static {
        try {
            loggerFactory =
                    (LoggerFactory) Class.forName("org.glowroot.agent.impl.LoggerFactoryImpl")
                            .getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return loggerFactory.getLogger(clazz);
    }

    public abstract String getName();

    public abstract boolean isTraceEnabled();

    public abstract void trace(String msg);

    public abstract void trace(String format, Object arg);

    public abstract void trace(String format, Object arg1,
            Object arg2);

    public abstract void trace(String format, Object... arguments);

    public abstract void trace(String msg, Throwable t);

    public abstract boolean isDebugEnabled();

    public abstract void debug(String msg);

    public abstract void debug(String format, Object arg);

    public abstract void debug(String format, Object arg1,
            Object arg2);

    public abstract void debug(String format, Object... arguments);

    public abstract void debug(String msg, Throwable t);

    public abstract boolean isInfoEnabled();

    public abstract void info(String msg);

    public abstract void info(String format, Object arg);

    public abstract void info(String format, Object arg1,
            Object arg2);

    public abstract void info(String format, Object... arguments);

    public abstract void info(String msg, Throwable t);

    public abstract boolean isWarnEnabled();

    public abstract void warn(String msg);

    public abstract void warn(String format, Object arg);

    public abstract void warn(String format, Object... arguments);

    public abstract void warn(String format, Object arg1,
            Object arg2);

    public abstract void warn(String msg, Throwable t);

    public abstract boolean isErrorEnabled();

    public abstract void error(String msg);

    public abstract void error(String format, Object arg);

    public abstract void error(String format, Object arg1,
            Object arg2);

    public abstract void error(String format, Object... arguments);

    public abstract void error(String msg, Throwable t);
}
