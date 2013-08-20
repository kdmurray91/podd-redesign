/*
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
package com.github.podd.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ansell.restletutils.RestletUtilUser;
import com.github.ansell.restletutils.SesameRealmConstants;
import com.github.podd.restlet.PoddAction;
import com.github.podd.restlet.PoddSesameRealm;
import com.github.podd.restlet.PoddWebServiceApplication;
import com.github.podd.restlet.RestletUtils;
import com.github.podd.utils.PoddRdfConstants;
import com.github.podd.utils.PoddUser;
import com.github.podd.utils.PoddUserStatus;
import com.github.podd.utils.PoddWebConstants;

/**
 * 
 * User Edit resource to modify PODD User details, except password changes.
 * Password changes should be made using the {@link UserPasswordResourceImpl}.
 * 
 * @author kutila
 * 
 */
public class UserEditResourceImpl extends AbstractPoddResourceImpl
{
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * Handle an HTTP GET request to display the Edit User page in HTML
     * 
     * FIXME: incomplete, initial untested code
     */
    @Get
    public Representation getUserEditPageHtml(final Representation entity) throws ResourceException
    {
        this.log.info("editUserHtml");

        final String requestedUserIdentifier =
                (String)this.getRequest().getAttributes().get(PoddWebConstants.KEY_USER_IDENTIFIER);
        this.log.info("requesting edit user: {}", requestedUserIdentifier);
        
        if(requestedUserIdentifier == null)
        {
            // no identifier specified.
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Did not specify user to edit");
        }
        
        final User user = this.getRequest().getClientInfo().getUser();
        this.log.info("authenticated user: {}", user);
        
        // Even though this page only displays user information, since the intention is
        // to modify user information, the Action is considered as a "User Edit".
        PoddAction action = PoddAction.OTHER_USER_EDIT;
        if(user != null && requestedUserIdentifier.equals(user.getIdentifier()))
        {
            action = PoddAction.CURRENT_USER_EDIT;
        }
        this.checkAuthentication(action);        
        
        final Map<String, Object> dataModel = RestletUtils.getBaseDataModel(this.getRequest());
        dataModel.put("contentTemplate", "editUser.html.ftl");
        dataModel.put("pageTitle", "Edit PODD User");
        dataModel.put("title", "Edit User");
        dataModel.put("authenticatedUsername", user.getIdentifier());
        
        final PoddSesameRealm realm = ((PoddWebServiceApplication)this.getApplication()).getRealm();
        final PoddUser poddUser = (PoddUser)realm.findUser(requestedUserIdentifier);
        
        if(poddUser == null)
        {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User not found.");
        }
        else
        {
            dataModel.put("requestedUser", poddUser);
            
            PoddUserStatus[] statuses = PoddUserStatus.values();
            dataModel.put("statusList", statuses);
        }
        
        return RestletUtils.getHtmlRepresentation(PoddWebConstants.PROPERTY_TEMPLATE_BASE, dataModel,
                MediaType.TEXT_HTML, this.getPoddApplication().getTemplateConfiguration());
    }
    
    /**
     * Handle an HTTP POST request submitting RDF data to edit an existing PoddUser.
     */
    @Post("rdf|rj|json|ttl")
    public Representation editUserRdf(final Representation entity, final Variant variant) throws ResourceException
    {
        this.log.info("editUserRdf");
        
        final String requestedUserIdentifier =
                (String)this.getRequest().getAttributes().get(PoddWebConstants.KEY_USER_IDENTIFIER);
        this.log.info("requesting details of user: {}", requestedUserIdentifier);
        
        if(requestedUserIdentifier == null)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Did not specify user to edit");
        }
        
        final User user = this.getRequest().getClientInfo().getUser();
        this.log.info("authenticated user: {}", user);

        // check authentication first
        PoddAction action = PoddAction.OTHER_USER_EDIT;
        if(user != null && requestedUserIdentifier.equals(user.getIdentifier()))
        {
            action = PoddAction.CURRENT_USER_EDIT;
        }
        this.checkAuthentication(action);        
        
        final PoddSesameRealm nextRealm = ((PoddWebServiceApplication)this.getApplication()).getRealm();
        
        final RestletUtilUser currentUser = nextRealm.findUser(requestedUserIdentifier);
        if (currentUser == null || !(currentUser instanceof PoddUser))
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "User not found");
        }
        final PoddUser poddUser = (PoddUser) currentUser;
        URI userUri = null;
        try
        {
            // - get input stream with RDF content
            final InputStream inputStream = entity.getStream();
            final RDFFormat inputFormat =
                    Rio.getParserFormatForMIMEType(entity.getMediaType().getName(), RDFFormat.RDFXML);
            final Model modifiedUserModel = Rio.parse(inputStream, "", inputFormat);
            
            
            // - create PoddUser with edited details
            this.modelToUser(modifiedUserModel, poddUser);
            
            // modify User record in the Realm
            userUri = nextRealm.updateUser(poddUser);
            
            this.log.debug("Updated User <{}>", poddUser);
            
            // - check the User was successfully added to the Realm
            final RestletUtilUser findUser = nextRealm.findUser(poddUser.getIdentifier());
            if(findUser == null)
            {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to add user");
            }
            
        }
        catch(final IOException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "There was a problem with the input", e);
        }
        catch(final OpenRDFException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "There was a problem with the input", e);
        }
        
        // - prepare response
        final ByteArrayOutputStream output = new ByteArrayOutputStream(8096);
        final RDFFormat outputFormat =
                Rio.getWriterFormatForMIMEType(variant.getMediaType().getName(), RDFFormat.RDFXML);
        try
        {
            final Model model = new LinkedHashModel();
            model.add(userUri, SesameRealmConstants.OAS_USERIDENTIFIER,
                    PoddRdfConstants.VF.createLiteral(poddUser.getIdentifier()));
            Rio.write(model, output, outputFormat);
        }
        catch(final OpenRDFException e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Could not create response");
        }
        
        return new ByteArrayRepresentation(output.toByteArray(), MediaType.valueOf(outputFormat.getDefaultMIMEType()));
    }
    
    /**
     * Helper method to update the {@link PoddUser} with information in the given {@link Model}.
     * 
     * @param model
     * @return
     * @throws ResourceException
     *             if mandatory data is missing.
     */
    private void modelToUser(final Model model, final PoddUser currentUser)
    {
        // User identifier and email are fixed and cannot be changed
        
        // Password change is not allowed from this User Edit Service. Print a warning.
        final String password = model.filter(null, SesameRealmConstants.OAS_USERSECRET, null).objectString();
        if (password != null)
        {
            this.log.warn("Attempting to change password via User Edit Service. Disallowed.");
        }
        
        final String firstName = model.filter(null, SesameRealmConstants.OAS_USERFIRSTNAME, null).objectString();
        if(firstName != null)
        {
            currentUser.setFirstName(firstName);
        }
        
        final String lastName = model.filter(null, SesameRealmConstants.OAS_USERLASTNAME, null).objectString();
        if(lastName != null)
        {
            currentUser.setLastName(lastName);
        }
        
        final URI homePage = model.filter(null, PoddRdfConstants.PODD_USER_HOMEPAGE, null).objectURI();
        if (homePage != null)
        {
            currentUser.setHomePage(homePage);
        }
        
        final String organization = model.filter(null, PoddRdfConstants.PODD_USER_ORGANIZATION, null).objectString();
        if (organization != null)
        {
            currentUser.setOrganization(organization);
        }
        
        final String orcidID = model.filter(null, PoddRdfConstants.PODD_USER_ORCID, null).objectString();
        if (orcidID != null)
        {
            currentUser.setOrcid(orcidID);
        }
        
        final String title = model.filter(null, PoddRdfConstants.PODD_USER_TITLE, null).objectString();
        if (title != null)
        {
            currentUser.setTitle(title);
        }
        
        final String phone = model.filter(null, PoddRdfConstants.PODD_USER_PHONE, null).objectString();
        if (phone != null)
        {
            currentUser.setPhone(phone);
        }
        
        final String address = model.filter(null, PoddRdfConstants.PODD_USER_ADDRESS, null).objectString();
        if (address != null)
        {
            currentUser.setAddress(address);
        }
        
        final String position = model.filter(null, PoddRdfConstants.PODD_USER_POSITION, null).objectString();
        if (position != null)
        {
            currentUser.setPosition(position);
        }
        
        // TODO: no longer seems such a good idea! Simply updating the Status if sent seems better.
        PoddUserStatus status = PoddUserStatus.INACTIVE;
        final URI statusUri = model.filter(null, PoddRdfConstants.PODD_USER_STATUS, null).objectURI();
        if (statusUri != null)
        {
            status = PoddUserStatus.getUserStatusByUri(statusUri);
        }
        else
        {
            this.log.warn("User Status was not sent. Setting the User status to INACTIVE");
        }
        currentUser.setUserStatus(status);
    }
}