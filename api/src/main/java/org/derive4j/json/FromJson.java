package org.derive4j.json;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.derive4j.json.Json.JAssoc;

import static org.derive4j.json.JAssocs.JAssoc;
import static org.derive4j.json.Jsons.JsonArray;
import static org.derive4j.json.Jsons.JsonBool;
import static org.derive4j.json.Jsons.JsonNull;
import static org.derive4j.json.Jsons.JsonNumber;
import static org.derive4j.json.Jsons.JsonObject;
import static org.derive4j.json.Jsons.JsonString;
import static org.derive4j.json.Lists.Nil;
import static org.derive4j.json.Steps.yield;

public interface FromJson<A> {

  interface ArrayFold<A, X> {
    <S, T> X run(S init, Function<S, Step<FromJson<T>, A>> stepper, BiFunction<T, S, S> merge);
  }

  interface ObjectFold<A, X> {
    <S, T> X run(S init, Function<S, Step<Function<String, FromJson<T>>, A>> stepper, BiFunction<T, S, S> merge);
  }

  A fromNull();

  A fromBool(boolean value);

  A fromString(String value);

  A fromNumber(BigDecimal value);

  <X> X fromArray(ArrayFold<A, X> fold);

  <X> X fromObject(ObjectFold<A, X> fold);

  default <B> FromJson<B> map(Function<A, B> f) {
    FromJson<A> self = this;
    return new FromJson<B>() {
      @Override
      public B fromNull() {
        return f.apply(self.fromNull());
      }

      @Override
      public B fromBool(boolean value) {
        return f.apply(self.fromBool(value));
      }

      @Override
      public B fromString(String value) {
        return f.apply(self.fromString(value));
      }

      @Override
      public B fromNumber(BigDecimal value) {
        return f.apply(self.fromNumber(value));
      }

      @Override
      public <X> X fromArray(ArrayFold<B, X> fold) {
        return self.fromArray(new ArrayFold<A, X>() {
          @Override
          public <S, T> X run(S init, Function<S, Step<FromJson<T>, A>> stepper, BiFunction<T, S, S> merge) {
            return fold.run(init, stepper.andThen(Steps.modValue(f)), merge);
          }
        });
      }

      @Override
      public <X> X fromObject(ObjectFold<B, X> fold) {
        return self.fromObject(new ObjectFold<A, X>() {
          @Override
          public <S, T> X run(S init, Function<S, Step<Function<String, FromJson<T>>, A>> stepper, BiFunction<T, S, S> merge) {
            return fold.run(init, stepper.andThen(Steps.modValue(f)), merge);
          }
        });
      }
    };
  }


  FromJson<Json> toJson = new FromJson<Json>() {
    @Override
    public Json fromNull() {
      return JsonNull();
    }

    @Override
    public Json fromBool(boolean value) {
      return JsonBool(value);
    }

    @Override
    public Json fromString(String value) {
      return JsonString(value);
    }

    @Override
    public Json fromNumber(BigDecimal value) {
      return JsonNumber(value);
    }

    @Override
    public <X> X fromArray(ArrayFold<Json, X> fold) {
      return fold.<List<Json>, Json>run(Nil(), acc -> yield(toJson, JsonArray(acc)), Lists::Cons);
    }

    @Override
    public <X> X fromObject(ObjectFold<Json, X> fold) {
      return fold.<List<JAssoc<Json>>, JAssoc<Json>>run(Nil(),
          acc -> yield(key -> toJson.map(json -> JAssoc(key, json)), JsonObject(acc)), Lists::Cons);
    }

  };
}
