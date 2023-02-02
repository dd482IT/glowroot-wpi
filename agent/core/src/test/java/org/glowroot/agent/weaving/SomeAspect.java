/*
 * Copyright 2012-2018 the original author or authors.
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
package org.glowroot.agent.weaving;

import java.util.List;

import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.plugin.api.ClassInfo;
import org.glowroot.agent.plugin.api.MethodInfo;
import org.glowroot.agent.plugin.api.ParameterHolder;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindMethodMeta;
import org.glowroot.agent.plugin.api.weaving.BindMethodName;
import org.glowroot.agent.plugin.api.weaving.BindOptionalReturn;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindParameterArray;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.MethodModifier;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.MixinInit;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.OptionalReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.weaving.targets.Misc;

public class SomeAspect {

    public static class BasicAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class SuperBasicAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class ThrowableToStringAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class GenericMiscAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BasicMiscConstructorAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BasicMiscAllConstructorAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.orderedEvents.get().add("isEnabled");
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.orderedEvents.get().add("onBefore");
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.orderedEvents.get().add("onReturn");
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.orderedEvents.get().add("onThrow");
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.orderedEvents.get().add("onAfter");
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BasicWithInnerClassArgAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BasicWithInnerClassAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BindReceiverAdvice {
        public static boolean isEnabled(Misc receiver) {
            SomeAspectThreadLocals.isEnabledReceiver.set(receiver);
            return true;
        }
        public static void onBefore(Misc receiver) {
            SomeAspectThreadLocals.onBeforeReceiver.set(receiver);
        }
        public static void onReturn(Misc receiver) {
            SomeAspectThreadLocals.onReturnReceiver.set(receiver);
        }
        public static void onThrow(Misc receiver) {
            SomeAspectThreadLocals.onThrowReceiver.set(receiver);
        }
        public static void onAfter(Misc receiver) {
            SomeAspectThreadLocals.onAfterReceiver.set(receiver);
        }
    }

    public static class BindParameterAdvice {
        public static boolean isEnabled(String one, int two) {
            SomeAspectThreadLocals.isEnabledParams.set(new Object[] {one, two});
            return true;
        }
        public static void onBefore(String one, int two) {
            SomeAspectThreadLocals.onBeforeParams.set(new Object[] {one, two});
        }
        public static void onReturn(String one, int two) {
            SomeAspectThreadLocals.onReturnParams.set(new Object[] {one, two});
        }
        public static void onThrow(String one, int two) {
            SomeAspectThreadLocals.onThrowParams.set(new Object[] {one, two});
        }
        public static void onAfter(String one, int two) {
            SomeAspectThreadLocals.onAfterParams.set(new Object[] {one, two});
        }
    }

    public static class BindParameterArrayAdvice {
        public static boolean isEnabled(Object[] args) {
            SomeAspectThreadLocals.isEnabledParams.set(args);
            return true;
        }
        public static void onBefore(Object[] args) {
            SomeAspectThreadLocals.onBeforeParams.set(args);
        }
        public static void onReturn(Object[] args) {
            SomeAspectThreadLocals.onReturnParams.set(args);
        }
        public static void onThrow(Object[] args) {
            SomeAspectThreadLocals.onThrowParams.set(args);
        }
        public static void onAfter(Object[] args) {
            SomeAspectThreadLocals.onAfterParams.set(args);
        }
    }

    public static class BindTravelerAdvice {
        public static String onBefore() {
            return "a traveler";
        }
        public static void onReturn(String traveler) {
            SomeAspectThreadLocals.onReturnTraveler.set(traveler);
        }
        public static void onThrow(String traveler) {
            SomeAspectThreadLocals.onThrowTraveler.set(traveler);
        }
        public static void onAfter(String traveler) {
            SomeAspectThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    public static class BindPrimitiveTravelerAdvice {
        public static int onBefore() {
            return 3;
        }
        public static void onReturn(int traveler) {
            SomeAspectThreadLocals.onReturnTraveler.set(traveler);
        }
        public static void onThrow(int traveler) {
            SomeAspectThreadLocals.onThrowTraveler.set(traveler);
        }
        public static void onAfter(int traveler) {
            SomeAspectThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    public static class BindPrimitiveBooleanTravelerAdvice {
        public static boolean onBefore() {
            return true;
        }
        public static void onReturn(boolean traveler) {
            SomeAspectThreadLocals.onReturnTraveler.set(traveler);
        }
        public static void onThrow(boolean traveler) {
            SomeAspectThreadLocals.onThrowTraveler.set(traveler);
        }
        public static void onAfter(boolean traveler) {
            SomeAspectThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    public static class BindPrimitiveTravelerBadAdvice {
        public static void onBefore() {}
        public static void onReturn(int traveler) {
            SomeAspectThreadLocals.onReturnTraveler.set(traveler);
        }
        public static void onThrow(int traveler) {
            SomeAspectThreadLocals.onThrowTraveler.set(traveler);
        }
        public static void onAfter(int traveler) {
            SomeAspectThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    public static class BindPrimitiveBooleanTravelerBadAdvice {
        public static void onBefore() {}
        public static void onReturn(boolean traveler) {
            SomeAspectThreadLocals.onReturnTraveler.set(traveler);
        }
        public static void onThrow(boolean traveler) {
            SomeAspectThreadLocals.onThrowTraveler.set(traveler);
        }
        public static void onAfter(boolean traveler) {
            SomeAspectThreadLocals.onAfterTraveler.set(traveler);
        }
    }

    public static class BindClassMetaAdvice {
        public static boolean isEnabled(TestClassMeta meta) {
            SomeAspectThreadLocals.isEnabledClassMeta.set(meta);
            return true;
        }
        public static void onBefore(TestClassMeta meta) {
            SomeAspectThreadLocals.onBeforeClassMeta.set(meta);
        }
        public static void onReturn(TestClassMeta meta) {
            SomeAspectThreadLocals.onReturnClassMeta.set(meta);
        }
        public static void onThrow(TestClassMeta meta) {
            SomeAspectThreadLocals.onThrowClassMeta.set(meta);
        }
        public static void onAfter(TestClassMeta meta) {
            SomeAspectThreadLocals.onAfterClassMeta.set(meta);
        }
    }

    public static class BindMethodMetaAdvice {
        public static boolean isEnabled(TestMethodMeta meta) {
            SomeAspectThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        public static void onBefore(TestMethodMeta meta) {
            SomeAspectThreadLocals.onBeforeMethodMeta.set(meta);
        }
        public static void onReturn(TestMethodMeta meta) {
            SomeAspectThreadLocals.onReturnMethodMeta.set(meta);
        }
        public static void onThrow(TestMethodMeta meta) {
            SomeAspectThreadLocals.onThrowMethodMeta.set(meta);
        }
        public static void onAfter(TestMethodMeta meta) {
            SomeAspectThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    public static class BindMethodMetaArrayAdvice {
        public static boolean isEnabled(TestMethodMeta meta) {
            SomeAspectThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        public static void onBefore(TestMethodMeta meta) {
            SomeAspectThreadLocals.onBeforeMethodMeta.set(meta);
        }
        public static void onReturn(TestMethodMeta meta) {
            SomeAspectThreadLocals.onReturnMethodMeta.set(meta);
        }
        public static void onThrow(TestMethodMeta meta) {
            SomeAspectThreadLocals.onThrowMethodMeta.set(meta);
        }
        public static void onAfter(TestMethodMeta meta) {
            SomeAspectThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    public static class BindMethodMetaReturnArrayAdvice {
        public static boolean isEnabled(TestMethodMeta meta) {
            SomeAspectThreadLocals.isEnabledMethodMeta.set(meta);
            return true;
        }
        public static void onBefore(TestMethodMeta meta) {
            SomeAspectThreadLocals.onBeforeMethodMeta.set(meta);
        }
        public static void onReturn(TestMethodMeta meta) {
            SomeAspectThreadLocals.onReturnMethodMeta.set(meta);
        }
        public static void onThrow(TestMethodMeta meta) {
            SomeAspectThreadLocals.onThrowMethodMeta.set(meta);
        }
        public static void onAfter(TestMethodMeta meta) {
            SomeAspectThreadLocals.onAfterMethodMeta.set(meta);
        }
    }

    public static class BindReturnAdvice {
        public static void onReturn(String value) {
            SomeAspectThreadLocals.returnValue.set(value);
        }
    }

    public static class BindPrimitiveReturnAdvice {
        public static void onReturn(int value) {
            SomeAspectThreadLocals.returnValue.set(value);
        }
    }

    public static class BindAutoboxedReturnAdvice {
        public static void onReturn(Object value) {
            SomeAspectThreadLocals.returnValue.set(value);
        }
    }

    public static class BindOptionalReturnAdvice {
        public static void onReturn(OptionalReturn optionalReturn) {
            SomeAspectThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    public static class BindOptionalVoidReturnAdvice {
        public static void onReturn(OptionalReturn optionalReturn) {
            SomeAspectThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    public static class BindOptionalPrimitiveReturnAdvice {
        public static void onReturn(OptionalReturn optionalReturn) {
            SomeAspectThreadLocals.optionalReturnValue.set(optionalReturn);
        }
    }

    public static class BindThrowableAdvice {
        public static void onThrow(Throwable t) {
            SomeAspectThreadLocals.onThrowCount.increment();
            SomeAspectThreadLocals.throwable.set(t);
        }
    }

    public static class ThrowInOnBeforeAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            throw new RuntimeException("Abxy");
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BasicHighOrderAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BindMethodNameAdvice {
        public static boolean isEnabled(String methodName) {
            SomeAspectThreadLocals.isEnabledMethodName.set(methodName);
            return true;
        }
        public static void onBefore(String methodName) {
            SomeAspectThreadLocals.onBeforeMethodName.set(methodName);
        }
        public static void onReturn(String methodName) {
            SomeAspectThreadLocals.onReturnMethodName.set(methodName);
        }
        public static void onThrow(String methodName) {
            SomeAspectThreadLocals.onThrowMethodName.set(methodName);
        }
        public static void onAfter(String methodName) {
            SomeAspectThreadLocals.onAfterMethodName.set(methodName);
        }
    }

    public static class ChangeReturnAdvice {
        public static boolean isEnabled() {
            return true;
        }
        public static String onReturn(String value, String methodName) {
            return "modified " + value + ":" + methodName;
        }
    }

    public static class MethodParametersDotDotAdvice1 {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class MethodParametersBadDotDotAdvice1 {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class MethodParametersDotDotAdvice2 {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class MethodParametersDotDotAdvice3 {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class SubTypeRestrictionAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class SubTypeRestrictionWhereMethodNotReImplementedInSubClassAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class SubTypeRestrictionWhereMethodNotReImplementedInSubSubClassAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class BasicAnnotationBasedAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class AnotherAnnotationBasedAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class AnotherAnnotationBasedAdviceButWrong {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class SuperTypeRestrictionAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class SuperTypeRestrictionWhereMethodNotReImplementedInSubClassAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public static class ComplexSuperTypeRestrictionAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return SomeAspectThreadLocals.enabled.get();
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
        public static void enable() {
            SomeAspectThreadLocals.enabled.set(true);
        }
        public static void disable() {
            SomeAspectThreadLocals.enabled.set(false);
        }
    }

    public interface Shimmy {
        Object shimmyGetString();
        void shimmySetString(String string);
    }

    public interface HasString {
        String getString();
        void setString(String string);
    }

    public static class HasStringClassMixin implements HasString {
        private transient String string;
        private void initHasString() {
            if (string == null) {
                string = "a string";
            } else {
                string = "init called twice";
            }
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    public static class HasStringInterfaceMixin implements HasString {
        private transient String string;
        private void initHasString() {
            string = "a string";
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    public static class HasStringMultipleMixin implements HasString {
        private transient String string;
        private void initHasString() {
            string = "a string";
        }
        @Override
        public String getString() {
            return string;
        }
        @Override
        public void setString(String string) {
            this.string = string;
        }
    }

    public static class InnerMethodAdvice extends BasicAdvice {}

    public static class MultipleMethodsAdvice extends BasicAdvice {}

    public static class StaticAdvice extends BasicAdvice {}

    public static class NonMatchingStaticAdvice extends BasicAdvice {}

    public static class MatchingPublicNonStaticAdvice extends BasicAdvice {}

    public static class ClassNamePatternAdvice extends BasicAdvice {}

    public static class MethodReturnVoidAdvice extends BasicAdvice {}

    public static class MethodReturnCharSequenceAdvice extends BasicAdvice {}

    public static class MethodReturnStringAdvice extends BasicAdvice {}

    public static class NonMatchingMethodReturnAdvice extends BasicAdvice {}

    public static class NonMatchingMethodReturnAdvice2 extends BasicAdvice {}

    public static class MethodReturnNarrowingAdvice extends BasicAdvice {}

    public static class WildMethodAdvice extends BasicAdvice {}

    public static class PrimitiveAdvice extends BasicAdvice {}

    public static class PrimitiveWithWildcardAdvice {
        public static boolean isEnabled(@SuppressWarnings("unused") int x) {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore(@SuppressWarnings("unused") int x) {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class PrimitiveWithAutoboxAdvice {
        public static boolean isEnabled(@SuppressWarnings("unused") Object x) {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
    }

    public static class BrokenAdvice {
        public static boolean isEnabled() {
            return true;
        }
        public static Object onBefore() {
            return null;
        }
        public static void onAfter(@SuppressWarnings("unused") Object traveler) {}
    }

    public static class VeryBadAdvice {
        public static Object onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
            throw new IllegalStateException("Sorry");
        }
        public static void onThrow() {
            // should not get called
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            // should not get called
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class MoreVeryBadAdvice {
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
            throw new IllegalStateException("Sorry");
        }
        public static void onThrow() {
            // should not get called
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            // should not get called
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    // same as MoreVeryBadAdvice, but testing weaving a method with a non-void return type
    public static class MoreVeryBadAdvice2 {
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
            throw new IllegalStateException("Sorry");
        }
        public static void onThrow() {
            // should not get called
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            // should not get called
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class CircularClassDependencyAdvice {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class InterfaceAppearsTwiceInHierarchyAdvice {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    public static class FinalMethodAdvice {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
    }

    // test weaving against JSR bytecode that ends up being inlined via JSRInlinerAdapter
    public static class TestJSRMethodAdvice {}

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice {}

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice2 {
        public static boolean isEnabled() {
            return true;
        }
    }

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice3 {
        public static void onBefore() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice4 {
        public static void onReturn() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice5 {
        public static void onThrow() {}
    }

    // test weaving against 1.7 bytecode with stack frames
    public static class TestBytecodeWithStackFramesAdvice6 {
        public static void onAfter() {}
    }

    public static class TestTroublesomeBytecodeAdvice {
        public static void onAfter() {}
    }

    public static class NotPerfectBytecodeAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class MoreNotPerfectBytecodeAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class HackedConstructorBytecodeAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class HackedConstructorBytecodeJumpingAdvice {
        public static boolean isEnabled() {
            SomeAspectThreadLocals.enabledCount.increment();
            return true;
        }
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class IterableAdvice {
        public static void onBefore() {
            SomeAspectThreadLocals.onBeforeCount.increment();
        }
        public static void onReturn() {
            SomeAspectThreadLocals.onReturnCount.increment();
        }
        public static void onThrow() {
            SomeAspectThreadLocals.onThrowCount.increment();
        }
        public static void onAfter() {
            SomeAspectThreadLocals.onAfterCount.increment();
        }
    }

    public static class BindMutableParameterAdvice {
        public static void onBefore(ParameterHolder<String> holder,
                ParameterHolder<Integer> holder2) {
            holder.set(holder.get() + " and more");
            holder2.set(holder2.get() + 1);
        }
    }

    public static class BindMutableParameterWithMoreFramesAdvice {
        public static void onBefore(ParameterHolder<String> holder,
                ParameterHolder<Integer> holder2) {
            holder.set(holder.get() + " and more");
            holder2.set(holder2.get() + 1);
        }
    }

    public static class TestClassMeta {

        private final ClassInfo classInfo;

        public TestClassMeta(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        public String getClazzName() {
            return classInfo.getName();
        }
    }

    public static class TestMethodMeta {

        private final MethodInfo methodInfo;

        public TestMethodMeta(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
        }

        public String getDeclaringClassName() {
            return methodInfo.getDeclaringClassName();
        }

        public String getReturnTypeName() {
            return methodInfo.getReturnType().getName();
        }

        public List<String> getParameterTypeNames() {
            List<String> parameterTypeNames = Lists.newArrayList();
            for (Class<?> parameterType : methodInfo.getParameterTypes()) {
                parameterTypeNames.add(parameterType.getName());
            }
            return parameterTypeNames;
        }
    }

    public @interface SomeClass {}

    public @interface SomeMethod {}
}
