/*
 * Copyright (c) 2016, Groupon, Inc.
 * All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. 
 *
 * Neither the name of GROUPON nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.groupon.lex.metrics.timeseries;

import com.groupon.lex.metrics.MetricMatcher;
import com.groupon.lex.metrics.MetricValue;
import com.groupon.lex.metrics.Tags;
import com.groupon.lex.metrics.config.ConfigStatement;
import com.groupon.lex.metrics.lib.Any2;
import com.groupon.lex.metrics.lib.SimpleMapEntry;
import com.groupon.lex.metrics.timeseries.expression.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author ariane
 */
public abstract class TimeSeriesMetricAggregate<T> implements TimeSeriesMetricExpression {
    private static final Logger LOG = Logger.getLogger(TimeSeriesMetricAggregate.class.getName());
    private final String fn_name_;
    private final Collection<MetricMatcher> matchers_;
    private final Collection<TimeSeriesMetricExpression> exprs_;
    private final TagAggregationClause aggregation_;

    public TimeSeriesMetricAggregate(String fn_name, Collection<Any2<MetricMatcher, TimeSeriesMetricExpression>> matchers, TagAggregationClause aggregation) {
        fn_name_ = requireNonNull(fn_name);
        matchers_ = new ArrayList<>(matchers.stream()
                .map((elem) -> elem.getLeft())
                .flatMap((x) -> x.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList()));
        exprs_ = unmodifiableList(new ArrayList<>(matchers.stream()
                .map((elem) -> elem.getRight())
                .flatMap((x) -> x.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList())));
        aggregation_ = requireNonNull(aggregation);
    }

    @Override
    public Collection<TimeSeriesMetricExpression> getChildren() { return exprs_; }

    protected abstract T initial_();
    protected MetricValue scalar_fallback_() { return MetricValue.EMPTY; }
    protected abstract T map_(MetricValue x);
    protected abstract T reducer_(T x, T y);
    protected abstract MetricValue unmap_(T v);

    @Override
    public TimeSeriesMetricDeltaSet apply(Context t) {
        /* Fetch each metric wildcard and add it to the to-be-processed list. */
        final List<Map.Entry<Tags, MetricValue>> matcher_tsvs = matchers_.stream()
                .flatMap(m -> m.filter(t))
                .map(named_entry -> SimpleMapEntry.create(named_entry.getKey().getTags(), named_entry.getValue()))
                .collect(Collectors.toList());
        /* Resolve each expression and resolve it. */
        final Map<Boolean, List<TimeSeriesMetricDeltaSet>> expr_tsvs_map = exprs_.stream()
                .map((expr) -> expr.apply(t))
                .collect(Collectors.partitioningBy(TimeSeriesMetricDeltaSet::isVector));
        final List<TimeSeriesMetricDeltaSet> expr_tsvs = Optional.ofNullable(expr_tsvs_map.get(true)).orElse(Collections.emptyList());
        final T scalar = Optional.ofNullable(expr_tsvs_map.get(false))
                .map(Collection::stream)
                .orElseGet(Stream::<TimeSeriesMetricDeltaSet>empty)
                .map(tsv_set -> map_(tsv_set.asScalar().get()))
                .reduce(initial_(), (x, y) -> reducer_(x, y));

        /*
         * Reduce everything using the reducer (in the derived class).
         */
        final Stream<Map.Entry<Tags, T>> map = aggregation_.apply(
                        matcher_tsvs.stream(), expr_tsvs.stream().flatMap(TimeSeriesMetricDeltaSet::streamAsMap),
                        Map.Entry::getKey, Map.Entry::getKey,
                        Map.Entry::getValue, Map.Entry::getValue)
                .entrySet().stream()
                .map(entry -> {
                    final T aggregated_value = entry.getValue().stream().map(this::map_)
                            .reduce(scalar, this::reducer_);
                    return SimpleMapEntry.create(entry.getKey(), aggregated_value);
                });

        final TimeSeriesMetricDeltaSet result;
        if (aggregation_.isScalar()) {
            result = new TimeSeriesMetricDeltaSet(map.map(Map.Entry::getValue).reduce(this::reducer_).map(this::unmap_).orElseGet(this::scalar_fallback_));
        } else {
            result = new TimeSeriesMetricDeltaSet(map.map(entry -> SimpleMapEntry.create(entry.getKey(), unmap_(entry.getValue()))));
        }
        LOG.log(Level.FINE, "{0} yields {1}", new Object[]{fn_name_, result});
        return result;
    }

    protected String configStringArgs() {
        return Stream.concat(matchers_.stream().map(MetricMatcher::configString), exprs_.stream().map(ConfigStatement::configString))
                .collect(Collectors.joining(", "));
    }

    @Override
    public StringBuilder configString() {
        final StringBuilder rv = new StringBuilder()
                .append(fn_name_)
                .append('(')
                .append(configStringArgs())
                .append(')');

        final StringBuilder agg_cfg = aggregation_.configString();
        if (agg_cfg.length() > 0) rv.append(' ').append(agg_cfg);

        return rv;
    }
}
