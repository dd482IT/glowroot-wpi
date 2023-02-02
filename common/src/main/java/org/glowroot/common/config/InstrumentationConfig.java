/*
 * Copyright 2013-2018 the original author or authors.
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
package org.glowroot.common.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig.AlreadyInTransactionBehavior;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig.CaptureKind;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig.MethodModifier;
import org.glowroot.wire.api.model.Proto.OptionalInt32;

public abstract class InstrumentationConfig {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationConfig.class);

    public String className() {
        return "";
    }

    public String classAnnotation() {
        return "";
    }

    public String subTypeRestriction() {
        return "";
    }

    public String superTypeRestriction() {
        return "";
    }

    // pointcuts with methodDeclaringClassName are no longer supported in 0.9.16, but included here
    // to help with transitioning of old instrumentation config
    @Deprecated
    public String methodDeclaringClassName() {
        return "";
    }

    public String methodName() {
        return "";
    }

    public String methodAnnotation() {
        return "";
    }

    // empty methodParameterTypes means match no-arg methods only
    public abstract ImmutableList<String> methodParameterTypes();

    public String methodReturnType() {
        return "";
    }

    // currently unused, but will have a purpose someday, e.g. to capture all public methods
    public abstract ImmutableList<MethodModifier> methodModifiers();

    public String nestingGroup() {
        return "";
    }

    public int order() {
        return 0;
    }

    public abstract CaptureKind captureKind();

    public String transactionType() {
        return "";
    }

    public String transactionNameTemplate() {
        return "";
    }

    public String transactionUserTemplate() {
        return "";
    }

    public abstract Map<String, String> transactionAttributeTemplates();

    // need to write zero since it is treated different from null
    public abstract Integer transactionSlowThresholdMillis();

    public abstract AlreadyInTransactionBehavior alreadyInTransactionBehavior();

    // corrected for data prior to 0.10.10
    public AlreadyInTransactionBehavior alreadyInTransactionBehaviorCorrected() {
        if (captureKind() == CaptureKind.TRANSACTION) {
            return MoreObjects.firstNonNull(alreadyInTransactionBehavior(),
                    AlreadyInTransactionBehavior.CAPTURE_TRACE_ENTRY);
        } else {
            return null;
        }
    }

    public boolean transactionOuter() {
        return false;
    }

    public String traceEntryMessageTemplate() {
        return "";
    }

    // need to write zero since it is treated different from null
    public abstract Integer traceEntryStackThresholdMillis();

    public boolean traceEntryCaptureSelfNested() {
        return false;
    }

    public String timerName() {
        return "";
    }

    // this is only for plugin authors (to be used in glowroot.plugin.json)
    public String enabledProperty() {
        return "";
    }

    // this is only for plugin authors (to be used in glowroot.plugin.json)
    public String traceEntryEnabledProperty() {
        return "";
    }

    public boolean isTimerOrGreater() {
        return captureKind() == CaptureKind.TIMER || captureKind() == CaptureKind.TRACE_ENTRY
                || captureKind() == CaptureKind.TRANSACTION;
    }

    public boolean isTraceEntryOrGreater() {
        return captureKind() == CaptureKind.TRACE_ENTRY || captureKind() == CaptureKind.TRANSACTION;
    }

    public boolean isTransaction() {
        return captureKind() == CaptureKind.TRANSACTION;
    }

    public ImmutableList<String> validationErrors() {
        List<String> errors = Lists.newArrayList();
        if (className().isEmpty() && classAnnotation().isEmpty()) {
            errors.add("className and classAnnotation are both empty");
        }
        if (methodName().isEmpty() && methodAnnotation().isEmpty()) {
            errors.add("methodName and methodAnnotation are both empty");
        }
        if (isTimerOrGreater() && timerName().isEmpty()) {
            errors.add("timerName is empty");
        }
        if (captureKind() == CaptureKind.TRACE_ENTRY && traceEntryMessageTemplate().isEmpty()) {
            errors.add("traceEntryMessageTemplate is empty");
        }
        if (isTransaction() && transactionType().isEmpty()) {
            errors.add("transactionType is empty");
        }
        if (isTransaction() && transactionNameTemplate().isEmpty()) {
            errors.add("transactionNameTemplate is empty");
        }
        if (!timerName().matches("[a-zA-Z0-9 ]*")) {
            errors.add("timerName contains invalid characters: " + timerName());
        }
        if (!methodDeclaringClassName().isEmpty()) {
            errors.add("methodDeclaringClassName is no longer supported");
        }
        return ImmutableList.copyOf(errors);
    }

    public void logValidationErrorsIfAny() {
        List<String> errors = validationErrors();
        if (!errors.isEmpty()) {
            logger.error("invalid instrumentation config: {} - {}", Joiner.on(", ").join(errors),
                    this);
        }
    }

    public AgentConfig.InstrumentationConfig toProto() {
        AgentConfig.InstrumentationConfig.Builder builder =
                AgentConfig.InstrumentationConfig.newBuilder()
                        .setClassName(className())
                        .setClassAnnotation(classAnnotation())
                        .setSubTypeRestriction(subTypeRestriction())
                        .setSuperTypeRestriction(superTypeRestriction())
                        // pointcuts with methodDeclaringClassName are no longer supported in
                        // 0.9.16, but included here to help with transitioning of old
                        // instrumentation config
                        .setMethodDeclaringClassName(methodDeclaringClassName())
                        .setMethodName(methodName())
                        .setMethodAnnotation(methodAnnotation())
                        .addAllMethodParameterType(methodParameterTypes())
                        .setMethodReturnType(methodReturnType())
                        .addAllMethodModifier(methodModifiers())
                        .setNestingGroup(nestingGroup())
                        .setOrder(order())
                        .setCaptureKind(captureKind())
                        .setTransactionType(transactionType())
                        .setTransactionNameTemplate(transactionNameTemplate())
                        .setTransactionUserTemplate(transactionUserTemplate())
                        .putAllTransactionAttributeTemplates(transactionAttributeTemplates());
        Integer transactionSlowThresholdMillis = transactionSlowThresholdMillis();
        if (transactionSlowThresholdMillis != null) {
            builder.setTransactionSlowThresholdMillis(
                    OptionalInt32.newBuilder().setValue(transactionSlowThresholdMillis));
        }
        AlreadyInTransactionBehavior alreadyInTransactionBehavior =
                alreadyInTransactionBehaviorCorrected();
        if (alreadyInTransactionBehavior != null) {
            builder.setAlreadyInTransactionBehavior(alreadyInTransactionBehavior);
        }
        builder.setTransactionOuter(transactionOuter())
                .setTraceEntryMessageTemplate(traceEntryMessageTemplate());
        Integer traceEntryStackThresholdMillis = traceEntryStackThresholdMillis();
        if (traceEntryStackThresholdMillis != null) {
            builder.setTraceEntryStackThresholdMillis(
                    OptionalInt32.newBuilder().setValue(traceEntryStackThresholdMillis));
        }
        return builder.setTraceEntryCaptureSelfNested(traceEntryCaptureSelfNested())
                .setTimerName(timerName())
                .setEnabledProperty(enabledProperty())
                .setTraceEntryEnabledProperty(traceEntryEnabledProperty())
                .build();
    }

    public static ImmutableInstrumentationConfig create(AgentConfig.InstrumentationConfig config) {
        @SuppressWarnings("deprecation")
        ImmutableInstrumentationConfig.Builder builder = ImmutableInstrumentationConfig.builder()
                .className(config.getClassName())
                .classAnnotation(config.getClassAnnotation())
                .subTypeRestriction(config.getSubTypeRestriction())
                .superTypeRestriction(config.getSuperTypeRestriction())
                // pointcuts with methodDeclaringClassName are no longer supported in 0.9.16, but
                // included here to help with transitioning of old instrumentation config
                .methodDeclaringClassName(config.getMethodDeclaringClassName())
                .methodName(config.getMethodName())
                .methodAnnotation(config.getMethodAnnotation())
                .addAllMethodParameterTypes(config.getMethodParameterTypeList())
                .methodReturnType(config.getMethodReturnType())
                .addAllMethodModifiers(config.getMethodModifierList())
                .nestingGroup(config.getNestingGroup())
                .order(config.getOrder())
                .captureKind(config.getCaptureKind())
                .transactionType(config.getTransactionType())
                .transactionNameTemplate(config.getTransactionNameTemplate())
                .transactionUserTemplate(config.getTransactionUserTemplate())
                .putAllTransactionAttributeTemplates(config.getTransactionAttributeTemplatesMap());
        if (config.hasTransactionSlowThresholdMillis()) {
            builder.transactionSlowThresholdMillis(
                    config.getTransactionSlowThresholdMillis().getValue());
        }
        if (config.getCaptureKind() == CaptureKind.TRANSACTION) {
            builder.alreadyInTransactionBehavior(config.getAlreadyInTransactionBehavior());
        }
        builder.transactionOuter(config.getTransactionOuter())
                .traceEntryMessageTemplate(config.getTraceEntryMessageTemplate());
        if (config.hasTraceEntryStackThresholdMillis()) {
            builder.traceEntryStackThresholdMillis(
                    config.getTraceEntryStackThresholdMillis().getValue());
        }
        return builder.traceEntryCaptureSelfNested(config.getTraceEntryCaptureSelfNested())
                .timerName(config.getTimerName())
                .enabledProperty(config.getEnabledProperty())
                .traceEntryEnabledProperty(config.getTraceEntryEnabledProperty())
                .build();
    }
}
