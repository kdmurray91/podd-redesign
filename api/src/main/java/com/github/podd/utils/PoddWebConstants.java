/**
 * PODD is an OWL ontology database used for scientific project management
 * 
 * Copyright (C) 2009-2013 The University Of Queensland
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.podd.utils;

/**
 * @author kutila
 * 
 */
public interface PoddWebConstants
{
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
    public static final String PATH_REDIRECT_LOGGED_IN = "";
    
    /**
     * Path to "about" page
     */
    public static final String PATH_ABOUT = "about";
    
    /**
     * Path to "help" pages
     */
    public static final String PATH_HELP = "help";
    
    /**
     * Path to "index" page
     */
    public static final String PATH_INDEX = "";
    
    /**
     * Path to "user details" page
     */
    public static final String PATH_USER_DETAILS = "user/details";
    
    /**
     * Path to "add user" page
     */
    public static final String PATH_USER_ADD = "admin/user/add";
    
    /**
     * Path to "edit user" page
     */
    public static final String PATH_USER_EDIT = "user/edit";
    
    /**
     * Path to "edit user" page
     */
    public static final String PATH_USER_EDIT_PWD = "user/editpwd";
    
    /**
     * Path to "edit user" page
     */
    public static final String PATH_USER_ROLES = "user/roles";
    
    /**
     * Path to "list users" page
     */
    public static final String PATH_USER_LIST = "admin/user/list";
    
    /**
     * Path to "search users" service
     */
    public static final String PATH_USER_SEARCH = "user/search";
    
    /**
     * Path to list artifacts
     */
    public static final String PATH_ARTIFACT_LIST = "artifacts";
    
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
     * Path to edit an artifact
     */
    public static final String PATH_ARTIFACT_EDIT = "artifact/edit";
    
    /**
     * Path to delete an artifact. This uses HTTP delete method
     */
    public static final String PATH_ARTIFACT_DELETE = "artifact/delete";
    
    /**
     * Path to edit an artifact's Participants.
     */
    public static final String PATH_ARTIFACT_ROLES = "artifact/roles";
    
    /**
     * Path to the file reference attachment service
     */
    public static final String PATH_ATTACH_FILE_REF = "attachref";
    
    /**
     * Path to the list data repositories service.
     */
    public static final String PATH_DATA_REPOSITORY_LIST = "datarepositories/list";
    
    /**
     * Path to the ontology search service
     */
    public static final String PATH_SEARCH = "search";
    
    /**
     * Path to create object service
     */
    public static final String PATH_OBJECT_ADD = "artifact/addobject";
    
    /**
     * Path to delete object service
     */
    public static final String PATH_OBJECT_DELETE = "artifact/deleteobject";
    
    /**
     * Path to get metadata about a particular object type
     */
    public static final String PATH_GET_METADATA = "metadata";
    
    /**
     * Path to get schema ontologies.
     */
    public static final String PATH_GET_SCHEMA = "ns/{schemaPath}";
    
    /**
     * Key used to represent user identifier part of a URL
     */
    public static final String KEY_USER_IDENTIFIER = "useridentifier";
    
    /**
     * Key used to represent an artifact id as part of a request
     */
    public static final String KEY_ARTIFACT_IDENTIFIER = "artifacturi";
    
    /**
     * Key used to represent an artifact's version URI as part of a request
     */
    public static final String KEY_ARTIFACT_VERSION_IDENTIFIER = "versionuri";
    
    /**
     * Key used to indicate that the action should be cascaded onto any child objects
     */
    public static final String KEY_CASCADE = "iscascade";
    
    /**
     * Key used to indicate that the request is to "delete" a resource
     */
    public static final String KEY_DELETE = "delete";
    
    /**
     * Key used to represent an object as part of a request
     */
    public static final String KEY_OBJECT_IDENTIFIER = "objecturi";
    
    /**
     * Key used to represent a parent object as part of a request
     */
    public static final String KEY_PARENT_IDENTIFIER = "parenturi";
    
    /**
     * Key used to represent a parent-child property as part of a request
     */
    public static final String KEY_PARENT_PREDICATE_IDENTIFIER = "parentpredicateuri";
    
    /**
     * Key used to represent the file reference verification policy to use
     */
    public static final String KEY_VERIFICATION_POLICY = "file_verification";
    
    /**
     * Key used to represent a specific help page as part of a request
     */
    public static final String KEY_HELP_PAGE_IDENTIFIER = "helppage";
    
    public static final String PROPERTY_CHALLENGE_AUTH_METHOD = "podd.webservice.auth.challenge.method";
    public static final String PROPERTY_TEST_WEBSERVICE_RESET_KEY = "podd.webservice.reset.key";
    
    public static final String PROPERTY_PURL_PREFIX = "podd.purl.prefix";
    
    public static final String PROPERTY_INITIAL_ADMIN_USERNAME = "podd.initialadmin.username";
    public static final String PROPERTY_INITIAL_ADMIN_PASSWORD = "podd.initialadmin.password";
    
    public static final String DEFAULT_INITIAL_ADMIN_USERNAME = "testAdminUser";
    public static final String DEFAULT_INITIAL_ADMIN_PASSWORD = "testAdminPassword";
<<<<<<< HEAD
=======
    
    public static final String PROPERTY_OWLAPI_REASONER = "podd.owlreasoner";
    public static final String DEFAULT_OWLAPI_REASONER = "HermiT";
    
    public static final String PROPERTY_SESAME_URL = "podd.sesamehttpurl";
    public static final String DEFAULT_SESAME_URL = "";
    
>>>>>>> 01c2f46... add support for persistence
    /**
     * Key used to select published artifacts. Defaults to true.
     */
    public static final String KEY_PUBLISHED = "published";
    
    /**
     * Key used to select unpublished artifacts. Defaults to true for authenticated users.
     */
    public static final String KEY_UNPUBLISHED = "unpublished";
    
    /**
     * Key used in "edit" artifact to indicate whether it should be a "merge" or "replace".
     */
    public static final String KEY_EDIT_WITH_REPLACE = "isreplace";
    
    /**
     * Key used in "edit" artifact to indicate whether any internal object deletions should be
     * carried out without seeking user confirmation.
     */
    public static final String KEY_EDIT_WITH_FORCE = "isforce";
    
    /**
     * Key used in "edit" artifact to indicate whether any updated file references should be
     * verified (for existence of the resource).
     */
    public static final String KEY_EDIT_VERIFY_FILE_REFERENCES = "verifyfilerefs";
    
    /**
     * Key used in "search" service to specify the term being searched for.
     */
    public static final String KEY_SEARCHTERM = "searchterm";
    
    /**
     * Key used in "search" service to specify the RDF types of objects being searched for.
     */
    public static final String KEY_SEARCH_TYPES = "searchtypes";
    
    /**
     * Key used in "create object" service to indicate the type of object to be generated.
     */
    public static final String KEY_OBJECT_TYPE_IDENTIFIER = "objecttypeuri";
    
    /**
     * Key used in "create object" service to indicate the type of object to be generated.
     */
    public static final String KEY_INCLUDE_DO_NOT_DISPLAY_PROPERTIES = "includedndprops";
    
    /**
     * Key used in "metadata" service to indicate policy on types of properties to be included.
     */
    public static final String KEY_METADATA_POLICY = "metadatapolicy";
    
    /**
     * Key used in "get artifact" service to indicate whether inferred axioms should be included in
     * the results.
     */
    public static final String KEY_INCLUDE_INFERRED = "includeInferred";
    
    public static final String METADATA_ALL = "all";
    
    public static final String METADATA_EXCLUDE_CONTAINS = "nocontains";
    
    public static final String METADATA_ONLY_CONTAINS = "containsonly";
    
    public static final String KEY_SCHEMA_URI = "schemauri";
    
    public static final String KEY_SCHEMA_VERSION_URI = "schemaversionuri";
    
    public static final String KEY_SCHEMA_PATH = "schemaPath";
    
    public static final String PURL_ORG_PODD_NS = "http://purl.org/podd/ns/";

}
