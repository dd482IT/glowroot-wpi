/*
 * Copyright 2016-2019 the original author or authors.
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
package org.glowroot.agent.plugin.executor;

import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.AuxThreadContext;
import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.ParameterHolder;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ExecutorAspect {

    private static final Logger logger = Logger.getLogger(ExecutorAspect.class);

    private static final String EXECUTOR_CLASSES = "java.util.concurrent.Executor"
            + "|java.util.concurrent.ExecutorService"
            + "|org.springframework.core.task.AsyncTaskExecutor"
            + "|org.springframework.core.task.AsyncListenableTaskExecutor";

    private static final AtomicBoolean isDoneExceptionLogged = new AtomicBoolean();

    // the field and method names are verbose since they will be mixed in to existing classes
    public abstract static class RunnableEtcImpl implements RunnableEtcMixin {

        private transient volatile AuxThreadContext glowroot$auxContext;

        @Override
        public AuxThreadContext glowroot$getAuxContext() {
            return glowroot$auxContext;
        }

        @Override
        public void glowroot$setAuxContext(AuxThreadContext auxContext) {
            glowroot$auxContext = auxContext;
        }
    }

    // TODO suppress various known thread pool threads (e.g. TimerThread)
    public static class SuppressedRunnableImpl implements SuppressedRunnableMixin {}

    // the method names are verbose since they will be mixed in to existing classes
    public interface RunnableEtcMixin {

        AuxThreadContext glowroot$getAuxContext();

        void glowroot$setAuxContext(AuxThreadContext auxContext);
    }

    public interface SuppressedRunnableMixin {}

    public static class ExecuteRunnableAdvice {
        public static void onBefore(ThreadContext context,
                ParameterHolder<Runnable> runnableHolder) {
            onBeforeWithRunnableHolder(context, runnableHolder);
        }
    }

    public static class ExecuteCallableAdvice {
        public static <T> void onBefore(ThreadContext context,
                ParameterHolder<Callable<T>> callableHolder) {
            onBeforeWithCallableHolder(context, callableHolder);
        }
    }

    public static class ForkJoinPoolAdvice {
        public static boolean isEnabled(Object forkJoinTask) {
            // this class may have been loaded before class file transformer was added to jvm
            return forkJoinTask instanceof RunnableEtcMixin;
        }
        public static void onBefore(ThreadContext context, Object forkJoinTask) {
            // cast is safe because of isEnabled() check above
            onBeforeCommon(context, (RunnableEtcMixin) forkJoinTask);
        }
    }

    public static class AkkaJsr166yForkJoinPoolAdvice {
        public static boolean isEnabled(Object forkJoinTask) {
            // this class may have been loaded before class file transformer was added to jvm
            return forkJoinTask instanceof RunnableEtcMixin;
        }
        public static void onBefore(ThreadContext context, Object forkJoinTask) {
            // cast is safe because of isEnabled() check above
            onBeforeCommon(context, (RunnableEtcMixin) forkJoinTask);
        }
    }

    public static class ScalaForkJoinPoolAdvice {
        public static boolean isEnabled(Object forkJoinTask) {
            // this class may have been loaded before class file transformer was added to jvm
            return forkJoinTask instanceof RunnableEtcMixin;
        }
        public static void onBefore(ThreadContext context, Object forkJoinTask) {
            // cast is safe because of isEnabled() check above
            onBeforeCommon(context, (RunnableEtcMixin) forkJoinTask);
        }
    }

    public static class ThreadInitAdvice {
        public static void onReturn(ThreadContext context, Thread thread) {
            onThreadInitCommon(context, thread);
        }
    }

    public static class ThreadInitWithStringAdvice {
        public static void onReturn(ThreadContext context, Thread thread) {
            onThreadInitCommon(context, thread);
        }
    }

    public static class ThreadInitWithStringAndThreadGroupAdvice {
        public static void onReturn(ThreadContext context, Thread thread) {
            onThreadInitCommon(context, thread);
        }
    }

    public static class ThreadInitWithRunnableAdvice {
        // cannot use @BindReceiver in @OnBefore of a constructor (at least not in OpenJ9, and for
        // good reason since receiver is not initialized before call to super)
        public static boolean onBefore(ThreadContext context,
                ParameterHolder<Runnable> runnableHolder) {
            return onThreadInitCommon(context, runnableHolder);
        }
        public static void onReturn(ThreadContext context, boolean alreadyHandled,
                Thread thread) {
            if (!alreadyHandled && thread instanceof RunnableEtcMixin) {
                onBeforeCommon(context, (RunnableEtcMixin) thread);
            }
        }
    }

    public static class ThreadInitWithThreadGroupAdvice {
        // cannot use @BindReceiver in @OnBefore of a constructor (at least not in OpenJ9, and for
        // good reason since receiver is not initialized before call to super)
        public static boolean onBefore(ThreadContext context,
                @SuppressWarnings("unused") ThreadGroup threadGroup,
                ParameterHolder<Runnable> runnableHolder) {
            return onThreadInitCommon(context, runnableHolder);
        }
        public static void onReturn(ThreadContext context, boolean alreadyHandled,
                Thread thread) {
            if (!alreadyHandled && thread instanceof RunnableEtcMixin) {
                onBeforeCommon(context, (RunnableEtcMixin) thread);
            }
        }
    }

    public static class AddListenerAdvice {
        public static void onBefore(ThreadContext context,
                ParameterHolder<Runnable> runnableHolder) {
            onBeforeWithRunnableHolder(context, runnableHolder);
        }
    }

    public static class InvokeAnyAllAdvice {
        public static void onBefore(ThreadContext context, Collection<?> callables) {
            if (callables == null) {
                return;
            }
            for (Object callable : callables) {
                // this class may have been loaded before class file transformer was added to jvm
                if (callable instanceof RunnableEtcMixin) {
                    RunnableEtcMixin callableMixin = (RunnableEtcMixin) callable;
                    AuxThreadContext auxContext = context.createAuxThreadContext();
                    callableMixin.glowroot$setAuxContext(auxContext);
                }
            }
        }
    }

    public static class ScheduleRunnableAdvice {
        public static void onBefore(ThreadContext context,
                ParameterHolder<Runnable> runnableHolder) {
            onBeforeWithRunnableHolder(context, runnableHolder);
        }
    }

    public static class ScheduleCallableAdvice {
        public static <T> void onBefore(ThreadContext context,
                ParameterHolder<Callable<T>> callableHolder) {
            onBeforeWithCallableHolder(context, callableHolder);
        }
    }

    public static class ScheduleOnceAdvice {
        public static void onBefore(ThreadContext context,
                @SuppressWarnings("unused") Object duration,
                ParameterHolder<Runnable> runnableHolder) {
            onBeforeWithRunnableHolder(context, runnableHolder);
        }
    }

    public static class TimerScheduleAdvice {
        public static boolean isEnabled(Object runnableEtc) {
            // this class may have been loaded before class file transformer was added to jvm
            return runnableEtc instanceof RunnableEtcMixin;
        }
        public static void onBefore(ThreadContext context, TimerTask timerTask) {
            // cast is safe because of isEnabled() check above
            onBeforeCommon(context, (RunnableEtcMixin) timerTask);
        }
    }

    // this method uses submit() and returns Future, but none of the callers use/wait on the Future
    public static class EhcacheDiskStorageScheduleAdvice {}

    // these methods use execute() to start long running threads that should not be tied to the
    // current transaction
    public static class JettyDoStartAdvice {}

    public static class StartAdvice {
        public static void onBefore(ThreadContext context,
                ParameterHolder<Runnable> runnableHolder) {
            onBeforeWithRunnableHolder(context, runnableHolder);
        }
    }

    public static class FutureGetAdvice {
        private static final TimerName timerName = Agent.getTimerName(FutureGetAdvice.class);
        public static boolean isEnabled(Future<?> future,
                FutureClassMeta futureClassMeta) {
            if (futureClassMeta.isNonStandardFuture()) {
                // this is to handle known non-standard Future implementations
                return false;
            }
            // don't capture if already done, primarily this is to avoid caching pattern where
            // a future is used to store the value to ensure only-once initialization
            try {
                return !future.isDone();
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
                if (!isDoneExceptionLogged.getAndSet(true)) {
                    logger.info("encountered a non-standard java.util.concurrent.Future"
                            + " implementation, please report this stack trace to the Glowroot"
                            + " project:", e);
                }
                return false;
            }
        }
        public static Timer onBefore(ThreadContext context) {
            return context.startTimer(timerName);
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    // the nesting group only starts applying once auxiliary thread context is started (it does not
    // apply to OptionalThreadContext that miss)
    public static class RunnableAdvice {
        public static boolean isEnabled(Runnable runnable) {
            if (!(runnable instanceof RunnableEtcMixin)) {
                // this class was loaded before class file transformer was added to jvm
                return false;
            }
            RunnableEtcMixin runnableMixin = (RunnableEtcMixin) runnable;
            return runnableMixin.glowroot$getAuxContext() != null;
        }
        public static TraceEntry onBefore(Runnable runnable) {
            RunnableEtcMixin runnableMixin = (RunnableEtcMixin) runnable;
            AuxThreadContext auxContext = runnableMixin.glowroot$getAuxContext();
            if (auxContext == null) {
                // this is unlikely (since checked in @IsEnabled) but possible under concurrency
                return null;
            }
            runnableMixin.glowroot$setAuxContext(null);
            return auxContext.start();
        }
        public static void onReturn(TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }

    // the nesting group only starts applying once auxiliary thread context is started (it does not
    // apply to OptionalThreadContext that miss)
    public static class CallableAdvice {
        public static boolean isEnabled(Callable<?> callable) {
            if (!(callable instanceof RunnableEtcMixin)) {
                // this class was loaded before class file transformer was added to jvm
                return false;
            }
            RunnableEtcMixin callableMixin = (RunnableEtcMixin) callable;
            return callableMixin.glowroot$getAuxContext() != null;
        }
        public static TraceEntry onBefore(Callable<?> callable) {
            RunnableEtcMixin callableMixin = (RunnableEtcMixin) callable;
            AuxThreadContext auxContext = callableMixin.glowroot$getAuxContext();
            if (auxContext == null) {
                // this is unlikely (since checked in @IsEnabled) but possible under concurrency
                return null;
            }
            callableMixin.glowroot$setAuxContext(null);
            return auxContext.start();
        }
        public static void onReturn(TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }

    // need to clear out the "executor-execute" nesting group, see
    // ExecutorWithLambdasIT.shouldCaptureNestedExecute()
    public static class RunnableWrapperAdvice {}

    // need to clear out the "executor-execute" nesting group, see
    // ExecutorWithLambdasIT.shouldCaptureNestedSubmit()
    public static class CallableWrapperAdvice {}

    // the nesting group only starts applying once auxiliary thread context is started (it does not
    // apply to OptionalThreadContext that miss)
    public static class ExecAdvice {
        public static boolean isEnabled(Object task) {
            if (!(task instanceof RunnableEtcMixin)) {
                // this class was loaded before class file transformer was added to jvm
                return false;
            }
            RunnableEtcMixin taskMixin = (RunnableEtcMixin) task;
            return taskMixin.glowroot$getAuxContext() != null;
        }
        public static TraceEntry onBefore(Object task) {
            RunnableEtcMixin taskMixin = (RunnableEtcMixin) task;
            AuxThreadContext auxContext = taskMixin.glowroot$getAuxContext();
            if (auxContext == null) {
                // this is unlikely (since checked in @IsEnabled) but possible under concurrency
                return null;
            }
            taskMixin.glowroot$setAuxContext(null);
            return auxContext.start();
        }
        public static void onReturn(TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }
    private static void onBeforeWithRunnableHolder(ThreadContext context,
            ParameterHolder<Runnable> runnableHolder) {
        Runnable runnable = runnableHolder.get();
        if (runnable instanceof SuppressedRunnableMixin) {
            return;
        } else if (runnable instanceof RunnableEtcMixin) {
            onBeforeCommon(context, (RunnableEtcMixin) runnable);
        } else if (runnable != null && runnable.getClass().getName().contains("$$Lambda$")) {
            wrapRunnable(runnableHolder, context);
        }
    }

    private static <T> void onBeforeWithCallableHolder(ThreadContext context,
            ParameterHolder<Callable<T>> callableHolder) {
        Callable<T> callable = callableHolder.get();
        if (callable instanceof RunnableEtcMixin) {
            onBeforeCommon(context, (RunnableEtcMixin) callable);
        } else if (callable != null && callable.getClass().getName().contains("$$Lambda$")) {
            wrapCallable(callableHolder, context);
        }
    }

    private static void onThreadInitCommon(ThreadContext context, Thread thread) {
        if (thread instanceof RunnableEtcMixin && !(thread instanceof SuppressedRunnableMixin)) {
            onBeforeCommon(context, (RunnableEtcMixin) thread);
        }
    }

    private static boolean onThreadInitCommon(ThreadContext context,
            ParameterHolder<Runnable> runnableHolder) {
        Runnable runnable = runnableHolder.get();
        if (!(runnable instanceof SuppressedRunnableMixin)) {
            if (runnable instanceof RunnableEtcMixin) {
                onBeforeCommon(context, (RunnableEtcMixin) runnable);
                return true;
            } else if (runnable != null && runnable.getClass().getName().contains("$$Lambda$")) {
                wrapRunnable(runnableHolder, context);
                return true;
            }
        }
        return false;
    }

    private static void onBeforeCommon(ThreadContext context, RunnableEtcMixin runnableEtc) {
        RunnableEtcMixin runnableMixin = runnableEtc;
        AuxThreadContext auxContext = context.createAuxThreadContext();
        runnableMixin.glowroot$setAuxContext(auxContext);
    }

    private static void wrapRunnable(ParameterHolder<Runnable> runnableHolder,
            ThreadContext context) {
        Runnable runnable = runnableHolder.get();
        if (runnable != null) {
            runnableHolder.set(new RunnableWrapper(runnable, context.createAuxThreadContext()));
        }
    }

    private static <T> void wrapCallable(ParameterHolder<Callable<T>> callableHolder,
            ThreadContext context) {
        Callable<T> callable = callableHolder.get();
        if (callable != null) {
            callableHolder.set(new CallableWrapper<T>(callable, context.createAuxThreadContext()));
        }
    }

    // ========== debug ==========

    // KEEP THIS CODE IT IS VERY USEFUL

    // private static final ThreadLocal<?> inAuxDebugLogging;
    //
    // static {
    // try {
    // Class<?> clazz = Class.forName("org.glowroot.agent.impl.AuxThreadContextImpl");
    // Field field = clazz.getDeclaredField("inAuxDebugLogging");
    // field.setAccessible(true);
    // inAuxDebugLogging = (ThreadLocal<?>) field.get(null);
    // } catch (Exception e) {
    // throw new IllegalStateException(e);
    // }
    // }
    //
    // @Pointcut(className = "/(?!org.glowroot).*/", methodName = "<init>",
    // methodParameterTypes = {".."})
    // public static class RunnableInitAdvice {
    //
    // @OnAfter
    // public static void onAfter(OptionalThreadContext context, @BindReceiver Object obj) {
    // if (obj instanceof Runnable && isNotGlowrootThread()
    // && inAuxDebugLogging.get() == null) {
    // new Exception(
    // "Init " + Thread.currentThread().getName() + " " + obj.getClass().getName()
    // + ":" + obj.hashCode() + " " + context.getClass().getName())
    // .printStackTrace();
    // }
    // }
    // }
    //
    // @Pointcut(className = "java.lang.Runnable", methodName = "run", methodParameterTypes = {},
    // order = 1)
    // public static class RunnableRunAdvice {
    //
    // @IsEnabled
    // public static boolean isEnabled() {
    // return isNotGlowrootThread();
    // }
    //
    // @OnBefore
    // public static void onBefore(OptionalThreadContext context, @BindReceiver Runnable obj) {
    // new Exception("Run " + Thread.currentThread().getName() + " " + obj.getClass().getName()
    // + ":" + obj.hashCode() + " " + context.getClass().getName()).printStackTrace();
    // }
    // }
    //
    // private static boolean isNotGlowrootThread() {
    // String threadName = Thread.currentThread().getName();
    // return !threadName.contains("GRPC") && !threadName.contains("Glowroot");
    // }
}
