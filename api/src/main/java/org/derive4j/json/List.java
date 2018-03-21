package org.derive4j.json;

import java.util.function.Consumer;
import java.util.function.Function;
import org.derive4j.Data;

import static org.derive4j.json.Lists.Cons;
import static org.derive4j.json.Lists.cases;
import static org.derive4j.json.Lists.lazy;

@Data
public interface List<A> {

  interface Cases<A, X> extends NonEmptyList.Cases<A, X> {
    X Nil();

    @Override
    X Cons(A head, List<A> tail);
  }

  default <B> List<B> map(Function<A, B> f) {
    return Lists.<A, List<B>>cata(Lists::Nil, (head, tail) -> Cons(f.apply(head), lazy(tail))).apply(this);
  }

  static <A, B> List<B> map(List<A> as, Cases<A, List<B>> cases) {
    return lazy(() -> as.match(cases));
  }

  static <A, B> Function<List<A>, List<B>> intersperseMap(B b, Function<A, B> f) {

    Function<List<A>, List<B>> prependToAll = new Object() {
      Function<List<A>, List<B>> prependToAll = Lists.<A>cases()
          .Nil_(Lists.<B>Nil())
          .Cons((head, tail) -> Cons(b, Cons(f.apply(head), lazy(() -> this.prependToAll.apply(tail)))));

    }.prependToAll;

    Function<List<A>, List<B>> intersperse = Lists.<A>cases()
        .Nil_(Lists.<B>Nil())
        .Cons((head, tail) -> Cons(f.apply(head), lazy(() -> prependToAll.apply(tail))));

    return as -> lazy(() -> intersperse.apply(as));
  }

  <X> X match(Cases<A, X> cases);

  default void forEach(Consumer<A> effect) {
    // a bit ugly due to lack of TCO
    class Visitor implements Cases<A, Boolean> {
      List<A> l = List.this;

      @Override
      public Boolean Nil() {
        return false;
      }

      @Override
      public Boolean Cons(A head, List<A> tail) {
        effect.accept(head);
        l = tail;
        return true;
      }
    }
    Visitor visitor = new Visitor();
    while (visitor.l.match(visitor)) {
    }
  }

}
