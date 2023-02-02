/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.agent.tests.plugin;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.weaving.BindOptionalReturn;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

// this exists to test bytecode verification problem when both @BindReturn and OptionalThreadContext
// are used in @OnReturn
public class SimpleAspect {

    public static class SimpleRunAdvice1 {

        @SuppressWarnings("unused")
        public static void onReturn(OptionalThreadContext context) {}
    }

    public static class SimpleRunAdvice2 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value, OptionalThreadContext context) {}
    }

    public static class SimpleRunAdvice3 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value,
                OptionalThreadContext context) {}
    }

    public static class SimpleRunAdvice4 {

        @SuppressWarnings("unused")
        public static Object onReturn(OptionalThreadContext context) {
            return null;
        }
    }

    public static class SimpleRunAdvice5 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value,
                OptionalThreadContext context) {
            return null;
        }
    }

    public static class SimpleRunAdvice6 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value,
                OptionalThreadContext context) {
            return null;
        }
    }

    public static class SimpleRunAdvice7 {

        public static void onReturn() {}
    }

    public static class SimpleRunAdvice8 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value) {}
    }

    public static class SimpleRunAdvice9 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value) {}
    }

    public static class SimpleRunAdvice10 {

        public static Object onReturn() {
            return null;
        }
    }

    public static class SimpleRunAdvice11 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value) {
            return null;
        }
    }

    public static class SimpleRunAdvice12 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value) {
            return null;
        }
    }

    public static class SimpleRunAdvice13 {

        @SuppressWarnings("unused")
        public static void onReturn(ThreadContext context) {}
    }

    public static class SimpleRunAdvice14 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value, ThreadContext context) {}
    }

    public static class SimpleRunAdvice15 {

        @SuppressWarnings("unused")
        public static void onReturn(Object value, ThreadContext context) {}
    }

    public static class SimpleRunAdvice16 {

        @SuppressWarnings("unused")
        public static Object onReturn(ThreadContext context) {
            return null;
        }
    }

    public static class SimpleRunAdvice17 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value, ThreadContext context) {
            return null;
        }
    }

    public static class SimpleRunAdvice18 {

        @SuppressWarnings("unused")
        public static Object onReturn(Object value,
                ThreadContext context) {
            return null;
        }
    }
}
