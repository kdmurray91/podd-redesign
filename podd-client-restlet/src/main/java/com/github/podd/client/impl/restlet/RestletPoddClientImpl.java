/**
 * 
 */
package com.github.podd.client.impl.restlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.restlet.data.MediaType;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

import com.github.podd.client.api.PoddClient;
import com.github.podd.client.api.PoddClientException;

/**
 * Restlet based PODD Client implementation.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RestletPoddClientImpl implements PoddClient
{
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String NEW_ARTIFACT = "";
    
    private String serverUrl = null;
    
    public RestletPoddClientImpl()
    {
    }
    
    public RestletPoddClientImpl(final String serverUrl)
    {
        this();
        this.serverUrl = serverUrl;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#appendArtifact(org.semanticweb.owlapi.model.IRI,
     * java.io.InputStream, org.openrdf.rio.RDFFormat)
     */
    @Override
    public OWLOntologyID appendArtifact(final IRI ontologyIRI, final InputStream partialInputStream,
            final RDFFormat format) throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.github.podd.client.api.PoddClient#attachFileReference(org.semanticweb.owlapi.model.IRI,
     * org.semanticweb.owlapi.model.IRI, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public OWLOntologyID attachFileReference(final IRI ontologyIRI, final IRI objectIRI, final String label,
            final String repositoryAlias, final String filePathInRepository) throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#deleteArtifact(org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public boolean deleteArtifact(final IRI ontologyIRI) throws PoddClientException
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    private OWLOntologyID getOntologyFromModel(final Model nextModel)
    {
        if(nextModel.contains(null, RDF.TYPE, OWL.ONTOLOGY))
        {
            for(final Resource nextOntology : nextModel.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects())
            {
                if(nextOntology instanceof URI)
                {
                    final URI nextOntologyURI = (URI)nextOntology;
                    
                    for(final Value nextVersion : nextModel.filter(nextOntologyURI,
                            ValueFactoryImpl.getInstance().createURI(OWL.NAMESPACE, "versionIRI"), null).objects())
                    {
                        if(nextVersion instanceof URI)
                        {
                            return new OWLOntologyID(nextOntologyURI, (URI)nextVersion);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#getPoddServerUrl()
     */
    @Override
    public String getPoddServerUrl()
    {
        return this.serverUrl;
    }
    
    /**
     * Creates the URL for a given path using the current {@link #getPoddServerUrl()} result, or
     * throws an IllegalStateException if the server URL has not been set.
     * 
     * @param path
     *            The path of the web service to get a full URL for.
     * @return The full URL to the given path.
     * @throws IllegalStateException
     *             If {@link #setPoddServerUrl(String)} has not been called with a valid URL before
     *             this point.
     */
    private String getUrl(final String path)
    {
        if(this.serverUrl == null)
        {
            throw new IllegalStateException("PODD Server URL has not been set for this client");
        }
        
        if(path == null)
        {
            throw new NullPointerException("Path cannot be null");
        }
        
        if(!path.startsWith("/"))
        {
            return this.serverUrl + "/podd/" + path;
        }
        else
        {
            return this.serverUrl + "/podd" + path;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#listFileReferenceRepositories()
     */
    @Override
    public List<String> listFileReferenceRepositories() throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#listPublishedArtifacts()
     */
    @Override
    public List<OWLOntologyID> listPublishedArtifacts() throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#listUnpublishedArtifacts()
     */
    @Override
    public List<OWLOntologyID> listUnpublishedArtifacts() throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#login(java.lang.String, char[])
     */
    @Override
    public boolean login(final String username, final char[] password) throws PoddClientException
    {
        final ClientResource resource = new ClientResource(this.getUrl(RestletPoddClientImpl.LOGIN));
        
        return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#logout()
     */
    @Override
    public boolean logout() throws PoddClientException
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    private Model parseRdf(final InputStream stream, final RDFFormat format) throws RDFParseException,
        RDFHandlerException, UnsupportedRDFormatException, IOException
    {
        Rio.createParser(format).parse(stream, this.getUrl(""));
        
        final Model result = new LinkedHashModel();
        
        return result;
    }
    
    private Model parseRdf(final Representation rep) throws PoddClientException, IOException
    {
        final RDFFormat format = Rio.getParserFormatForMIMEType(rep.getMediaType().getName());
        
        if(format == null)
        {
            throw new PoddClientException("Did not understand the format for the RDF response: "
                    + rep.getMediaType().getName());
        }
        
        try
        {
            return this.parseRdf(rep.getStream(), format);
        }
        catch(RDFParseException | RDFHandlerException | UnsupportedRDFormatException e)
        {
            throw new PoddClientException("There was an error parsing the artifact", e);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#publishArtifact(org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public OWLOntologyID publishArtifact(final IRI ontologyIRI) throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#setPoddServerUrl(java.lang.String)
     */
    @Override
    public void setPoddServerUrl(final String serverUrl)
    {
        this.serverUrl = serverUrl;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.github.podd.client.api.PoddClient#unpublishArtifact(org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public OWLOntologyID unpublishArtifact(final IRI ontologyIRI) throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#updateArtifact(org.semanticweb.owlapi.model.IRI,
     * java.io.InputStream, org.openrdf.rio.RDFFormat)
     */
    @Override
    public OWLOntologyID updateArtifact(final IRI ontologyIRI, final InputStream fullInputStream, final RDFFormat format)
        throws PoddClientException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.github.podd.client.api.PoddClient#uploadNewArtifact(java.io.InputStream,
     * org.openrdf.rio.RDFFormat)
     */
    @Override
    public OWLOntologyID uploadNewArtifact(final InputStream input, final RDFFormat format) throws PoddClientException
    {
        final InputRepresentation rep = new InputRepresentation(input, MediaType.valueOf(format.getDefaultMIMEType()));
        
        final ClientResource resource = new ClientResource(this.getUrl(RestletPoddClientImpl.NEW_ARTIFACT));
        
        final Representation post = resource.post(rep);
        
        try
        {
            final Model parsedStatements = this.parseRdf(post);
            
            final OWLOntologyID result = this.getOntologyFromModel(parsedStatements);
            
            if(result != null)
            {
                return result;
            }
            
            throw new PoddClientException("Failed to verify that the artifact was uploaded correctly.");
        }
        catch(final IOException e)
        {
            throw new PoddClientException("Could not parse artifact details due to an IOException", e);
        }
    }
}
