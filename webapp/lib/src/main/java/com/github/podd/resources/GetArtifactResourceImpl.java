/**
 * 
 */
package com.github.podd.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.security.User;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.api.PoddArtifactManager;
import com.github.podd.exception.PoddException;
import com.github.podd.exception.UnmanagedArtifactIRIException;
import com.github.podd.exception.UnmanagedSchemaIRIException;
import com.github.podd.restlet.PoddAction;
import com.github.podd.restlet.PoddWebServiceApplication;
import com.github.podd.restlet.RestletUtils;
import com.github.podd.utils.FreemarkerUtil;
import com.github.podd.utils.InferredOWLOntologyID;
import com.github.podd.utils.PoddObject;
import com.github.podd.utils.PoddRdfConstants;
import com.github.podd.utils.PoddWebConstants;
import com.github.podd.utils.SparqlQueryHelper;

/**
 * 
 * Get an artifact from PODD. This resource handles requests for asserted statements as well as
 * inferred statements.
 * 
 * @author kutila
 * 
 */
public class GetArtifactResourceImpl extends AbstractPoddResourceImpl
{
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Get("html")
    public Representation getArtifactHtml(final Representation entity) throws ResourceException
    {
        this.log.info("getArtifactHtml");
        
        final String artifactUri = this.getQuery().getFirstValue(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER);
        
        if(artifactUri == null)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Artifact ID not submitted");
        }
        
        final String objectToView = this.getQuery().getFirstValue(PoddWebConstants.KEY_OBJECT_IDENTIFIER);
        
        // URI objectToView = topObject URI by default
        // optional parameter for inner objects
        
        this.log.info("requesting get artifact (HTML): {}", artifactUri);
        
        this.checkAuthentication(PoddAction.UNPUBLISHED_ARTIFACT_READ,
                Collections.<URI> singleton(PoddRdfConstants.VALUE_FACTORY.createURI(artifactUri)));
        // completed checking authorization
        
        final User user = this.getRequest().getClientInfo().getUser();
        this.log.info("authenticated user: {}", user);
        
        InferredOWLOntologyID ontologyID;
        try
        {
            final PoddArtifactManager artifactManager =
                    ((PoddWebServiceApplication)this.getApplication()).getPoddArtifactManager();
            ontologyID = artifactManager.getArtifactByIRI(IRI.create(artifactUri));
        }
        catch(final UnmanagedArtifactIRIException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not find the given artifact", e);
        }
        
        final List<URI> schemaOntologyGraphs = new ArrayList<URI>(SparqlQueryHelper.getSchemaOntologyGraphs());
        
        final Map<String, Object> dataModel = RestletUtils.getBaseDataModel(this.getRequest());
        dataModel.put("contentTemplate", "objectDetails.html.ftl");
        dataModel.put("pageTitle", "View Artifact");
        
        try
        {
            this.populateDataModelWithArtifactData(ontologyID, objectToView, schemaOntologyGraphs, dataModel);
        }
        catch(final OpenRDFException e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to populate data model");
        }
        
        return RestletUtils.getHtmlRepresentation(PoddWebConstants.PROPERTY_TEMPLATE_BASE, dataModel,
                MediaType.TEXT_HTML, this.getPoddApplication().getTemplateConfiguration());
    }
    
    @Get("rdf|rj|ttl")
    public Representation getArtifactRdf(final Representation entity, final Variant variant) throws ResourceException
    {
        this.log.info("getArtifactRdf");
        
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        
        try
        {
            final String artifactUri = this.getQuery().getFirstValue(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER);
            
            if(artifactUri == null)
            {
                this.log.error("Artifact ID not submitted");
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Artifact ID not submitted");
            }
            
            this.log.info("requesting get artifact ({}): {}", variant.getMediaType().getName(), artifactUri);
            
            this.checkAuthentication(PoddAction.UNPUBLISHED_ARTIFACT_READ,
                    Collections.<URI> singleton(PoddRdfConstants.VALUE_FACTORY.createURI(artifactUri)));
            // completed checking authorization
            
            final User user = this.getRequest().getClientInfo().getUser();
            this.log.info("authenticated user: {}", user);
            
            final InferredOWLOntologyID ontologyID =
                    this.getPoddApplication().getPoddArtifactManager().getArtifactByIRI(IRI.create(artifactUri));
            
            // FIXME: support prototype method for this
            // use this instead of ../base/ ../inferred/.. in the Prototype. Change documentation
            // too.
            final String includeInferredString =
                    this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("includeInferred", true);
            final boolean includeInferred = Boolean.valueOf(includeInferredString);
            
            this.getPoddApplication()
                    .getPoddArtifactManager()
                    .exportArtifact(ontologyID, stream,
                            RDFFormat.forMIMEType(variant.getMediaType().getName(), RDFFormat.TURTLE), includeInferred);
        }
        catch(final UnmanagedArtifactIRIException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not find the given artifact", e);
        }
        catch(OpenRDFException | PoddException | IOException e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to export artifact", e);
        }
        
        return new ByteArrayRepresentation(stream.toByteArray());
    }
    
    /**
     * This method retrieves necessary info about the object being viewed via SPARQL queries and
     * populates the data model.
     * 
     * @param ontologyID
     *            The artifact to be viewed
     * @param objectToView
     *            An optional internal object to view
     * @param ontologyGraphs
     *            The schema ontology graphs that should be part of the context for SPARQL
     * @param dataModel
     *            Freemarker data model to be populated
     * @throws OpenRDFException
     */
    private void populateDataModelWithArtifactData(final InferredOWLOntologyID ontologyID, final String objectToView,
            final List<URI> ontologyGraphs, final Map<String, Object> dataModel) throws OpenRDFException
    {
        
        final RepositoryConnection conn =
                this.getPoddApplication().getPoddRepositoryManager().getRepository().getConnection();
        conn.begin();
        try
        {
            final SparqlQueryHelper sparql = new SparqlQueryHelper();
            
            // get top-object of this artifact
            final List<PoddObject> topObjectList =
                    sparql.getTopObjects(conn, ontologyID.getVersionIRI().toOpenRDFURI(), ontologyID
                            .getInferredOntologyIRI().toOpenRDFURI());
            if(topObjectList == null || topObjectList.size() != 1)
            {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "There should be only 1 top object");
            }
            
            
            // the object to display (default is Top Object)
            URI objectUri = topObjectList.get(0).getUri();
            
            if(objectToView != null)
            {
                objectUri = ValueFactoryImpl.getInstance().createURI(objectToView);
            }
            
            
            // hack together the list of contexts to query in
            ontologyGraphs.add(ontologyID.getVersionIRI().toOpenRDFURI());
            // ontologyGraphs.add(ontologyID.getInferredOntologyIRI().toOpenRDFURI());
            
            
            // first get the title & description encapsulated in a PoddObject
            final PoddObject theObject = sparql.getPoddObject(objectUri, conn, ontologyGraphs.toArray(new URI[0]));
            dataModel.put("poddObject", theObject);
            
            
            // find the object's type
            final PoddObject theObjectType = sparql.getObjectType(objectUri, conn, ontologyGraphs.toArray(new URI[0]));
            if(theObjectType == null)
            {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Could not determine type of object");
            }
            if (theObjectType.getTitle() != null)
            {
                dataModel.put("objectType", theObjectType.getTitle());
            }
            else
            {
                dataModel.put("objectType", theObjectType.getUri());
            }

            
            // populate the properties of the object
            final List<URI> orderedProperties =
                    sparql.getDirectProperties(objectUri, conn, ontologyGraphs.toArray(new URI[0]));
            
            final Model allNeededStatementsForDisplay =
                    sparql.getPoddObjectDetails(objectUri, conn, ontologyGraphs.toArray(new URI[0]));
            
            dataModel.put("artifactUri", ontologyID.getOntologyIRI().toOpenRDFURI());
            dataModel.put("propertyList", orderedProperties);
            dataModel.put("completeModel", allNeededStatementsForDisplay);
        }
        finally
        {
            if(conn != null)
            {
                conn.rollback(); // read only, nothing to commit
                conn.close();
            }
        }
        // add other required info to data model
        dataModel.put("rdfsLabelUri", RDFS.LABEL);
        dataModel.put("rdfsRangeUri", RDFS.RANGE);
        dataModel.put("util", new FreemarkerUtil());
        
        // FIXME: determine based on project status and user authorization
        dataModel.put("canEditObject", true);
        
        // -TODO: populate refers to list
        final List<Object> refersToList = new ArrayList<Object>();
        
        final Map<String, Object> refersToElement = new HashMap<String, Object>();
        refersToElement.put("label", "Refers To Label");
        // DESIGN FIXME: Figure out a way of doing this without removing characters. It is not an
        // option to remove characters or split URIs.
        refersToElement.put("propertyUriWithoutNamespace", "artifact89");
        refersToElement.put("availableObjects", this.getDummyReferredObjects());
        refersToElement.put("areSelectedObjects", true);
        
        refersToList.add(refersToElement);
        
        dataModel.put("refersToList", refersToList);
        
        dataModel.put("selectedObjectCount", 0);
        dataModel.put("childHierarchyList", Collections.emptyList());
    }
    
    private List<Object> getDummyReferredObjects()
    {
        final List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < 2; i++)
        {
            final Map<String, Object> anObject = new HashMap<String, Object>();
            anObject.put("isSelected", true);
            anObject.put("state", "A");
            anObject.put("type", "IntrnalObject");
            anObject.put("uri", "object:34343");
            anObject.put("title", "Object " + i);
            anObject.put("description", "This is a simple object within an artifact");
            
            list.add(anObject);
        }
        
        return list;
    }
    
    // FIXME: cannot work until Schema Manager is implemented
    protected List<URI> getSchemaOntologyGraphs() throws UnmanagedSchemaIRIException
    {
        String[] schemaOntologies =
                { PoddRdfConstants.PODD_DCTERMS, PoddRdfConstants.PODD_FOAF, PoddRdfConstants.PODD_USER,
                        PoddRdfConstants.PODD_BASE, PoddRdfConstants.PODD_SCIENCE, PoddRdfConstants.PODD_PLANT };
        
        final List<URI> schemaOntologyGraphs = new ArrayList<URI>();
        
        for(String schema : schemaOntologies)
        {
            InferredOWLOntologyID ontologyID =
                    this.getPoddApplication().getPoddSchemaManager()
                            .getCurrentSchemaOntologyVersion(IRI.create(schema));
            schemaOntologyGraphs.add(ontologyID.getVersionIRI().toOpenRDFURI());
            schemaOntologyGraphs.add(ontologyID.getInferredOntologyIRI().toOpenRDFURI());
        }
        return schemaOntologyGraphs;
    }

}
