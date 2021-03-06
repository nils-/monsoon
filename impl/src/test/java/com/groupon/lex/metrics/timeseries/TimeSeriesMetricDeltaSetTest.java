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

import com.groupon.lex.metrics.MetricValue;
import com.groupon.lex.metrics.Tags;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ariane
 */
public class TimeSeriesMetricDeltaSetTest {
    private TimeSeriesMetricDeltaSet empty, scalar23, vector;

    @Before
    public void setup() {
        empty = new TimeSeriesMetricDeltaSet();
        scalar23 = new TimeSeriesMetricDeltaSet(MetricValue.fromIntValue(23));
        vector = new TimeSeriesMetricDeltaSet(singletonMap(
                new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))),
                MetricValue.fromStrValue("baz")));
    }

    @Test
    public void emptySet() {
        assertTrue(empty.isEmpty());
        assertEquals(0L, empty.streamValues().count());
        assertEquals(0L, empty.streamAsMap().count());
        assertEquals(0, empty.size());
        assertFalse(empty.isScalar());
        assertTrue(empty.isVector());
        assertEquals(Optional.empty(), empty.asScalar());
        assertNotEquals(Optional.empty(), empty.asVector());
        assertTrue(empty.asVector().get().isEmpty());
    }

    @Test
    public void scalar23Set() {
        assertFalse(scalar23.isEmpty());
        assertThat(scalar23.streamValues().collect(Collectors.toList()),
                hasItem(MetricValue.fromIntValue(23)));
        assertThat(scalar23.streamAsMap().collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
                hasEntry(Tags.EMPTY, MetricValue.fromIntValue(23)));
        assertEquals(1, scalar23.size());
        assertTrue(scalar23.isScalar());
        assertFalse(scalar23.isVector());
        assertEquals(Optional.of(MetricValue.fromIntValue(23)), scalar23.asScalar());
        assertEquals(Optional.empty(), scalar23.asVector());
        assertEquals(MetricValue.fromIntValue(23), scalar23.asScalar().get());
    }

    @Test
    public void vectorTest() {
        assertFalse(vector.isEmpty());
        assertThat(vector.streamValues().collect(Collectors.toList()),
                hasItem(MetricValue.fromStrValue("baz")));
        assertThat(vector.streamAsMap().collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromStrValue("baz")));
        assertEquals(1, vector.size());
        assertFalse(vector.isScalar());
        assertTrue(vector.isVector());
        assertEquals(Optional.empty(), vector.asScalar());
        assertNotEquals(Optional.empty(), vector.asVector());
        assertThat(vector.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromStrValue("baz")));
    }

    @Test
    public void empty_mapping() {
        TimeSeriesMetricDeltaSet tsd = empty.map(x -> x); // Identity

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void empty_opt_mapping() {
        TimeSeriesMetricDeltaSet opt_set = empty.mapOptional(x -> Optional.of(x)); // Identity
        TimeSeriesMetricDeltaSet opt_empty = empty.mapOptional(x -> Optional.empty()); // Identity

        assertTrue(opt_set.isEmpty());
        assertTrue(opt_empty.isEmpty());
    }

    @Test
    public void scalar23_mapping() {
        TimeSeriesMetricDeltaSet tsd = scalar23.map(x -> MetricValue.TRUE);

        assertEquals(Optional.of(MetricValue.TRUE),
                tsd.asScalar());
    }

    @Test
    public void scalar23_opt_mapping() {
        TimeSeriesMetricDeltaSet opt_set = scalar23.mapOptional(x -> Optional.of(MetricValue.TRUE));
        TimeSeriesMetricDeltaSet opt_empty = scalar23.mapOptional(x -> Optional.empty());

        assertEquals(Optional.of(MetricValue.TRUE),
                opt_set.asScalar());
        assertEquals(Optional.of(MetricValue.EMPTY),
                opt_empty.asScalar());
    }

    @Test
    public void vector_mapping() {
        TimeSeriesMetricDeltaSet tsd = vector.map(x -> MetricValue.TRUE);

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.TRUE));
    }

    @Test
    public void vector_opt_mapping() {
        TimeSeriesMetricDeltaSet opt_set = vector.mapOptional(x -> Optional.of(MetricValue.TRUE));
        TimeSeriesMetricDeltaSet opt_empty = vector.mapOptional(x -> Optional.empty());

        assertThat(opt_set.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.TRUE));
        assertThat(opt_empty.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.EMPTY));
    }

    @Test
    public void empty_empty_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(empty, empty, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void empty_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(empty, scalar23, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void empty_vector_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(empty, vector, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void scalar_empty_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(scalar23, empty, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void scalar_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(scalar23, scalar23, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertEquals(Optional.of(MetricValue.fromIntValue(23 * 23)),
                tsd.asScalar());
    }

    @Test
    public void scalar_vector_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(scalar23, vector, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromIntValue(23 * 23)));
    }

    @Test
    public void vector_empty_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(vector, empty, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertTrue(tsd.isEmpty());
    }

    @Test
    public void vector_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(vector, scalar23, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromIntValue(23 * 23)));
    }

    @Test
    public void vector_vector_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(vector, vector, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromIntValue(23 * 23)));
    }

    @Test
    public void vector_vector_apply_mismatch() {
        final TimeSeriesMetricDeltaSet other_vector = new TimeSeriesMetricDeltaSet(singletonMap(
                new Tags(singletonMap("NOT MY", MetricValue.fromStrValue("TAGS"))),
                MetricValue.fromStrValue("baz")));
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(vector, other_vector, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.streamAsMap().collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
                allOf(not(hasKey(new Tags(singletonMap("NOT MY", MetricValue.fromStrValue("TAGS"))))),
                        not(hasKey(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar")))))));
        assertTrue("TSD must be empty", tsd.isEmpty());
    }

    @Test
    public void vector_immediate_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(vector, MetricValue.fromIntValue(23), (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromIntValue(23 * 23)));
    }

    @Test
    public void immediate_scalar_vector_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(MetricValue.fromIntValue(23), vector, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertThat(tsd.asVector().get(),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromIntValue(23 * 23)));
    }

    @Test
    public void scalar_immediate_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(scalar23, MetricValue.fromIntValue(23), (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertEquals(MetricValue.fromIntValue(23 * 23), tsd.asScalar().get());
    }

    @Test
    public void immediate_scalar_scalar_apply() {
        TimeSeriesMetricDeltaSet tsd = TimeSeriesMetricDeltaSet.apply(MetricValue.fromIntValue(23), scalar23, (x, y) -> MetricValue.fromIntValue(23 * 23));

        assertEquals(MetricValue.fromIntValue(23 * 23), tsd.asScalar().get());
    }

    @Test
    public void filter_tags() {
        assertThat(vector.filterTags(singleton("foo")).streamAsMap().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())),
                hasEntry(new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))), MetricValue.fromStrValue("baz")));

        assertThat(vector.filterTags(singleton("baz")).streamAsMap().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())),
                hasEntry(Tags.EMPTY, MetricValue.fromStrValue("baz")));

        assertThat(vector.filterTags(Collections.EMPTY_SET).streamAsMap().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue())),
                hasEntry(Tags.EMPTY, MetricValue.fromStrValue("baz")));
    }

    @Test(expected = IllegalStateException.class)
    public void fail_with_duplicate_keys() {
        Map<Tags, MetricValue> key = singletonMap(
                new Tags(singletonMap("foo", MetricValue.fromStrValue("bar"))),
                MetricValue.fromStrValue("baz"));

        new TimeSeriesMetricDeltaSet(Stream.of(key, key).map(Map::entrySet).flatMap(Collection::stream));
    }
}
