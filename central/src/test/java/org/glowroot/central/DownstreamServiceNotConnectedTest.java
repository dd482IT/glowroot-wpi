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
package org.glowroot.central;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.glowroot.central.util.ClusterManager;
import org.glowroot.common.live.LiveJvmService.AgentNotConnectedException;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig;
import org.glowroot.wire.api.model.DownstreamServiceOuterClass.MBeanDumpRequest.MBeanDumpKind;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DownstreamServiceNotConnectedTest {

    private static ClusterManager clusterManager;

    private DownstreamServiceImpl downstreamService =
            new DownstreamServiceImpl(mock(GrpcCommon.class), clusterManager);

    public static void setUp() throws Exception {
        clusterManager = ClusterManager.create();
    }

    public static void tearDown() throws Exception {
        clusterManager.close();
    }

    public void shouldNotThrowAgentNotConnectExceptionOnUpdateAgentConfig() throws Exception {
        downstreamService.updateAgentConfigIfConnected("a", AgentConfig.getDefaultInstance());
    }

    public void shouldThrowAgentNotConnectExceptionOnThreadDump() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.threadDump("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnAvailableDiskSpaceBytes() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.availableDiskSpaceBytes("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnHeapDump() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.heapDump("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnExplicitGcDisabled() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.isExplicitGcDisabled("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGc() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.forceGC("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnMbeanDump() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.mbeanDump("a", MBeanDumpKind.ALL_MBEANS_INCLUDE_ATTRIBUTES,
                    ImmutableList.of()));
    }

    public void shouldThrowAgentNotConnectExceptionOnMatchingMBeanObjectNames() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.matchingMBeanObjectNames("a", "b", 3));
    }

    public void shouldThrowAgentNotConnectExceptionOnMbeanMeta() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.mbeanMeta("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnCapabilities() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.capabilities("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGlobalMeta() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.globalMeta("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnPreloadClasspathCache() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.preloadClasspathCache("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnMatchingClassNames() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.matchingClassNames("a", "b", 3));
    }

    public void shouldThrowAgentNotConnectExceptionOnMatchingMethodNames() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.matchingMethodNames("a", "b", "c", 4));
    }

    public void shouldThrowAgentNotConnectExceptionOnMethodSignatures() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.methodSignatures("a", "b", "c"));
    }

    public void shouldThrowAgentNotConnectExceptionOnReweave() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.reweave("a"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGetHeader() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.getHeader("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGetEntries() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.getEntries("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOngetMainThreadProfile() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.getMainThreadProfile("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGetAuxThreadProfile() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.getAuxThreadProfile("a", "dummy"));
    }

    public void shouldThrowAgentNotConnectExceptionOnGetFullTrace() {
        assertThrows(AgentNotConnectedException.class, () ->
            downstreamService.getFullTrace("a", "dummy"));
    }
}
