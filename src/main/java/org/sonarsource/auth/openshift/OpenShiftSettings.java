package org.sonarsource.auth.openshift;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;
import static org.sonar.api.PropertyType.STRING;

import java.io.BufferedReader;
import java.io.File;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;

@ServerSide
public class OpenShiftSettings {

	private static final String USER_SCOPE = "user:info";
    private static final String ACCESS_SCOPE = "user:check-access";
    
    private static final String USER_URI = "oapi/v1/users/~";
    private static final String SAR_URI = "oapi/v1/subjectaccessreviews";
    static final String DEFAULT_SVR_PREFIX = "https://openshift.default.svc";
	private static final String CA_CRT = "ca.crt";
	private static final String CATEGORY = "OpenShift";
	private static final String SUBCATEGORY = "Authentication";
	
	private static final String WEB_URL = "sonar.auth.openshift.webUrl";
	private static final String API_URL = "sonar.auth.openshift.apiUrl";
	private static final String SERVER_BASE_URL = "sonar.core.serverBaseURL";
	private static final String IS_ENABLED = "sonar.auth.openshift.isEnabled";
	private static final String BUTTON_COLOR = "sonar.auth.openshift.btnColor";	
	private static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.openshift.allowUsersToSignUp";
	private static final String NAMESPACE = "namespace";
	private static final String TOKEN = "token";
	
	private static final String BACKSLASH = "/";
	private static final String COLON = ":";
	
	private static final String SERVICEACCOUNT_NAMESPACE_PREFIX = "system:serviceaccount:";
	private static final String SERVICEACCOUNT_DIRECTORY = "/run/secrets/kubernetes.io/serviceaccount";
	
	private Set<String> openShiftGroups = new HashSet<>();

	private final Settings settings;	
	private String serviceAccountName;
	
	public OpenShiftSettings(Settings settings) {
		 this.settings = settings;
	}	
	
	public String getCert() {
		return CA_CRT;
	}
	
		
	public String getSarURI() {
		return SAR_URI;
	}
	
	public String getUserURI() {
		return USER_URI;
	}
	
	public String getUserScope() {
		return USER_SCOPE;
	}
	
	public String getAccessScope() {
		return ACCESS_SCOPE;
	}
	
	public String getServiceAccountName() {
		return this.serviceAccountName;
	}
	
	public void setServicAccountName(String serviceAccountName) {
		this.serviceAccountName = serviceAccountName;
	}
	
	public void setOpenShiftGroups(Set<String> openShiftGroups) {
		this.openShiftGroups = openShiftGroups;
	}
	
	public Set<String> getOpenShiftGroups() {
		return this.openShiftGroups;		
	}
	public String getNamespace() throws IOException { //FileNotFoundException, 
		return ServiceAccountBufferReader(NAMESPACE);
	}
	
	public boolean isEnabled() {
		return settings.getBoolean(IS_ENABLED);
	}
	
	public String getBaseUrl() {	
		return settings.getString(SERVER_BASE_URL);
	}
		
	public String getServiceAccountNamespacePrefix() {
		return SERVICEACCOUNT_NAMESPACE_PREFIX;
	}
		
	public String getOpenShiftServiceAccountDirectory() {		
		return SERVICEACCOUNT_DIRECTORY;
	}
	
	public boolean getAllowUsersToSignUp() {
		return settings.getBoolean(ALLOW_USERS_TO_SIGN_UP);
	}
	
	public String getWebURL() {
		return urlWithEndingSlash(settings.getString(WEB_URL));
	}
	
	public String getApiURL() {	
		return urlWithEndingSlash(settings.getString(API_URL));
	}
		
	public String getClientId() throws IOException {		//FileNotFoundException, 
		return getServiceAccountNamespacePrefix()+namespacePathWithEndingColon()+getServiceAccountName();
	  }

	public String getClientSecret() throws IOException {	 //FileNotFoundException, 
	     return ServiceAccountBufferReader(TOKEN);
	}

	private String namespacePathWithEndingColon() throws IOException {	// FileNotFoundException, 
			return getNamespace() + COLON;		
	}
	
	private static String urlWithEndingSlash(String url) {
		if (url != null && !url.endsWith(BACKSLASH)) {
			return url + BACKSLASH;
		} return url;
	}
	
	public String getButtonColor() {		
		switch(settings.getString(BUTTON_COLOR)) {		
		case "Black":  	   return "#000000"; 
		case "Green":  	   return "#00A100";
		case "Blue":   	   return "#007FFF";
		case "Purple": 	   return "#B266FF";
		case "Red":    	   return "#DE3933";
		case "Orange": 	   return "#ED7905";
		case "Yellow": 	   return "#D4D400";
		case "Pink":   	   return "#FF66B3";
		case "Light grey": return "#999999";
		default:       	   return "#666666";
		}
	}
	
	public static List<PropertyDefinition> definitions() {
	    int index = 1;
	    return Arrays.asList(
	      PropertyDefinition.builder(IS_ENABLED)
	        .name("Login enabled")
	        .description("Enable OpenShift users to login. Value is ignored and treated as default if "
	        		+ "client ID and client secret cannot be defined.")
	        .category(CATEGORY)
	        .subCategory(SUBCATEGORY)
	        .type(BOOLEAN)
	        .defaultValue(valueOf(false))
	        .index(index++)
	        .build(),
	        PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
	        .name("Sign up enabled")
	        .description("Enable OpenShift users to sign up to the platform for the first time. "
	        		+ "Else, only pre registered users are allowed")
	        .category(CATEGORY)
	        .subCategory(SUBCATEGORY)
	        .type(BOOLEAN)
	        .defaultValue(valueOf(false))
	        .index(index++)
	        .build(),
	        PropertyDefinition.builder(BUTTON_COLOR)
	        .name("Login button color")
	        .description("Set the color of the login button.")
	        .category(CATEGORY)
	        .subCategory(SUBCATEGORY)
	        .type(SINGLE_SELECT_LIST)
	        .defaultValue("Dark grey")
	        .options("Black", "Blue", "Purple", "Red", "Orange", "Pink", "Green", "Yellow", "Light grey")
	        .index(index++)
	        .build(),       
	      PropertyDefinition.builder(API_URL)
	        .name("The API url for a OpenShift instance.")
	        .description("The API url for a OpenShift instance. You need to supply the correct API url.")
	        .category(CATEGORY)
	        .subCategory(SUBCATEGORY)
	        .type(STRING)
	        .index(index++)
	        .build(),
	      PropertyDefinition.builder(WEB_URL)
	        .name("The WEB url for a OpenShift instance.")
	        .description("The Web url for an OpenShift instance. You need to supply the correct Web url")
	        .category(CATEGORY)
	        .subCategory(SUBCATEGORY)
	        .type(STRING)
	        .index(index++)
	        .build());	      
	  }
	 
	 private String ServiceAccountBufferReader(String directory) throws FileNotFoundException, IOException{				            
		    BufferedReader bufferReader = 
		    			new BufferedReader(
		    				new FileReader(
		    					new File(getOpenShiftServiceAccountDirectory(), directory)));	        
		    String id = bufferReader.readLine();          	    
		    bufferReader.close();		    
			return id;
	 }	
}