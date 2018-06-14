package org.sonarsource.auth.openshift;

import com.google.gson.Gson;

/**
 * POST SAR response
 */
 
public class OpenShiftSubjectAccessReviewResponse {
	
	private String namespace;
	private boolean allowed;
	private String reason;

    public OpenShiftSubjectAccessReviewResponse() {
    }
    
    public OpenShiftSubjectAccessReviewResponse(String namespace, boolean allowed, String reason) {
    	this.namespace = namespace;
    	this.allowed = allowed;
    	this.reason = reason;
    }
    
    public String getNamespace() {
    	return namespace;
    }

    public boolean getAllowed() {
    	return allowed;
    }
    
    public String getReason() {
    	return reason;
    }
    
    /**
     * Parse the POST SAR response
     * @param json
     * @return
     */
    public OpenShiftSubjectAccessReviewResponse parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OpenShiftSubjectAccessReviewResponse.class);
      }    
}
