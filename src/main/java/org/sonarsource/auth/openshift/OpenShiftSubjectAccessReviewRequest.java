package org.sonarsource.auth.openshift;

import java.util.ArrayList;
import java.util.List;
import com.google.api.client.util.Key;

/**
 * SAR template for POST request
 */
public class OpenShiftSubjectAccessReviewRequest {

    public static final String SUBJECT_ACCESS_REVIEW = "SubjectAccessReview";
    public static final String V1 = "v1";
    public static final String DEFAULT_RESOURCE_API_GROUP = "build.openshift.io";
    public static final String DEFAULT_RESOURCE = "jenkins";

    public OpenShiftSubjectAccessReviewRequest() {
    	
         kind = SUBJECT_ACCESS_REVIEW;
         apiVersion = V1;
         namespace = null;
         verb = null;
         resourceAPIGroup = DEFAULT_RESOURCE_API_GROUP;
         resourceAPIVersion = "";
         resource = DEFAULT_RESOURCE;
         resourceName = "";
         content = null;
         user = "";
         groups = new ArrayList<String>();
         scopes = new ArrayList<String>();       
    }
    
    @Key
    public String kind;

    @Key
    public String apiVersion;

    @Key
    public String namespace;

    @Key
    public String verb;

    @Key
    public String resourceAPIGroup;

    @Key
    public String resourceAPIVersion;

    @Key
    public String resource;

    @Key
    public String resourceName;

    @Key
    public String content;

    @Key
    public String user;

    @Key
    public List<String> groups;

    @Key
    public List<String> scopes;
}














