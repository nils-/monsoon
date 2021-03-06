package com.groupon.lex.metrics.timeseries;

import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 *
 * @author ariane
 */
public class ImmutableTimeSeriesCollectionPair implements TimeSeriesCollectionPair {
    private final List<TimeSeriesCollection> history_;

    public ImmutableTimeSeriesCollectionPair(List<? extends TimeSeriesCollection> tsc) {
        history_ = unmodifiableList(tsc);
    }

    public static TimeSeriesCollectionPair copyList(List<? extends TimeSeriesCollection> tsc) {
        return new ImmutableTimeSeriesCollectionPair(new ArrayList<>(tsc));
    }

    @Override
    public TimeSeriesCollection getCurrentCollection() {
        if (history_.isEmpty()) return TimeSeriesCollection.empty();
        return history_.get(0);
    }

    @Override
    public TimeSeriesCollection getPreviousCollection() {
        if (history_.isEmpty()) return TimeSeriesCollection.empty();
        if (history_.size() == 1) return TimeSeriesCollection.empty(history_.get(0).getTimestamp());
        return history_.get(1);
    }

    @Override
    public Optional<TimeSeriesCollection> getPreviousCollection(int n) {
        if (history_.size() <= n) return Optional.empty();
        return Optional.of(history_.get(n));
    }

    @Override
    public Optional<TimeSeriesCollection> getPreviousCollection(Duration duration) {
        if (history_.isEmpty()) return Optional.empty();
        final DateTime ts = getCurrentCollection().getTimestamp().minus(duration);
        final ListIterator<TimeSeriesCollection> iter = history_.listIterator(1);
        while (iter.hasNext()) {
            final TimeSeriesCollection next = iter.next();
            if (!next.getTimestamp().isAfter(ts)) return Optional.of(next);
        }
        return Optional.empty();
    }

    @Override
    public TimeSeriesCollectionPair getPreviousCollectionPair(int n) {
        if (history_.size() <= n) return new ImmutableTimeSeriesCollectionPair(EMPTY_LIST);
        return new ImmutableTimeSeriesCollectionPair(history_.subList(n, history_.size()));
    }

    @Override
    public TimeSeriesCollectionPair getPreviousCollectionPair(Duration duration) {
        if (history_.isEmpty()) return new ImmutableTimeSeriesCollectionPair(EMPTY_LIST);
        final DateTime ts = getCurrentCollection().getTimestamp().minus(duration);
        final ListIterator<TimeSeriesCollection> iter = history_.listIterator(1);
        while (iter.hasNext()) {
            final int iter_idx = iter.nextIndex();
            final TimeSeriesCollection next = iter.next();
            if (!next.getTimestamp().isAfter(ts)) return new ImmutableTimeSeriesCollectionPair(history_.subList(iter_idx, history_.size()));
        }
        return new ImmutableTimeSeriesCollectionPair(EMPTY_LIST);
    }
}
