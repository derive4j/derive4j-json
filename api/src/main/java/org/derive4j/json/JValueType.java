package org.derive4j.json;

import org.derive4j.Data;
import org.derive4j.hkt.TypeEq;

@Data
public interface JValueType<J> {

  enum JNull {}
  enum JBool {}
  enum JNumber {}
  enum JString {}
  enum JArray {}
  enum JObject {}

  interface Cases<J, X> {
    X JNull(TypeEq<JNull, J> id);
    X JBool(TypeEq<JBool, J> id);
    X JNumber(TypeEq<JNumber, J> id);
    X JString(TypeEq<JString, J> id);
    X JArray(TypeEq<JArray, J> id);
    X JObject(TypeEq<JObject, J> id);
  }

  <X> X match(Cases<J, X> cases);

}