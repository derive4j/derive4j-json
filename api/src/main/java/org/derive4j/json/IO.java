package org.derive4j.json;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IO<A> {

  A run() throws IOException;

  default <B> IO<B> bind(Function<A, IO<B>> f) {
    return () -> f.apply(run()).run();
  }

  default <B> IO<B> map(Function<A, B> f) {
    return () -> f.apply(run());
  }

  interface Effect extends IO<Unit> {
    void runEffect() throws IOException;

    default Unit run() throws IOException {
      runEffect();
      return Unit.unit;
    }
  }

  static IO<Unit> effect(Effect effect) {
    return effect;
  }

  static <A, B> IO<Unit> sequence_(List<A> as, Function<A, IO<B>> f) {
    // a bit ugly due to lack of TCO
    class Visitor implements List.Cases<A, IO<Boolean>> {
      List<A> l = as;

      @Override
      public IO<Boolean> Nil() {
        return () -> false;
      }

      @Override
      public IO<Boolean> Cons(A head, List<A> tail) {
        IO<B> io = f.apply(head);
        l = tail;
        return () -> {
          io.run();
          return true;
        };
      }
    }
    Visitor visitor = new Visitor();
    return  effect(() -> {while (visitor.l.match(visitor).run()) {}});
  }

  static <A> IO<Unit> sequence_(List<IO<A>> as) {
    // a bit ugly due to lack of TCO
    class Visitor implements List.Cases<IO<A>, IO<Boolean>> {
      List<IO<A>> l = as;

      @Override
      public IO<Boolean> Nil() {
        return () -> false;
      }

      @Override
      public IO<Boolean> Cons(IO<A> head, List<IO<A>> tail) {
        l = tail;
        return () -> {
          head.run();
          return true;
        };
      }
    }
    Visitor visitor = new Visitor();
    return  effect(() -> {while (visitor.l.match(visitor).run()) {}});
  }

}
