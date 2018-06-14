package org.sonarsource.auth.openshift;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * Parse the JSON response of GET https://$ApiURL/oapi/v1/users/~
 */
public class GsonUser {
  private String name;	
  private static Gson gson = new Gson(); 
  private static String METADATA = "metadata";  
  private static JsonParser parser = new JsonParser();
  
  public GsonUser() {	
  }

  public GsonUser(String name) {
    this.name = name;
  }  
  
  /**
   * Name of the OpenShift user, 
   * used as login to the SonarQube platform
   */
  public String getName() {
    return name;
  }
  
  /*
   * Retrieves data from metadata object
   */
  private static GsonUser parseMetaData(JsonObject jsonObject) {	  	 
	return gson.fromJson(jsonObject.get(METADATA), GsonUser.class);	
  }
 
  /**
   * Parse Json object
   */
  public static GsonUser parseObject(String json) {	  
	  JsonObject jsonObject = parser.parse(json).getAsJsonObject(); 	
	  jsonObject.isJsonObject();
	 return parseMetaData(jsonObject);
  }
}


