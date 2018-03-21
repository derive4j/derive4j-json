package org.derive4j.json;

public interface ToJson<A> {

   <JSON> JSON write(Json.Factory<JSON> jsonFactory, A a);

   ToJson<Json> fromJson = new ToJson<Json>() {
      @Override
      public <JSON> JSON write(Json.Factory<JSON> jsonFactory, Json json) {
         return Jsons.caseOf(json)
             .JsonNull(jsonFactory::JsonNull)
             .JsonBool(jsonFactory::JsonBool)
             .JsonNumber(jsonFactory::JsonNumber)
             .JsonString(jsonFactory::JsonString)
             .JsonArray(jsonList -> jsonFactory.JsonArray(jsonList.map(jsonEl -> write(jsonFactory, jsonEl))))
             .JsonObject(jAssocList -> jsonFactory.JsonObject(jAssocList.map(JAssocs.modValue(jsonEl -> write(jsonFactory, jsonEl)))));
      }
   };

}
