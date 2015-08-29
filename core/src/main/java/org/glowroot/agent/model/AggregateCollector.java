/*
 * Copyright 2013-2015 the original author or authors.
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
package org.glowroot.agent.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import org.glowroot.agent.model.ThreadInfoComponent.ThreadInfoData;
import org.glowroot.collector.spi.Aggregate;
import org.glowroot.common.config.AdvancedConfig;
import org.glowroot.common.repo.AggregateRepository.ErrorPoint;
import org.glowroot.common.repo.AggregateRepository.OverallErrorSummary;
import org.glowroot.common.repo.AggregateRepository.OverallSummary;
import org.glowroot.common.repo.AggregateRepository.OverviewAggregate;
import org.glowroot.common.repo.AggregateRepository.PercentileAggregate;
import org.glowroot.common.repo.AggregateRepository.TransactionErrorSummary;
import org.glowroot.common.repo.AggregateRepository.TransactionSummary;
import org.glowroot.common.repo.ImmutableErrorPoint;
import org.glowroot.common.repo.ImmutableOverallErrorSummary;
import org.glowroot.common.repo.ImmutableOverallSummary;
import org.glowroot.common.repo.ImmutableOverviewAggregate;
import org.glowroot.common.repo.ImmutablePercentileAggregate;
import org.glowroot.common.repo.ImmutableTransactionErrorSummary;
import org.glowroot.common.repo.ImmutableTransactionSummary;
import org.glowroot.common.repo.LazyHistogram;
import org.glowroot.common.repo.MutableProfileNode;
import org.glowroot.common.repo.MutableQuery;
import org.glowroot.common.repo.MutableTimerNode;
import org.glowroot.common.repo.QueryCollector;
import org.glowroot.common.util.Styles;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

// must be used under an appropriate lock
@Styles.Private
@Value.Include(Aggregate.class)
class AggregateCollector {

    private final @Nullable String transactionName;
    private long totalMicros;
    private long errorCount;
    private long transactionCount;
    private long totalCpuTime = ThreadInfoData.NOT_AVAILABLE;
    private long totalBlockedTime = ThreadInfoData.NOT_AVAILABLE;
    private long totalWaitedTime = ThreadInfoData.NOT_AVAILABLE;
    private long totalAllocatedBytes = ThreadInfoData.NOT_AVAILABLE;
    // histogram uses microseconds to reduce (or at least simplify) bucket allocations
    private final LazyHistogram lazyHistogram = new LazyHistogram();
    private final MutableTimerNode syntheticRootTimerNode =
            MutableTimerNode.createSyntheticRootNode();
    private final QueryCollector mergedQueries;
    private final AggregateProfileBuilder aggregateProfile = new AggregateProfileBuilder();

    AggregateCollector(@Nullable String transactionName, int maxAggregateQueriesPerQueryType) {
        int hardLimitMultiplierWhileBuilding = transactionName == null
                ? AdvancedConfig.OVERALL_AGGREGATE_QUERIES_HARD_LIMIT_MULTIPLIER
                : AdvancedConfig.TRANSACTION_AGGREGATE_QUERIES_HARD_LIMIT_MULTIPLIER;
        mergedQueries = new QueryCollector(maxAggregateQueriesPerQueryType,
                hardLimitMultiplierWhileBuilding);
        this.transactionName = transactionName;
    }

    void add(Transaction transaction) {
        long durationMicros = NANOSECONDS.toMicros(transaction.getDuration());
        totalMicros += durationMicros;
        if (transaction.getErrorMessage() != null) {
            errorCount++;
        }
        transactionCount++;
        ThreadInfoData threadInfo = transaction.getThreadInfo();
        if (threadInfo != null) {
            totalCpuTime = notAvailableAwareAdd(totalCpuTime, threadInfo.threadCpuTime());
            totalBlockedTime =
                    notAvailableAwareAdd(totalBlockedTime, threadInfo.threadBlockedTime());
            totalWaitedTime = notAvailableAwareAdd(totalWaitedTime, threadInfo.threadWaitedTime());
            totalAllocatedBytes =
                    notAvailableAwareAdd(totalAllocatedBytes, threadInfo.threadAllocatedBytes());
        }
        lazyHistogram.add(durationMicros);
    }

    void addToTimers(TimerImpl rootTimer) {
        syntheticRootTimerNode.mergeAsChildTimer(rootTimer);
    }

    void addToQueries(Iterable<QueryData> queries) {
        for (QueryData query : queries) {
            mergedQueries.mergeQuery(query.getQueryType(), query);
        }
    }

    void addToProfile(Profile profile) {
        aggregateProfile.addProfile(profile);
    }

    Aggregate build(long captureTime) throws IOException {
        return new AggregateBuilder()
                .captureTime(captureTime)
                .totalMicros(totalMicros)
                .errorCount(errorCount)
                .transactionCount(transactionCount)
                .totalCpuMicros(notAvailableAwareNanosToMicros(totalCpuTime))
                .totalBlockedMicros(notAvailableAwareNanosToMicros(totalBlockedTime))
                .totalWaitedMicros(notAvailableAwareNanosToMicros(totalWaitedTime))
                .totalAllocatedKBytes(notAvailableAwareBytesToKBytes(totalAllocatedBytes))
                .histogram(lazyHistogram)
                .syntheticRootTimerNode(syntheticRootTimerNode)
                .queries(getQueries())
                .syntheticRootProfileNode(getSyntheticRootProfileNode())
                .build();
    }

    OverviewAggregate buildLiveOverviewAggregate(long captureTime) throws IOException {
        return ImmutableOverviewAggregate.builder()
                .captureTime(captureTime)
                .totalMicros(totalMicros)
                .transactionCount(transactionCount)
                .totalCpuMicros(notAvailableAwareNanosToMicros(totalCpuTime))
                .totalBlockedMicros(notAvailableAwareNanosToMicros(totalBlockedTime))
                .totalWaitedMicros(notAvailableAwareNanosToMicros(totalWaitedTime))
                .totalAllocatedKBytes(notAvailableAwareBytesToKBytes(totalAllocatedBytes))
                .syntheticRootTimer(syntheticRootTimerNode)
                .build();
    }

    PercentileAggregate buildLivePercentileAggregate(long captureTime) throws IOException {
        return ImmutablePercentileAggregate.builder()
                .captureTime(captureTime)
                .totalMicros(totalMicros)
                .transactionCount(transactionCount)
                .histogram(lazyHistogram)
                .build();
    }

    OverallSummary getLiveOverallSummary() {
        return ImmutableOverallSummary.builder()
                .totalMicros(totalMicros)
                .transactionCount(transactionCount)
                .build();
    }

    TransactionSummary getLiveTransactionSummary() {
        // this method should not be called on overall aggregate
        checkNotNull(transactionName);
        return ImmutableTransactionSummary.builder()
                .transactionName(transactionName)
                .totalMicros(totalMicros)
                .transactionCount(transactionCount)
                .build();
    }

    OverallErrorSummary getLiveOverallErrorSummary() {
        return ImmutableOverallErrorSummary.builder()
                .errorCount(errorCount)
                .transactionCount(transactionCount)
                .build();
    }

    TransactionErrorSummary getLiveTransactionErrorSummary() {
        // this method should not be called on overall aggregate
        checkNotNull(transactionName);
        return ImmutableTransactionErrorSummary.builder()
                .transactionName(transactionName)
                .errorCount(errorCount)
                .transactionCount(transactionCount)
                .build();
    }

    // needs to return copy for thread safety
    MutableProfileNode getLiveProfile() throws IOException {
        return aggregateProfile.getSyntheticRootNode().copy();
    }

    // needs to return copy for thread safety
    Map<String, List<MutableQuery>> getLiveQueries() {
        return mergedQueries.copy();
    }

    @Nullable
    ErrorPoint buildErrorPoint(long captureTime) {
        if (errorCount == 0) {
            return null;
        }
        return ImmutableErrorPoint.builder()
                .captureTime(captureTime)
                .errorCount(errorCount)
                .transactionCount(transactionCount)
                .build();
    }

    private Map<String, List<MutableQuery>> getQueries() throws IOException {
        return mergedQueries.getOrderedAndTruncatedQueries();
    }

    @Nullable
    private MutableProfileNode getSyntheticRootProfileNode() throws IOException {
        MutableProfileNode syntheticRootNode = aggregateProfile.getSyntheticRootNode();
        if (syntheticRootNode.isEmpty()) {
            return null;
        }
        return syntheticRootNode;
    }

    private static long notAvailableAwareNanosToMicros(long nanoseconds) {
        if (nanoseconds == ThreadInfoData.NOT_AVAILABLE) {
            return ThreadInfoData.NOT_AVAILABLE;
        }
        return NANOSECONDS.toMicros(nanoseconds);
    }

    private static long notAvailableAwareBytesToKBytes(long bytes) {
        if (bytes == ThreadInfoData.NOT_AVAILABLE) {
            return ThreadInfoData.NOT_AVAILABLE;
        }
        return bytes / 1024;
    }

    private static long notAvailableAwareAdd(long x, long y) {
        if (x == ThreadInfoData.NOT_AVAILABLE) {
            return y;
        }
        if (y == ThreadInfoData.NOT_AVAILABLE) {
            return x;
        }
        return x + y;
    }
}