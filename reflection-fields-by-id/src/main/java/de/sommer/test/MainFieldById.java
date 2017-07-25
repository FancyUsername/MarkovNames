package de.sommer.test;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

public class MainFieldById {
	public static void main(String[] args) throws IntrospectionException, IOException {
		List<PropertyDescriptor> properties = Stream.of(Introspector.getBeanInfo(Person.class).getPropertyDescriptors())
				.filter(d -> !d.getName().equals("class"))
				.collect(Collectors.toList());
		System.out.println(properties.stream().map(d -> d.getName()).collect(Collectors.joining("\t")));
		String file = "kafka_en.txt";
		Markov<Character> markov = Markov.charFromStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
		printPersons(getPersons(50, markov), properties);
	}

	private static void printPersons(Stream<Person> persons, List<PropertyDescriptor> properties) {
		Function<Person, String> toString = person -> properties.stream().map(p -> q(() -> p.getReadMethod().invoke(person)).toString()).collect(Collectors.joining("\t"));
		persons.map(toString).forEach(System.out::println);
	}

	private static Stream<Person> getPersons(int count, Markov<Character> markov) throws IOException {
		return IntStream.range(0, count).mapToObj(i -> new Person(
				StringUtils.capitalize(markov.random(3 + (int) (Math.random() * 10))),
				StringUtils.capitalize(markov.random(3 + (int) (Math.random() * 10))),
				(int) (Math.random() * 10),
				Math.random() > 0.5));
	}

	public static interface Sup<T> {
		T get() throws Exception;
	}

	public static <T> T q(Sup<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
