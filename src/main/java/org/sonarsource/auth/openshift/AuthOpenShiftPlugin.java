
package org.sonarsource.auth.openshift;

import org.sonar.api.Plugin;
import java.util.logging.Logger;

public class AuthOpenShiftPlugin implements Plugin {
	
	static final Logger LOGGER = Logger.getLogger(AuthOpenShiftPlugin.class.getName());

  @Override
  public void define(Context context) {
    context.addExtensions(
    	  OpenShiftApi.class, 
      OpenShiftSettings.class,     
      OpenShiftIdentityProvider.class,
      OpenShiftUserIdentityFactory.class
      );
    context.addExtensions(OpenShiftSettings.definitions());
  }
}


