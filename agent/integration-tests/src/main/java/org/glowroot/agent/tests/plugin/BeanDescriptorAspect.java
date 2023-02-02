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
package org.glowroot.agent.tests.plugin;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.ClassInfo;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.MethodInfo;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindMethodMeta;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

// this is for testing weaving of bootstrap classes
public class BeanDescriptorAspect {

    public static class GetBeanClassAdvice {

        private static final TimerName timerName = Agent.getTimerName(GetBeanClassAdvice.class);

        public static TraceEntry onBefore(OptionalThreadContext context,
                TestClassMeta meta) {
            return context.startTraceEntry(MessageSupplier.create(meta.classInfo.getName()),
                    timerName);
        }
        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    public static class GetCustomizerClassAdvice {

        private static final TimerName timerName =
                Agent.getTimerName(GetCustomizerClassAdvice.class);

        public static TraceEntry onBefore(OptionalThreadContext context,
                TestMethodMeta meta) {
            return context.startTraceEntry(MessageSupplier.create(meta.methodInfo.getName()),
                    timerName);
        }
        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    public static class TestClassMeta {

        private final ClassInfo classInfo;

        public TestClassMeta(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }
    }

    public static class TestMethodMeta {

        private final MethodInfo methodInfo;

        public TestMethodMeta(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
        }
    }
}
