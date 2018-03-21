package org.derive4j.json;

import java.util.function.BiFunction;
import org.derive4j.Data;
import org.derive4j.FieldNames;

@Data
public interface ValidationNel<E, A> {

  interface Cases<E, A, X> {
    X failure(NonEmptyList<E> failures);
    X success(A  value);
  }

  <X> X match(Cases<E, A, X> cases);

  //static sequence()

}