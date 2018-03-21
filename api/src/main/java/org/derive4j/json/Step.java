package org.derive4j.json;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.derive4j.Data;
import org.derive4j.FieldNames;

@Data
public abstract class Step<S, A> {
  abstract <X> X match(@FieldNames("value") Function<A, X> done, @FieldNames({ "stepper", "value" }) BiFunction<S, A, X> yield);
}
