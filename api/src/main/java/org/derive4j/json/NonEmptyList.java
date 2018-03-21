package org.derive4j.json;

import org.derive4j.Data;

@Data
public interface NonEmptyList<A> {

  interface Cases<A, X> {
    X Cons(A head, List<A> tail);
  }

  <X> X match(Cases<A, X> cases);

  default List<A> asList() {
    return this::match;
  }

}
