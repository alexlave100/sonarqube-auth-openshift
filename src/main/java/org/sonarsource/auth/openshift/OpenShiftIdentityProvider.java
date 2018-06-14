package org.sonarsource.auth.openshift;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ExecutionException;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

@ServerSide
public class OpenShiftIdentityProvider implements OAuth2IdentityProvider {
	
	public static final String KEY = "openshift";
	private static final String NAME = "OpenShift";
	private static final String IMAGE_PATH = "/static/authopenshift/openshift.svg";		
	private static final String ADMIN = "admin";
	private static final String EDIT = "edit";
	private static final String VIEW = "view";
	private static final String[] ROLES = new String[] { ADMIN, EDIT, VIEW };
	private static final String SONAR_ADMIN = "sonar-administrators";
	private static final String SONAR_USER = "sonar-users";
	private static final String UTF8_ENCODING = "UTF-8";
	
	private final OpenShiftApi scribeApi;
	private final OpenShiftSettings settings;
	private final OpenShiftUserIdentityFactory identityFactory;
		
	static final Logger LOGGER = Logger.getLogger(OpenShiftIdentityProvider.class.getName());
	
	public OpenShiftIdentityProvider(OpenShiftSettings settings, OpenShiftUserIdentityFactory identityFactory, OpenShiftApi scribeApi) {
		this.settings = settings;
		this.scribeApi = scribeApi;	
		this.identityFactory = identityFactory;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName() {		
		return NAME;
	}

	@Override
	public Display getDisplay() {			
		 return Display.builder()
			      .setIconPath(IMAGE_PATH)
			      .setBackgroundColor(settings.getButtonColor())
			      .build();		  		
	}

	@Override
	public boolean isEnabled() {		
		return settings.isEnabled(); 
	}

	@Override
	public boolean allowsUsersToSignUp(){
		return settings.getAllowUsersToSignUp();
	}

	private void getServiceAccountName() throws IOException { 
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(settings.getApiURL() + settings.getUserURI());		
		
		request.addHeader("Authorization", "Bearer " + settings.getClientSecret());
		request.addHeader("Accept", "application/json");		
		
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();		
		
		String serviceAccountAsJson = EntityUtils.toString(entity, UTF8_ENCODING);
		GsonUser gson =  GsonUser.parseObject(serviceAccountAsJson);		
		String[] userNameParts = gson.getName().split(":");       
		LOGGER.info(String.format("\n\n %s \n\n", serviceAccountAsJson));

		if (userNameParts != null && userNameParts.length == 4) 
            settings.setServicAccountName(userNameParts[3]);
	}
	
	@Override
	public void init(InitContext context) {			
		try {
			getServiceAccountName();				
		} catch (IOException e1) {
			LOGGER.info(String.format("Service account name couldn't be resolved.%n"));
			LOGGER.log(Level.INFO, String.format("Service account name couldn't be resolved.%n"), e1);

		}		
		
		try {
			 String state = context.generateCsrfState();			 
			    OAuth20Service scribe = newScribeBuilder(context)
			    	  .scope(getScope())
			      .state(state)
			      .build(scribeApi);
			    String url = scribe.getAuthorizationUrl();		
			    context.redirectTo(url);				    
		} catch (IOException e) {
			LOGGER.info(String.format(
					"Unable to read/write client id and/or client secret from service account.%n"));
			LOGGER.log(Level.INFO, String.format("Unable to read/write client id and/or client secret from service account.%n"), e);

		}								
	}
		
	private String getScope() {
		return settings.getUserScope() + 
				" " + settings.getAccessScope();
    }
	
	
	@Override
	  public void callback(CallbackContext context) {
	    try {	    
	      onCallback(context);	    
	    } catch (IOException | InterruptedException | ExecutionException e) {
	      throw new IllegalStateException(e);
	    }	  	    
	  }

	  private void onCallback(CallbackContext context) throws InterruptedException, ExecutionException, IOException {							
			context.verifyCsrfState();
		    HttpServletRequest request = context.getRequest();
		   
		    HashSet<String> sonarQubeGroups = new HashSet<String>();	
		    OAuth20Service scribe = newScribeBuilder(context).build(scribeApi);		   		    
		    String code = request.getParameter("code");		    
		    OAuth2AccessToken accessToken = scribe.getAccessToken(code);		    	
		    
		    Set<String> responseRole = executePOSTSARRequest(settings.getApiURL() + settings.getSarURI(), scribe, accessToken);
			
		    LOGGER.info(String.format("Allowed OpenShift Roles are %s.%n%n", responseRole.toString()));
						
			if(responseRole.contains(ADMIN)) 
				sonarQubeGroups.add(SONAR_ADMIN);
			
			if(responseRole.contains(EDIT) || responseRole.contains(VIEW)) 
				sonarQubeGroups.add(SONAR_USER);
			
			LOGGER.info(String.format("Allowed SonarQube Groups are %s.%n%n", sonarQubeGroups));

			settings.setOpenShiftGroups(sonarQubeGroups);			  
		    
			GsonUser user = getUser(scribe, accessToken);	   			
		    UserIdentity userIdentity = identityFactory.create(user);
	    	    
		    context.authenticate(userIdentity);	    	    
	    	    context.redirectToRequestedPage();
	  }
	  
	  private GsonUser getUser(OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException, ExecutionException, InterruptedException {		  
		  String responseBody = executeGETUserRequest(settings.getApiURL() + settings.getUserURI(), scribe, accessToken);		  
	    return GsonUser.parseObject(responseBody);
	  }
	  
	  public Set<String> executePOSTSARRequest(String requestUrl, OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException, InterruptedException, ExecutionException {
		  Response response=null;
		 
		  OpenShiftSubjectAccessReviewResponse accessReviewResponse = new OpenShiftSubjectAccessReviewResponse();		  
		  HashSet<String> allowedVerbs = new HashSet<String>();		  
		  String namespace = settings.getNamespace();
		  
		  
		  OAuthRequest request = new OAuthRequest(Verb.POST , requestUrl);		  
		  request.addHeader("Content-Type", "application/json");

		  for (String verb : ROLES) {			  			  
			  String json = buildSARJson(namespace, verb);		  	
			  
			  request.setPayload(json);	   		    		  	  
			  scribe.signRequest(accessToken, request);	    		      
			  response = scribe.execute(request);   		    
		      accessReviewResponse = accessReviewResponse.parse(response.getBody());
		    		  
		      LOGGER.info(String.format("SubjectAccessReviewResponse for %s: { Allowed: %s, Reason: %s }.", verb, accessReviewResponse.getAllowed(), accessReviewResponse.getReason()));
		      
		    
		    if(accessReviewResponse.getAllowed()) 
		    		allowedVerbs.add(verb);		    
		  }
		  return allowedVerbs;  
	  }	  
	  
	  private static String executeGETUserRequest(String requestUrl, OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException, ExecutionException, InterruptedException {	  
		OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);		
	    scribe.signRequest(accessToken, request);	    
	    Response response = scribe.execute(request);
	  	    
	    if (!response.isSuccessful()) 
	      throw unexpectedResponseCode(requestUrl, response);
	    	    
	    return response.getBody();
	  }
	  
	  private String buildSARJson(String namespace, String verb) throws IOException {
		  	Gson gson = new Gson();	        
		  	OpenShiftSubjectAccessReviewRequest request = new OpenShiftSubjectAccessReviewRequest();	        
		  	request.namespace = namespace;	        
		  	request.verb = verb;	        
	        
	        return gson.toJson(request);
	    }
	  	  
	  private static IllegalStateException unexpectedResponseCode(String requestUrl, Response response) throws IOException {
	    return new IllegalStateException(format("Fail to execute request '%s'. HTTP code: %s, response: %s.", requestUrl, response.getCode(), response.getBody()));
	  }
	  
	  private ServiceBuilder newScribeBuilder(OAuth2IdentityProvider.OAuth2Context context) throws IOException { //FileNotFoundException, 
		  if (!isEnabled()) {
		      throw new IllegalStateException("OpenShift authentication is disabled.");
		    }
		    return new ServiceBuilder(settings.getClientId())
		    		.apiSecret(settings.getClientSecret())
		    		.callback(context.getCallbackUrl());	
	  }
}
