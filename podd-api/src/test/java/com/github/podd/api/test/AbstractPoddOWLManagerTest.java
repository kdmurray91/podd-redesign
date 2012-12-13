/**
 * 
 */
package com.github.podd.api.test;

import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.semanticweb.owlapi.formats.OWLOntologyFormatFactoryRegistry;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyManagerFactoryRegistry;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.rio.RioMemoryTripleSource;

import com.github.podd.api.PoddOWLManager;
import com.github.podd.exception.EmptyOntologyException;
import com.github.podd.utils.InferredOWLOntologyID;

/**
 * Abstract test to verify that the PoddOWLManager API contract is followed by implementations.
 * 
 * TODO: add test cases for non-default cases (e.g. empty/null/invalid/non-matching values)
 * 
 * @author kutila
 * 
 */
public abstract class AbstractPoddOWLManagerTest
{
    
    private String poddBaseResourcePath = "/ontologies/poddBase.owl";
    
    protected PoddOWLManager testOWLManager;
    
    private Repository testRepository;
    
    protected RepositoryConnection testRepositoryConnection;
    
    /**
     * @return A new OWLReasonerFactory instance for use with the PoddOWLManager
     */
    protected abstract OWLReasonerFactory getNewOWLReasonerFactoryInstance();
    
    /**
     * @return A new instance of PoddOWLManager, for each call to this method
     */
    protected abstract PoddOWLManager getNewPoddOWLManagerInstance();
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.testOWLManager = this.getNewPoddOWLManagerInstance();
        Assert.assertNotNull("Null implementation of test OWLManager", this.testOWLManager);
        
        // set an OWLOntologyManager for this PoddOWLManager
        final OWLOntologyManager manager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        Assert.assertNotNull("Null implementation of OWLOntologymanager", manager);
        this.testOWLManager.setOWLOntologyManager(manager);
        
        // set a ReasonerFactory for this PoddOWLManager
        final OWLReasonerFactory reasonerFactory = this.getNewOWLReasonerFactoryInstance();
        Assert.assertNotNull("Null implementation of reasoner factory", reasonerFactory);
        this.testOWLManager.setReasonerFactory(reasonerFactory);
        
        // create a memory Repository for tests
        this.testRepository = new SailRepository(new MemoryStore());
        this.testRepository.initialize();
        this.testRepositoryConnection = this.testRepository.getConnection();
        this.testRepositoryConnection.begin();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.testRepositoryConnection.rollback();
        this.testRepositoryConnection.close();
        this.testRepository.shutDown();
        
        this.testOWLManager = null;
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#cacheSchemaOntology(com.github.podd.utils.InferredOWLOntologyID, org.openrdf.repository.RepositoryConnection)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testCacheSchemaOntology() throws Exception
    {
        // prepare: load a schema ontology independently
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        final OWLOntologyManager testOWLOntologyManager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        final OWLOntology loadedOntology = testOWLOntologyManager.loadOntologyFromOntologyDocument(inputStream);
        
        final URI schemaOntologyGraph = loadedOntology.getOntologyID().getVersionIRI().toOpenRDFURI();
        final URI inferredOntologyGraph = null;
        
        // prepare: load poddBase schema ontology into the test repository
        this.testRepositoryConnection.add(inputStream, "", RDFFormat.RDFXML, schemaOntologyGraph);
        final List<Statement> statements =
                this.testRepositoryConnection.getStatements(null, null, null, false, schemaOntologyGraph).asList();
        Assert.assertEquals("Not the expected number of statements in Repository", 278, statements.size());
        
        final InferredOWLOntologyID inferredOntologyID = null;
        
        this.testOWLManager.cacheSchemaOntology(inferredOntologyID, this.testRepositoryConnection);
        
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#createReasoner(org.semanticweb.owlapi.model.OWLOntology)}
     * .
     * 
     */
    @Test
    public void testCreateReasoner() throws Exception
    {
        // prepare: load an Ontology independently
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        final OWLOntologyManager testOWLOntologyManager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        final OWLOntology loadedOntology = testOWLOntologyManager.loadOntologyFromOntologyDocument(inputStream);
        
        final OWLReasoner reasoner = this.testOWLManager.createReasoner(loadedOntology);
        
        // verify:
        Assert.assertNotNull("Created reasoner was NULL", reasoner);
        Assert.assertEquals(this.testOWLManager.getReasonerFactory().getReasonerName(), reasoner.getReasonerName());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#createReasoner(org.semanticweb.owlapi.model.OWLOntology)}
     * .
     * 
     */
    @Test
    public void testCreateReasonerFromEmptyOntology() throws Exception
    {
        // prepare: load an Ontology independently
        final OWLOntologyManager testOWLOntologyManager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        final OWLOntology emptyOntology = testOWLOntologyManager.createOntology();
        
        final OWLReasoner reasoner = this.testOWLManager.createReasoner(emptyOntology);
        
        // verify:
        Assert.assertNotNull("Created reasoner was NULL", reasoner);
        Assert.assertEquals(this.testOWLManager.getReasonerFactory().getReasonerName(), reasoner.getReasonerName());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#createReasoner(org.semanticweb.owlapi.model.OWLOntology)}
     * .
     * 
     */
    @Test
    public void testCreateReasonerWithNull() throws Exception
    {
        try
        {
            this.testOWLManager.createReasoner(null);
            Assert.fail("Should have thrown a Runtime Exception");
        }
        catch(final RuntimeException e)
        {
            Assert.assertTrue("Exception not expected type", e instanceof NullPointerException);
            // this exception is thrown by the OWL API with a null message
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#generateInferredOntologyID(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testGenerateInferredOntologyID() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#getCurrentVersion(org.semanticweb.owlapi.model.IRI)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testGetCurrentVersion() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#getOntology(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testGetOntology() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for {@link com.github.podd.api.PoddOWLManager#getOWLOntologyManager()} .
     * 
     */
    @Test
    public void testGetOWLOntologyManagerWithMockObject() throws Exception
    {
        this.testOWLManager.setOWLOntologyManager(null);
        Assert.assertNull("OWLOntologyManager should have been null", this.testOWLManager.getOWLOntologyManager());
        
        final OWLOntologyManager mockOWLOntologyManager = Mockito.mock(OWLOntologyManager.class);
        this.testOWLManager.setOWLOntologyManager(mockOWLOntologyManager);
        
        Assert.assertNotNull("OWLOntologyManager was not set", this.testOWLManager.getOWLOntologyManager());
        Assert.assertEquals("Not the expected mock OWLManager", mockOWLOntologyManager,
                this.testOWLManager.getOWLOntologyManager());
    }
    
    /**
     * Test method for {@link com.github.podd.api.PoddOWLManager#getReasonerFactory()} .
     * 
     */
    @Test
    public void testGetReasonerFactoryWithMockObject() throws Exception
    {
        this.testOWLManager.setReasonerFactory(null);
        Assert.assertNull("ReasonerFactory should have been null", this.testOWLManager.getReasonerFactory());
        
        final OWLReasonerFactory mockReasonerFactory = Mockito.mock(OWLReasonerFactory.class);
        
        this.testOWLManager.setReasonerFactory(mockReasonerFactory);
        
        Assert.assertNotNull("The reasoner factory was not set", this.testOWLManager.getReasonerFactory());
        Assert.assertEquals("Not the expected mock ReasonerFactory", mockReasonerFactory,
                this.testOWLManager.getReasonerFactory());
    }
    
    /**
     * Test method for {@link com.github.podd.api.PoddOWLManager#getReasonerProfile()} .
     * 
     */
    @Ignore
    @Test
    public void testGetReasonerProfile() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#getVersions(org.semanticweb.owlapi.model.IRI)} .
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void testGetVersion() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#inferStatements(com.github.podd.utils.InferredOWLOntologyID, org.openrdf.repository.RepositoryConnection)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testInferStatements() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#isPublished(org.semanticweb.owlapi.model.IRI)} .
     * 
     */
    @Ignore
    @Test
    public void testIsPublishedIRI() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#isPublished(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testIsPublishedOWLOntologyID() throws Exception
    {
        // look for object property http://purl.org/podd/ns/poddBase#hasPublicationStatus
        
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#loadOntology(org.semanticweb.owlapi.rio.RioMemoryTripleSource)}
     * . Attempts to load an RDF resource which does not contain an ontology.
     */
    @Test
    public void testLoadOntologyFromEmptyOWLOntologyDocumentSource() throws Exception
    {
        // prepare: load an ontology into a StreamDocumentSource
        final InputStream inputStream = this.getClass().getResourceAsStream("/test/ontologies/empty.owl");
        Assert.assertNotNull("Could not find resource", inputStream);
        
        final OWLOntologyDocumentSource owlSource =
                new StreamDocumentSource(inputStream, OWLOntologyFormatFactoryRegistry.getInstance().getByMIMEType(
                        RDFFormat.RDFXML.getDefaultMIMEType()));
        
        try
        {
            this.testOWLManager.loadOntology(owlSource);
            Assert.fail("Should have thrown an OWLOntologyCreationException");
        }
        catch(final EmptyOntologyException e)
        {
            Assert.assertEquals("Unexpected message in expected Exception", "Loaded ontology is empty", e.getMessage());
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#loadOntology(org.semanticweb.owlapi.rio.RioMemoryTripleSource)}
     * .
     * 
     */
    @Test
    public void testLoadOntologyFromOWLOntologyDocumentSource() throws Exception
    {
        // prepare: load an ontology into a StreamDocumentSource
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        
        final OWLOntologyDocumentSource owlSource =
                new StreamDocumentSource(inputStream, OWLOntologyFormatFactoryRegistry.getInstance().getByMIMEType(
                        RDFFormat.RDFXML.getDefaultMIMEType()));
        
        final OWLOntology loadedOntology = this.testOWLManager.loadOntology(owlSource);
        
        // verify:
        Assert.assertNotNull(loadedOntology);
        Assert.assertEquals("<http://purl.org/podd/ns/poddBase>", loadedOntology.getOntologyID().getOntologyIRI()
                .toQuotedString());
        Assert.assertEquals("<http://purl.org/podd/ns/version/poddBase/1>", loadedOntology.getOntologyID()
                .getVersionIRI().toQuotedString());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#loadOntology(org.semanticweb.owlapi.rio.RioMemoryTripleSource)}
     * .
     * 
     */
    @Test
    public void testLoadOntologyFromRioMemoryTripleSource() throws Exception
    {
        // prepare: load an ontology into a RioMemoryTripleSource via the test repository
        final URI context = ValueFactoryImpl.getInstance().createURI("urn:context:test");
        
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        
        this.testRepositoryConnection.add(inputStream, "", RDFFormat.RDFXML, context);
        final List<Statement> statements =
                this.testRepositoryConnection.getStatements(null, null, null, false, context).asList();
        Assert.assertEquals("Not the expected number of statements in Repository", 278, statements.size());
        
        final RioMemoryTripleSource owlSource = new RioMemoryTripleSource(statements.iterator());
        
        final OWLOntology loadedOntology = this.testOWLManager.loadOntology(owlSource);
        
        // verify:
        Assert.assertNotNull(loadedOntology);
        Assert.assertEquals("<http://purl.org/podd/ns/poddBase>", loadedOntology.getOntologyID().getOntologyIRI()
                .toQuotedString());
        Assert.assertEquals("<http://purl.org/podd/ns/version/poddBase/1>", loadedOntology.getOntologyID()
                .getVersionIRI().toQuotedString());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#loadOntology(org.semanticweb.owlapi.rio.RioMemoryTripleSource)}
     * . Attempts to load a non-RDF resource.
     */
    @Test
    public void testLoadOntologyFromTextDocumentSource() throws Exception
    {
        // prepare: load an ontology into a StreamDocumentSource
        final InputStream inputStream = this.getClass().getResourceAsStream("/test/ontologies/justatextfile.owl");
        Assert.assertNotNull("Could not find resource", inputStream);
        
        final OWLOntologyDocumentSource owlSource =
                new StreamDocumentSource(inputStream, OWLOntologyFormatFactoryRegistry.getInstance().getByMIMEType(
                        RDFFormat.RDFXML.getDefaultMIMEType()));
        
        try
        {
            this.testOWLManager.loadOntology(owlSource);
            Assert.fail("Should have thrown an OWLOntologyCreationException");
        }
        catch(final OWLOntologyCreationException e)
        {
            Assert.assertTrue("Exception not expected type", e instanceof UnparsableOntologyException);
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#loadOntology(org.semanticweb.owlapi.rio.RioMemoryTripleSource)}
     * . Attempts to pass NULL value into loadOntlogy().
     */
    @Test
    public void testLoadOntologyWithNull() throws Exception
    {
        try
        {
            this.testOWLManager.loadOntology(null);
            Assert.fail("Should have thrown a RuntimeException");
        }
        catch(final RuntimeException e)
        {
            Assert.assertTrue("Exception not expected type", e instanceof NullPointerException);
            // this exception is thrown by the OWL API with a null message
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#parseRDFStatements(org.openrdf.repository.RepositoryConnection, org.openrdf.model.URI...)}
     * .
     * 
     */
    @Test
    public void testParseRDFStatements() throws Exception
    {
        // prepare: load poddBase schema ontology into the test repository
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        
        final URI context = ValueFactoryImpl.getInstance().createURI("urn:test:context:");
        this.testRepositoryConnection.add(inputStream, "", RDFFormat.RDFXML, context);
        final List<Statement> statements =
                this.testRepositoryConnection.getStatements(null, null, null, false, context).asList();
        Assert.assertEquals("Not the expected number of statements in Repository", 278, statements.size());
        
        final OWLOntologyID loadedOntologyID =
                this.testOWLManager.parseRDFStatements(this.testRepositoryConnection, context);
        
        // verify:
        Assert.assertNotNull("OntologyID was null", loadedOntologyID);
        Assert.assertEquals("<http://purl.org/podd/ns/poddBase>", loadedOntologyID.getOntologyIRI().toQuotedString());
        Assert.assertEquals("<http://purl.org/podd/ns/version/poddBase/1>", loadedOntologyID.getVersionIRI()
                .toQuotedString());
        
        final OWLOntology loadedOntology = this.testOWLManager.getOntology(loadedOntologyID);
        Assert.assertNotNull("Ontology not in memory", loadedOntology);
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#parseRDFStatements(org.openrdf.repository.RepositoryConnection, org.openrdf.model.URI...)}
     * .
     * 
     */
    @Test
    public void testParseRDFStatementsFromEmptyRepository() throws Exception
    {
        final URI context = ValueFactoryImpl.getInstance().createURI("urn:test:context:");
        try
        {
            this.testOWLManager.parseRDFStatements(this.testRepositoryConnection, context);
        }
        catch(final EmptyOntologyException e)
        {
            Assert.assertEquals("No statements to create an ontology", e.getMessage());
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#parseRDFStatements(org.openrdf.repository.RepositoryConnection, org.openrdf.model.URI...)}
     * . This test asserts that when the RepositoryConnection has just one statement in it, a
     * non-empty and anonymous Ontology is loaded to the memory.
     */
    @Test
    public void testParseRDFStatementsFromRepositoryWithOneStatement() throws Exception
    {
        // prepare: add a single statement to the Repository so that it is not empty
        final URI context = ValueFactoryImpl.getInstance().createURI("urn:test:context:");
        
        this.testRepositoryConnection.add(ValueFactoryImpl.getInstance().createURI("urn:dummy:subject"), RDF.TYPE,
                ValueFactoryImpl.getInstance().createURI("urn:dummy:object"), context);
        
        final OWLOntologyID loadedOntologyID =
                this.testOWLManager.parseRDFStatements(this.testRepositoryConnection, context);
        
        // verify:
        Assert.assertNotNull("OntologyID was null", loadedOntologyID);
        
        Assert.assertNull("Was not an anonymous ontology", loadedOntologyID.getOntologyIRI());
        Assert.assertNull("Was not an anonymous ontology", loadedOntologyID.getVersionIRI());
        
        final OWLOntology loadedOntology = this.testOWLManager.getOntology(loadedOntologyID);
        Assert.assertNotNull("Ontology not in memory", loadedOntology);
        Assert.assertFalse("Ontology is empty", loadedOntology.isEmpty());
        Assert.assertEquals("Not the expected number of axioms", 1, loadedOntology.getAxiomCount());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#removeCache(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Test
    public void testRemoveCache() throws Exception
    {
        // prepare: load an ontology into the OWLManager
        final InputStream inputStream = this.getClass().getResourceAsStream(this.poddBaseResourcePath);
        Assert.assertNotNull("Could not find resource", inputStream);
        final OWLOntologyDocumentSource owlSource =
                new StreamDocumentSource(inputStream, OWLOntologyFormatFactoryRegistry.getInstance().getByMIMEType(
                        RDFFormat.RDFXML.getDefaultMIMEType()));
        final OWLOntology loadedOntology = this.testOWLManager.loadOntology(owlSource);
        
        final OWLOntologyID ontologyID = loadedOntology.getOntologyID();
        final OWLOntology ontologyLoadedFromMemory = this.testOWLManager.getOntology(ontologyID);
        Assert.assertNotNull("Ontology should be in memory", ontologyLoadedFromMemory);
        
        final boolean removed = this.testOWLManager.removeCache(ontologyID);
        
        // verify:
        Assert.assertTrue("Ontology could not be removed from cache", removed);
        
        final OWLOntology ontologyFromMemoryShouldBeNull = this.testOWLManager.getOntology(ontologyID);
        Assert.assertNull("Ontology is still in cache", ontologyFromMemoryShouldBeNull);
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#removeCache(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Test
    public void testRemoveCacheWithOntologyNotInMemory() throws Exception
    {
        // prepare: create an ontology externally
        final OWLOntology ontologyLoadedFromMemory =
                OWLOntologyManagerFactoryRegistry.createOWLOntologyManager().createOntology();
        Assert.assertNotNull("Ontology should not be in memory", ontologyLoadedFromMemory);
        
        final OWLOntologyID ontologyID = ontologyLoadedFromMemory.getOntologyID();
        final boolean removed = this.testOWLManager.removeCache(ontologyID);
        
        // verify:
        Assert.assertFalse("Ontology should not have existed in memory/cache", removed);
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#removeCache(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Test
    public void testRemoveCacheWithEmptyOntology() throws Exception
    {
        // prepare: create an empty ontology inside this OWLManager
        final OWLOntologyID ontologyID = this.testOWLManager.getOWLOntologyManager().createOntology().getOntologyID();
        final OWLOntology theOntologyFromMemory = this.testOWLManager.getOntology(ontologyID);
        Assert.assertNotNull("The ontology was not in memory", theOntologyFromMemory);
        Assert.assertTrue("Ontology was not empty", theOntologyFromMemory.isEmpty());
        
        final boolean removed = this.testOWLManager.removeCache(ontologyID);
        
        // verify:
        Assert.assertTrue("Ontology could not be removed from cache", removed);
        
        final OWLOntology ontologyFromMemoryShouldBeNull = this.testOWLManager.getOntology(ontologyID);
        Assert.assertNull("Ontology is still in cache", ontologyFromMemoryShouldBeNull);
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#removeCache(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Test
    public void testRemoveCacheWithNullOntology() throws Exception
    {
        try
        {
            this.testOWLManager.removeCache(null);
            Assert.fail("Should have thrown a RuntimeException");
        }
        catch(final RuntimeException e)
        {
            Assert.assertTrue("Not the expected type of Exception", e instanceof NullPointerException);
            // this exception is thrown by the OWL API with a null message
        }
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setCurrentVersion(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     */
    @Ignore
    @Test
    public void testSetCurrentVersion() throws Exception
    {
        // omv.ontoware/currentversion
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setOWLOntologyManager(org.semanticweb.owlapi.model.OWLOntologyManager)}
     * .
     * 
     */
    @Test
    public void testSetOWLOntologyManager() throws Exception
    {
        // set null to forget the manager being set in setUp()
        this.testOWLManager.setOWLOntologyManager(null);
        Assert.assertNull("OWLOntologyManager could not be set to NULL", this.testOWLManager.getOWLOntologyManager());
        
        final OWLOntologyManager manager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        Assert.assertNotNull("Null implementation of OWLOntologymanager", manager);
        
        this.testOWLManager.setOWLOntologyManager(manager);
        
        Assert.assertNotNull("OWLOntologyManager was not set", this.testOWLManager.getOWLOntologyManager());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setOWLOntologyManager(org.semanticweb.owlapi.model.OWLOntologyManager)}
     * .
     * 
     */
    @Test
    public void testSetOWLOntologyManagerWithNull() throws Exception
    {
        this.testOWLManager.setOWLOntologyManager(null);
        Assert.assertNull("OWLOntologyManager could not be set to NULL", this.testOWLManager.getOWLOntologyManager());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setPublished(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void testSetPublished() throws Exception
    {
        Assert.fail("TODO: Implement me");
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setReasonerFactory(org.semanticweb.owlapi.reasoner.OWLReasonerFactory)}
     * .
     * 
     */
    @Test
    public void testSetReasonerFactory() throws Exception
    {
        // set null to forget the reasoner factory being set in setUp()
        this.testOWLManager.setReasonerFactory(null);
        Assert.assertNull("The reasoner factory could not be set to NULL", this.testOWLManager.getReasonerFactory());
        
        final OWLReasonerFactory reasonerFactory = this.getNewOWLReasonerFactoryInstance();
        Assert.assertNotNull("Null implementation of reasoner factory", reasonerFactory);
        
        this.testOWLManager.setReasonerFactory(reasonerFactory);
        
        Assert.assertNotNull("The reasoner factory was not set", this.testOWLManager.getReasonerFactory());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddOWLManager#setReasonerFactory(org.semanticweb.owlapi.reasoner.OWLReasonerFactory)}
     * .
     * 
     */
    @Test
    public void testSetReasonerFactoryWithNull() throws Exception
    {
        this.testOWLManager.setReasonerFactory(null);
        Assert.assertNull("The reasoner factory could not be set to NULL", this.testOWLManager.getReasonerFactory());
    }
}
