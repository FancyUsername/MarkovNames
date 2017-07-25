package de.sommer.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Markov<T> {
	public static Markov<Character> charFromStream(InputStream stream) throws IOException {
		Reader reader = new InputStreamReader(stream, "UTF-8");
		Markov<Character> markov = new Markov<Character>();
		int c, lastc = 0;
		while ((c = reader.read()) != -1) {
			if (Character.isAlphabetic(c)) {
				c = Character.toLowerCase(c);
				if (lastc != 0) {
					markov.add((char) lastc, (char) c);
				}
				lastc = c;
			}
		}
		return markov;
	}
	
	public class Propability {
		T item;
		double propability;

		public Propability(T item, double propability) {
			this.item = item;
			this.propability = propability;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(item);
			builder.append("=");
			builder.append(propability);
			return builder.toString();
		}
	}
	
	Map<T, Map<T, Integer>> map = new HashMap<>();
	Map<T, List<Propability>> propabilities = new HashMap<>();
	List<T> keys;
	
	public void add(T prev, T next) {
		Map<T, Integer> map2 = map.get(prev);
		if (map2 == null) {
			map.put(next, map2 = new HashMap<T, Integer>());
		}
		map2.put(next, map2.getOrDefault(next, 0) + 1);
	}
	
	public String random(int size) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < size; i++) {
			sb.append(next(getKey((int) (Math.random() * map.size()))));
		}
		
		return sb.toString();
	}

	private T next(T prev) {
		List<Propability> list = get(prev);

		double totalProp = 0.0;
		
		for (int i = 0; i < list.size(); i++) {
			totalProp += list.get(i).propability;
			
			if (totalProp >= Math.random()) {
				return list.get(i).item;
			}
		}
		
		return list.get(list.size() - 1).item;
	}

	private T getKey(int index) {
		if (keys == null) {
			keys = new ArrayList<>(map.keySet());
		}
		return keys.get(index);
	}

	public List<Propability> get(T prev) {
		List<Propability> list = propabilities.get(prev);
		
		if (list == null) {
			propabilities.put(prev, list = new ArrayList<>(map.get(prev).size()));
			int total = map.get(prev).values().stream().mapToInt(e -> e.intValue()).sum();
			final List<Propability> list2 = list;
			map.get(prev).entrySet().stream().map(e -> new Propability(e.getKey(), e.getValue() / (double) total)).collect(Collectors.toCollection(() -> list2));
			Collections.sort(list, (b, a) -> Double.compare(a.propability, b.propability));
		}
		
		return propabilities.get(prev);
	}
}
