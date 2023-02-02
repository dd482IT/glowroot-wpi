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
package org.glowroot.agent.plugin.play;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext.Priority;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;

public class Play2xAspect {

    private static final BooleanProperty useAltTransactionNaming =
            Agent.getConfigService("play").getBooleanProperty("useAltTransactionNaming");

    // "play.core.routing.TaggingInvoker" is for play 2.4.x and later
    // "play.core.Router$Routes$TaggingInvoker" is for play 2.3.x
    public interface TaggingInvoker {

        ScalaMap glowroot$cachedHandlerTags();
    }

    public interface ScalaMap {

        ScalaOption glowroot$get(Object key);
    }

    public interface ScalaOption {

        boolean isDefined();

        Object get();
    }

    public static class HandlerInvokerAdvice {
        public static void onBefore(ThreadContext context,
                TaggingInvoker taggingInvoker) {
            ScalaMap tags = taggingInvoker.glowroot$cachedHandlerTags();
            if (tags == null) {
                return;
            }
            if (useAltTransactionNaming.value()) {
                ScalaOption controllerOption = tags.glowroot$get("ROUTE_CONTROLLER");
                ScalaOption methodOption = tags.glowroot$get("ROUTE_ACTION_METHOD");
                if (controllerOption != null && controllerOption.isDefined() && methodOption != null
                        && methodOption.isDefined()) {
                    String controller = toString(controllerOption.get());
                    String transactionName =
                            getAltTransactionName(controller, toString(methodOption.get()));
                    context.setTransactionName(transactionName, Priority.CORE_PLUGIN);
                }
            } else {
                ScalaOption option = tags.glowroot$get("ROUTE_PATTERN");
                if (option != null && option.isDefined()) {
                    String route = toString(option.get());
                    route = Routes.simplifiedRoute(route);
                    context.setTransactionName(route, Priority.CORE_PLUGIN);
                }
            }
        }
        private static String toString(Object obj) {
            String str = obj.toString();
            return str == null ? "" : str;
        }
    }

    public static class RenderAdvice {

        private static final TimerName timerName = Agent.getTimerName(RenderAdvice.class);

        public static TraceEntry onBefore(ThreadContext context, Object view) {
            String viewName = view.getClass().getSimpleName();
            // strip off trailing $
            viewName = viewName.substring(0, viewName.length() - 1);
            return context.startTraceEntry(MessageSupplier.create("play render: {}", viewName),
                    timerName);
        }

        public static void onReturn(TraceEntry traceEntry) {
            traceEntry.end();
        }

        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    private static String getAltTransactionName(String controller, String methodName) {
        int index = controller.lastIndexOf('.');
        if (index == -1) {
            return controller + "#" + methodName;
        } else {
            return controller.substring(index + 1) + "#" + methodName;
        }
    }

    // ========== play 2.0.x - 2.2.x ==========

    public interface HandlerDef {

        String controller();

        String method();
    }

    public static class OldHandlerInvokerAdvice {
        public static void onBefore(ThreadContext context,
                @SuppressWarnings("unused") Object action,
                HandlerDef handlerDef, PlayInvoker invoker) {
            String controller = handlerDef.controller();
            String method = handlerDef.method();
            // path() method doesn't exist in play 2.0.x so need to use reflection instead of shim
            String path = invoker.path(handlerDef);
            if (useAltTransactionNaming.value() || path == null) {
                if (controller != null && method != null) {
                    context.setTransactionName(getAltTransactionName(controller, method),
                            Priority.CORE_PLUGIN);
                }
            } else {
                path = Routes.simplifiedRoute(path);
                context.setTransactionName(path, Priority.CORE_PLUGIN);
            }
        }
    }
}
