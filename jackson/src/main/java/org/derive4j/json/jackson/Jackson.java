package org.derive4j.json.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.derive4j.json.FromJson;
import org.derive4j.json.IO;
import org.derive4j.json.JAssocs;
import org.derive4j.json.Json;
import org.derive4j.json.Json.JAssoc;
import org.derive4j.json.List;
import org.derive4j.json.Step;
import org.derive4j.json.Steps;
import org.derive4j.json.ToJson;
import org.derive4j.json.Unit;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static org.derive4j.json.IO.effect;
import static org.derive4j.json.JAssocs.JAssoc;
import static org.derive4j.json.Lists.Cons;
import static org.derive4j.json.Lists.Nil;

public class Jackson {
  public static void main(String[] args) throws IOException {
    JsonFactory factory = new JsonFactory();
    // configure, if necessary:
    factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
    StringWriter sw = new StringWriter();
    JsonGenerator generator = factory.createGenerator(sw);
    Json.Factory<IO<Unit>> writer = jsonFactory(generator);

    IO<Unit> json = writer.JsonObject(Cons(JAssoc("nullField", writer.JsonNull()),
        Cons(JAssoc("booleanField", writer.JsonBool(true)), Cons(JAssoc("number", writer.JsonNumber(new BigDecimal("3.1416"))),
            Cons(JAssoc("array", writer.JsonArray(Cons(writer.JsonNumber(new BigDecimal("3.141654")),
                Cons(writer.JsonNull(), Cons(writer.JsonBool(true), Nil()))))), Nil())))));
    IO<Unit> json2 = writer.JsonObject(Cons(JAssoc("nullField", writer.JsonNull()),

        Cons(JAssoc("booleanField", writer.JsonBool(true)), Cons(JAssoc("number", writer.JsonNumber(new BigDecimal("3.1416"))),
            Cons(JAssoc("array", writer.JsonArray(
                Cons(writer.JsonNumber(new BigDecimal("3.141654")), Cons(json, Cons(writer.JsonBool(true), Nil()))))),
                Cons(JAssoc("object", json), Nil()))))));

    json2.run();
    generator.flush();
    sw.flush();
    System.out.println(sw.toString());
    JsonParser jsonParser = factory.createParser(sw.toString());
    jsonParser.nextToken();
    Json reParsedJson = parser(jsonParser, FromJson.toJson).run();
    ToJson.fromJson.write(writer, reParsedJson).run();
    System.out.println();
    System.out.println(sw.toString());
    //factory.par
  }


  static <A> IO<A> parser(JsonParser jacksonParser, FromJson<A> fromJson) {
      switch (jacksonParser.currentToken()) {
        case VALUE_NULL:
          return  fromJson::fromNull;
        case VALUE_FALSE:
          return () -> fromJson.fromBool(false);
        case VALUE_TRUE:
          return () -> fromJson.fromBool(true);
        case VALUE_STRING:
          return () -> fromJson.fromString(jacksonParser.getText());
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
          return () -> fromJson.fromNumber(jacksonParser.getDecimalValue());
        case START_ARRAY:
          return parseArray(jacksonParser, fromJson);
        case START_OBJECT:
          return parseObject(jacksonParser, fromJson);
        default:
          throw new UnsupportedOperationException(jacksonParser.getCurrentToken().name());
      }
  }


  private static <A> IO<A> parseArray(JsonParser jacksonParser, FromJson<A> fromJson) {
    return fromJson.fromArray(new FromJson.ArrayFold<A, IO<A>>() {
      @Override
      public <S, T> IO<A> run(S init, Function<S, Step<FromJson<T>, A>> stepper, BiFunction<T, S, S> merge) {
        return Steps.caseOf(stepper.apply(init))
            .<IO<A>>done(a -> () -> {
              if (jacksonParser.skipChildren().currentToken() != END_ARRAY) {
                while (jacksonParser.nextToken() != END_ARRAY){}
              }
              jacksonParser.nextToken();
              return a;
            })
            .yield((fromJson, a) -> () -> {
              switch (jacksonParser.nextToken()) {
                case END_ARRAY:
                  return a;
                default:
                  //FIXME not stack-safe:
                  return run(merge.apply(parser(jacksonParser, fromJson).run(), init), stepper, merge).run();
              }
            } );
      }
    });
  }


  private static <A> IO<A> parseObject(JsonParser jacksonParser, FromJson<A> fromJson) {

    return fromJson.fromObject(new FromJson.ObjectFold<A, IO<A>>() {
      @Override
      public <S, T> IO<A> run(S init, Function<S, Step<Function<String, FromJson<T>>, A>> stepper, BiFunction<T, S, S> merge) {
        return Steps.caseOf(stepper.apply(init))
            .<IO<A>>done(a -> () -> {
              if (jacksonParser.skipChildren().currentToken() != END_OBJECT) {
                while (jacksonParser.nextToken() != END_OBJECT){}
              }
              jacksonParser.nextToken();
              return a;
            })
            .yield((fromJson, a) -> () -> {
              switch (jacksonParser.nextToken()) {
                case FIELD_NAME:
                  FromJson<T> fromJsonT = fromJson.apply(jacksonParser.getText());
                  jacksonParser.nextToken().name();
                  return run(merge.apply(parser(jacksonParser, fromJsonT).run(), init), stepper, merge).run();
                case END_OBJECT:
                  return a;
                default:
                  throw new UnsupportedOperationException(jacksonParser.getCurrentToken().name());
              }
            } );
      }
    });
  }

  static Json.Factory<IO<Unit>> jsonFactory(JsonGenerator generator) {

    return new Json.Factory<IO<Unit>>() {

      final Function<JAssoc<IO<Unit>>, IO<Unit>> writeJAssoc = JAssocs.<IO<Unit>>cases().JAssoc((key, ioValue) -> effect(() -> {
        generator.writeFieldName(key);
        ioValue.run();
      }));

      @Override
      public IO<Unit> JsonNull() {
        return effect(generator::writeNull);
      }

      @Override
      public IO<Unit> JsonBool(boolean bool) {
        return effect(() -> generator.writeBoolean(bool));
      }

      @Override
      public IO<Unit> JsonNumber(BigDecimal number) {
        return effect(() -> generator.writeNumber(number));
      }

      @Override
      public IO<Unit> JsonString(String string) {
        return effect(() -> generator.writeString(string));
      }

      @Override
      public IO<Unit> JsonArray(List<IO<Unit>> array) {
        return effect(() -> {
          generator.writeStartArray();
          IO.sequence_(array).run();
          generator.writeEndArray();
        });
      }

      @Override
      public IO<Unit> JsonObject(List<JAssoc<IO<Unit>>> object) {
        return effect(() -> {
          generator.writeStartObject();
          IO.sequence_(object, writeJAssoc).run();
          generator.writeEndObject();
        });
      }
    };

  }

}

