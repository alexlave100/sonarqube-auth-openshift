package org.sonarsource.auth.openshift;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * Converts OpenShift JSON response to {@link UserIdentity}
 * to be registered into SonarQube
 */
@ServerSide
public class OpenShiftUserIdentityFactory {

  private final OpenShiftSettings settings;
  private static final Logger LOGGER = Loggers.get(OpenShiftUserIdentityFactory.class);
		  
  public OpenShiftUserIdentityFactory(OpenShiftSettings settings) {
    this.settings = settings;
  }

  public UserIdentity create(GsonUser user) {
	  if(settings.getOpenShiftGroups()==null) 
		  LOGGER.info("OpenShift groups cannot be identified.");
		  
	  UserIdentity.Builder builder = UserIdentity.builder()
			  .setGroups(settings.getOpenShiftGroups())
			  .setProviderLogin(user.getName())
			  .setLogin(generateLogin(user))
			  .setName(generateName(user));       
    
	  return builder.build();
  }

  private String generateLogin(GsonUser gsonUser) {
        return generateUniqueLogin(gsonUser);    
    }
    
  private static String generateName(GsonUser gson) {
    String name = gson.getName();
    return name == null || name.isEmpty() ? gson.getName() : name;
  }

  private static String generateUniqueLogin(GsonUser gsonUser) {
    return format("%s@%s", gsonUser.getName(), OpenShiftIdentityProvider.KEY);
  }
}
