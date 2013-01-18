/**
 * 
 */
package com.github.podd.utils;

import org.openrdf.model.URI;

/**
 * @author kutila
 *
 */
public interface PoddWebConstants
{
    /**
     * Default value is urn:podd:default:usermanagementgraph:
     */
    public static final URI DEF_USER_MANAGEMENT_GRAPH = PoddRdfConstants.VALUE_FACTORY
            .createURI("urn:podd:default:usermanagementgraph:");

    public static final String DEF_CHALLENGE_AUTH_METHOD = "digest";

    
    public static final String COOKIE_NAME = "PODDAuthentication";

    
    /**
     * Freemarker template used as the base for rendering all HTML pages 
     */
    public static final String PROPERTY_TEMPLATE_BASE = "poddBase.html.ftl";

    /**
     * Path to locate resources
     */
    public static final String PATH_RESOURCES = "/resources/";

    /**
     * Path to login page
     */
    public static final String PATH_LOGIN_FORM = "loginpage";
    
    /**
     * Path to submit login details
     */
    public static final String PATH_LOGIN_SUBMIT = "login";
    
    /**
     * Path to logout from PODD
     */
    public static final String PATH_LOGOUT = "logout";
    
    
    /**
     * Path to redirect user on successful login 
     */
    public static final String PATH_REDIRECT_LOGGED_IN = "index";
    
    /**
     * Path to "about" page
     */
    public static final String PATH_ABOUT = "about";
    
    /**
     * Path to "index" page
     */
    public static final String PATH_INDEX = "index";
    
    /**
     * Path to "user details" page
     */
    public static final String PATH_USER_DETAILS = "userdetails";
    
    /**
     * Path to load a new artifact into PODD
     */
    public static final String PATH_ARTIFACT_UPLOAD = "artifact/new";

    /**
     * Path to get the base (asserted) statements of an artifact 
     */
    public static final String PATH_ARTIFACT_GET_BASE = "artifact/base";
    
    /**
     * Path to get the inferred statements of an artifact 
     */
    public static final String PATH_ARTIFACT_GET_INFERRED = "artifact/inferred";
    
    /**
     * Path to edit an artifact, merging it with existing statements 
     */
    public static final String PATH_ARTIFACT_EDIT_MERGE = "artifact/edit/merge";

    /**
     * Path to edit an artifact, replacing previous statements about the edited objects 
     */
    public static final String PATH_ARTIFACT_EDIT_REPLACE = "artifact/edit/replace";

    /**
     * Path to delete an artifact. This uses HTTP delete method 
     */
    public static final String PATH_ARTIFACT_DELETE = "artifact";
    
    /**
     * Path to the file reference attachment service
     */
    public static final String PATH_ATTACH_FILE_REF = "attachref";
    
    
    
    public static final String PATH_PODD_PLANT = "/ontologies/poddPlant.owl";
    public static final String PATH_PODD_SCIENCE = "/ontologies/poddScience.owl";
    public static final String PATH_PODD_BASE = "/ontologies/poddBase.owl";
    
    public static final String URI_PODD_PLANT = "http://purl.org/podd/ns/poddPlant";
    public static final String URI_PODD_SCIENCE = "http://purl.org/podd/ns/poddScience";
    public static final String URI_PODD_BASE = "http://purl.org/podd/ns/poddBase";


    public static final String PROPERTY_CHALLENGE_AUTH_METHOD = "podd.webservice.auth.challenge.method";



    
    
}