/**
 * 
 */
package com.github.podd.api.purl.test;

import java.io.InputStream;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.github.podd.api.purl.PoddPurlManager;
import com.github.podd.api.purl.PoddPurlProcessorFactoryRegistry;
import com.github.podd.api.purl.PoddPurlReference;

/**
 * Abstract class to test PoddPurlManager.
 * 
 * @author kutila
 */
public abstract class AbstractPoddPurlManagerTest
{
    
    protected static final String TEMP_URI_PREFIX = "urn:temp";
    
    protected PoddPurlManager testPurlManager;
    
    private PoddPurlProcessorFactoryRegistry testRegistry;
    
    private Repository testRepository;
    
    protected RepositoryConnection testRepositoryConnection;
    
    /**
     * @return A new PoddPurlManager instance for use by this test
     */
    public abstract PoddPurlManager getNewPoddPurlManager();
    
    /**
     * @return A new PurlProcessorFactory Registry for use by this test
     */
    public abstract PoddPurlProcessorFactoryRegistry getNewPoddPurlProcessorFactoryRegistry();
    
    /**
     * Helper method loads RDF statements from a test resource into the test Repository.
     * 
     * @return The context into which the statements are loaded
     * @throws Exception
     */
    protected URI loadTestResources() throws Exception
    {
        final String resourcePath = "/test/artifacts/basicProject-1-internal-object.rdf";
        final URI context = ValueFactoryImpl.getInstance().createURI("urn:testcontext");
        
        final InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        this.testRepositoryConnection.add(inputStream, "", RDFFormat.RDFXML, context);
        
        return context;
    }
    
    @Before
    public void setUp() throws Exception
    {
        this.testRegistry = this.getNewPoddPurlProcessorFactoryRegistry();
        Assert.assertNotNull("Null implementation of test Registry", this.testRegistry);
        
        this.testPurlManager = this.getNewPoddPurlManager();
        Assert.assertNotNull("Null implementation of test PurlManager", this.testPurlManager);
        
        this.testRepository = new SailRepository(new MemoryStore());
        this.testRepository.initialize();
        
        this.testRepositoryConnection = this.testRepository.getConnection();
        this.testRepositoryConnection.begin(); // Transaction per each test
        
        this.testPurlManager.setPurlProcessorFactoryRegistry(this.testRegistry);
    }
    
    @After
    public void tearDown() throws Exception
    {
        this.testRegistry.clear();
        this.testRepositoryConnection.rollback();
        this.testRepositoryConnection.close();
        this.testRepository.shutDown();
        
        this.testPurlManager = null;
    }
    
    /**
     * Tests that a PurlManager is able to replace temporary URIs in a given context of a Repository
     * with previously generated Purls.
     * 
     * This test makes use of PurlManager.extractPurlReferences() to generate new Purls.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertTemporaryUris() throws Exception
    {
        final URI context = this.loadTestResources();
        
        final Set<PoddPurlReference> purlSet =
                this.testPurlManager.extractPurlReferences(this.testRepositoryConnection, context);
        
        this.testPurlManager.convertTemporaryUris(purlSet, this.testRepositoryConnection, context);
        
        // verify temporary URIs no longer exist in the Repository
        final RepositoryResult<Statement> repoContents =
                this.testRepositoryConnection.getStatements(null, null, null, false, context);
        while(repoContents.hasNext())
        {
            final Statement statement = repoContents.next();
            Assert.assertFalse("Temporary URI exists in Subject",
                    AbstractPoddPurlManagerTest.TEMP_URI_PREFIX.contains(statement.getSubject().stringValue()));
            Assert.assertFalse("Temporary URI exists in Object",
                    AbstractPoddPurlManagerTest.TEMP_URI_PREFIX.contains(statement.getObject().stringValue()));
            // System.out.println(statement.toString());
        }
        
        // verify generated Purls exist in the Repository
        for(final PoddPurlReference purl : purlSet)
        {
            // check each Purl is present in the updated RDF statements as a subject or object
            final boolean purlExistsAsSubject =
                    this.testRepositoryConnection.getStatements(purl.getPurlURI(), null, null, false, context)
                            .hasNext();
            final boolean purlExistsAsObject =
                    this.testRepositoryConnection.getStatements(null, null, purl.getPurlURI(), false, context)
                            .hasNext();
            Assert.assertTrue("Purl not found in updated RDF statements", purlExistsAsSubject || purlExistsAsObject);
        }
    }
    
    /**
     * Tests that a PurlManager is able to correctly identify temporary URIs in a given context of a
     * Repository and generate Purls for them.
     * 
     * @throws Exception
     */
    @Test
    public void testExtractPurlReferences() throws Exception
    {
        final URI context = this.loadTestResources();
        
        final Set<PoddPurlReference> purlSet =
                this.testPurlManager.extractPurlReferences(this.testRepositoryConnection, context);
        
        Assert.assertNotNull("Extracted Purl references were null", purlSet);
        Assert.assertFalse("Extracted Purl references were empty", purlSet.isEmpty());
        Assert.assertEquals("Incorrect number of Purl references extracted", 3, purlSet.size());
        
        for(final PoddPurlReference purl : purlSet)
        {
            Assert.assertNotNull("Purl has null temporary URI", purl.getTemporaryURI());
            Assert.assertNotNull("Purl has null permanent URI", purl.getPurlURI());
            
            Assert.assertFalse("Purl and Temporary URI were same", purl.getPurlURI().equals(purl.getTemporaryURI()));
            
            // check temporary URI is present in the original RDF statements as a subject or object
            final boolean tempUriExistsAsSubject =
                    this.testRepositoryConnection.getStatements(purl.getTemporaryURI(), null, null, false, context)
                            .hasNext();
            final boolean tempUriExistsAsObject =
                    this.testRepositoryConnection.getStatements(null, null, purl.getTemporaryURI(), false, context)
                            .hasNext();
            Assert.assertTrue("Temporary URI not found in original RDF statements", tempUriExistsAsSubject
                    || tempUriExistsAsObject);
        }
    }
    
    @Test
    public void testGetPurlProcessorFactoryRegistry() throws Exception
    {
        Assert.assertNotNull("getRegistry() returned null", this.testPurlManager.getPurlProcessorFactoryRegistry());
    }
    
    @Test
    public void testSetPurlProcessorFactoryRegistry() throws Exception
    {
        // first set the Registry to Null and verify it
        this.testPurlManager.setPurlProcessorFactoryRegistry(null);
        Assert.assertNull("Registry was not set to null", this.testPurlManager.getPurlProcessorFactoryRegistry());
        
        // set the Registry
        this.testPurlManager.setPurlProcessorFactoryRegistry(this.testRegistry);
        
        Assert.assertNotNull("getRegistry() returned null ", this.testPurlManager.getPurlProcessorFactoryRegistry());
    }
    
}
