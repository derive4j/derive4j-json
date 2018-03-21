package org.derive4j.json;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import org.derive4j.Data;
import org.derive4j.FieldNames;

@Data
public interface Json {

  @Data
  interface JAssoc<J> {
    <X> X match(@FieldNames({"key", "value"}) BiFunction<String, J, X> JAssoc);
  }

  interface Cases<J, X> {
    X JsonNull();
    X JsonBool(boolean bool);
    X JsonNumber(BigDecimal number);
    X JsonString(String string);
    X JsonArray(List<J> array);
    X JsonObject(List<JAssoc<J>> object);
  }

  interface Factory<JSON> extends Cases<JSON, JSON>  {}

  @Data
  interface JsonP<P> {
    <X> X match(Cases<P, X> cases);
  }

  <X> X match(Cases<Json, X> cases);

  default JsonP<Json>  asJsonP() {
    return this::match;
  }

  static Json ofJsonP(JsonP<Json> jsonP) {
    return jsonP::match;
  }


}