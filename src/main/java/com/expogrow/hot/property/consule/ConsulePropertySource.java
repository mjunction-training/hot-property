package com.expogrow.hot.property.consule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.KeyValueClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConsulePropertySource extends AbstractExecutionThreadService implements WatchedConfigurationSource {

	private final String rootPath;
	private final KeyValueClient client;
	private final long watchIntervalSeconds;

	private final AtomicReference<ImmutableMap<String, Object>> lastState = new AtomicReference<>(null);
	private final AtomicLong latestIndex = new AtomicLong(0);

	private final List<WatchedUpdateListener> listeners = new CopyOnWriteArrayList<>();

	public ConsulePropertySource(final String rootPath, final KeyValueClient client) {
		this(rootPath, client, 10, TimeUnit.SECONDS);
	}

	public ConsulePropertySource(final String rootPath, final KeyValueClient client, final long watchInterval,
			final TimeUnit watchIntervalUnit) {
		this.rootPath = checkNotNull(rootPath);
		this.client = checkNotNull(client);
		watchIntervalSeconds = watchIntervalUnit.toSeconds(watchInterval);
	}

	private Response<List<GetValue>> getRaw(final QueryParams params) {
		return client.getKVValues(rootPath, params);
	}

	private Response<List<GetValue>> updateIndex(final Response<List<GetValue>> response) {
		if (response != null) {
			latestIndex.set(response.getConsulIndex());
		}
		return response;
	}

	private WatchedUpdateResult incrementalResult(final ImmutableMap<String, Object> newState,
			final ImmutableMap<String, Object> previousState) {

		final Map<String, Object> added = Maps.newHashMap();
		final Map<String, Object> removed = Maps.newHashMap();
		final Map<String, Object> changed = Maps.newHashMap();

		// added
		addAllKeys(Sets.difference(newState.keySet(), previousState.keySet()), newState, added);

		// removed
		addAllKeys(Sets.difference(previousState.keySet(), newState.keySet()), previousState, removed);

		// changed
		addFilteredKeys(Sets.intersection(previousState.keySet(), newState.keySet()), newState, changed,
				key -> !previousState.get(key).equals(newState.get(key)));

		return WatchedUpdateResult.createIncremental(added, changed, removed);
	}

	private void addAllKeys(final Set<String> keys, final ImmutableMap<String, Object> source,
			final Map<String, Object> dest) {
		addFilteredKeys(keys, source, dest, input -> true);
	}

	private void addFilteredKeys(final Set<String> keys, final ImmutableMap<String, Object> source,
			final Map<String, Object> dest, final Predicate<String> filter) {

		for (final String key : keys) {
			if (filter.apply(key)) {
				dest.put(key, source.get(key));
			}
		}

	}

	protected void fireEvent(final WatchedUpdateResult result) {
		for (final WatchedUpdateListener l : listeners) {
			try {
				l.updateConfiguration(result);
			} catch (final Throwable ex) {
				log.error(() -> "Error invoking WatchedUpdateListener", ex);
			}
		}
	}

	@Override
	public void addUpdateListener(final WatchedUpdateListener l) {
		if (l != null) {
			listeners.add(l);
		}
	}

	@Override
	public void removeUpdateListener(final WatchedUpdateListener l) {
		if (l != null) {
			listeners.remove(l);
		}
	}

	@Override
	public Map<String, Object> getCurrentData() throws Exception {
		return lastState.get();
	}

	@VisibleForTesting
	protected long getLatestIndex() {
		return latestIndex.get();
	}

	private ImmutableMap<String, Object> convertToMap(final Response<List<GetValue>> kv) {
		if (kv == null || kv.getValue() == null) {
			return ImmutableMap.of();
		}
		final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		for (final GetValue gv : kv.getValue()) {
			builder.put(keyFunc(gv), valFunc(gv));
		}
		return builder.build();
	}

	private Object valFunc(final GetValue getValue) {
		return new String(base64().decode(getValue.getValue())).trim();
	}

	private String keyFunc(final GetValue getValue) {
		return getValue.getKey().substring(rootPath.length() + 1);
	}

	@Override
	protected void run() throws Exception {
		while (isRunning()) {
			runOnce();
		}

	}

	@VisibleForTesting
	protected void runOnce() {
		try {
			final Response<List<GetValue>> kvals = updateIndex(getRaw(watchParams()));
			final ImmutableMap<String, Object> full = convertToMap(kvals);
			final WatchedUpdateResult result;
			if (lastState.get() == null) {
				result = WatchedUpdateResult.createFull(full);
			} else {
				result = incrementalResult(full, lastState.get());
			}
			lastState.set(full);
			fireEvent(result);
		} catch (final Exception e) {
			log.error(() -> "Error watching path", e);
		}
	}

	private QueryParams watchParams() {
		return new QueryParams(watchIntervalSeconds, latestIndex.get());
	}

}