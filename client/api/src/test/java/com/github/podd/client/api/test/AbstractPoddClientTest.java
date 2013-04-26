/**
 * 
 */
package com.github.podd.client.api.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.api.file.FileReference;
import com.github.podd.client.api.PoddClient;
import com.github.podd.utils.InferredOWLOntologyID;
import com.github.podd.utils.PoddRdfConstants;

/**
 * Abstract tests for {@link PoddClient}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class AbstractPoddClientTest
{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    
    protected static final String TEST_ADMIN_USER = "testAdminUser";
    protected static final String TEST_ADMIN_PASSWORD = "testAdminPassword";
    
    private PoddClient testClient;
    
    /**
     * Implementing test classes must return a new instance of {@link PoddClient} for each call to
     * this method.
     * 
     * @return A new instance of {@link PoddClient}.
     */
    protected abstract PoddClient getNewPoddClientInstance();
    
    /**
     * Returns the URL of a running PODD Server to test the client against.
     * 
     * @return The URL of the PODD Server to test using.
     */
    protected abstract String getTestPoddServerUrl();
    
    protected Model parseRdf(final InputStream inputStream, final RDFFormat format, final int expectedStatements)
        throws RDFParseException, RDFHandlerException, IOException
    {
        final Model model = Rio.parse(inputStream, "", format);
        Assert.assertEquals(expectedStatements, model.size());
        return model;
    }
    
    /**
     * Resets the test PODD server if possible. If not possible it is not recommended to rely on
     * these tests for extensive verification.
     */
    protected abstract void resetTestPoddServer();
    
    /**
     * Instruct the implementors of this test to attempt to deploy a file reference that has the
     * given label and return a partial FileReference object that contains the specific details of
     * how and where the file reference is located.
     * <p>
     * The {@link FileReference#getArtifactID()}, {@link FileReference#getObjectIri()},
     * {@link FileReference#getParentIri()}, and {@link FileReference#getParentPredicateIRI()}
     * SHOULD not be populated, as they will be overwritten.
     * <p>
     * The {@link FileReference#getRepositoryAlias()} MUST be populated, and MUST match an alias
     * returned from {@link PoddClient#listFileReferenceRepositories()}.
     * <p>
     * Successive calls with different labels must return distinct FileReferences.
     * <p>
     * Successive calls with the same label must return a FileReference containing the same
     * essential details.
     * 
     * @param label
     *            The label to give the file reference.
     * @return A partially populated {@link FileReference} object.
     * @throws Exception
     *             If there were any issues deploying a file reference for this label.
     */
    protected abstract FileReference deployFileReference(String label) throws Exception;
    
    /**
     * Any file repositories necessary to perform file reference attachment tests must be available
     * after this method completes.
     */
    protected abstract void startFileRepositoryTest() throws Exception;
    
    /**
     * Any file repositories that were made active for this test can now be shutdown.
     * <p>
     * For example, an SSH server created specifically for this test may be cleaned up and shutdown.
     */
    protected abstract void endFileRepositoryTest() throws Exception;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.testClient = this.getNewPoddClientInstance();
        Assert.assertNotNull("PODD Client implementation was null", this.testClient);
        
        this.testClient.setPoddServerUrl(this.getTestPoddServerUrl());
        
    }
    
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.resetTestPoddServer();
        this.testClient = null;
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#appendArtifact(OWLOntologyID, InputStream, RDFFormat)}
     * .
     */
    @Ignore
    @Test
    public final void testAppendArtifact() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#attachFileReference(OWLOntologyID, org.semanticweb.owlapi.model.IRI, String, String, String)}
     * .
     */
    @Test
    public final void testAttachFileReference() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        try
        {
            this.startFileRepositoryTest();
            
            // TODO: Pick a project with more child objects
            final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
            Assert.assertNotNull("Test resource missing", input);
            
            final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
            Assert.assertNotNull(newArtifact);
            Assert.assertNotNull(newArtifact.getOntologyIRI());
            Assert.assertNotNull(newArtifact.getVersionIRI());
            
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
            this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
            Model parseRdf =
                    this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                            BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
            
            Model topObject =
                    parseRdf.filter(newArtifact.getOntologyIRI().toOpenRDFURI(), PoddRdfConstants.PODD_BASE_HAS_TOP_OBJECT,
                            null);
            
            Assert.assertEquals("Did not find unique top object in artifact", 1, topObject.size());
            
            FileReference testRef = this.deployFileReference("test-file-label");
            testRef.setArtifactID(newArtifact);
            testRef.setParentIri(IRI.create(topObject.objectURI()));
            // TODO: If this breaks then need to attach it to a different part of an extended project
            testRef.setObjectIri(IRI.create(topObject.objectURI()));
            
            this.testClient.attachFileReference(testRef);
        }
        finally
        {
            this.endFileRepositoryTest();
        }
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#deleteArtifact(OWLOntologyID)} .
     */
    @Test
    public final void testDeleteArtifact() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
        Assert.assertNotNull("Test resource missing", input);
        
        final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
        Assert.assertNotNull(newArtifact);
        Assert.assertNotNull(newArtifact.getOntologyIRI());
        Assert.assertNotNull(newArtifact.getVersionIRI());
        
        // verify that the artifact is accessible and complete
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
        this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
        Model parseRdf =
                this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                        BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
        
        Assert.assertTrue(this.testClient.deleteArtifact(newArtifact));
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#downloadArtifact(InferredOWLOntologyID, java.io.OutputStream, RDFFormat)}
     * .
     */
    @Ignore
    @Test
    public final void testDownloadArtifactCurrentVersion() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#downloadArtifact(InferredOWLOntologyID, java.io.OutputStream, RDFFormat)}
     * .
     */
    @Ignore
    @Test
    public final void testDownloadArtifactDummyVersion() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#downloadArtifact(InferredOWLOntologyID, java.io.OutputStream, RDFFormat)}
     * .
     */
    @Test
    public final void testDownloadArtifactNoVersion() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
        Assert.assertNotNull("Test resource missing", input);
        
        final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
        Assert.assertNotNull(newArtifact);
        Assert.assertNotNull(newArtifact.getOntologyIRI());
        Assert.assertNotNull(newArtifact.getVersionIRI());
        
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
        this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
        this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#downloadArtifact(InferredOWLOntologyID, java.io.OutputStream, RDFFormat)}
     * .
     */
    @Ignore
    @Test
    public final void testDownloadArtifactOldVersion() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#getPoddServerUrl()}.
     */
    @Test
    public final void testGetPoddServerUrlNull() throws Exception
    {
        Assert.assertNotNull(this.testClient.getPoddServerUrl());
        
        this.testClient.setPoddServerUrl(null);
        
        Assert.assertNull(this.testClient.getPoddServerUrl());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listFileReferenceRepositories()}
     * .
     */
    @Ignore
    @Test
    public final void testListFileReferenceRepositories() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listPublishedArtifacts()}.
     */
    @Test
    public final void testListPublishedArtifactsEmpty() throws Exception
    {
        final Collection<InferredOWLOntologyID> results = this.testClient.listPublishedArtifacts();
        Assert.assertTrue(results.isEmpty());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listPublishedArtifacts()}.
     */
    @Ignore
    @Test
    public final void testListPublishedArtifactsMultiple() throws Exception
    {
        // TODO: Create 50 artifacts
        Assert.fail("TODO: Implement me!");
        
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final Collection<InferredOWLOntologyID> results = this.testClient.listPublishedArtifacts();
        Assert.assertFalse(results.isEmpty());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listPublishedArtifacts()}.
     */
    @Ignore
    @Test
    public final void testListPublishedArtifactsSingle() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
        Assert.assertNotNull("Test resource missing", input);
        
        final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
        Assert.assertNotNull(newArtifact);
        Assert.assertNotNull(newArtifact.getOntologyIRI());
        Assert.assertNotNull(newArtifact.getVersionIRI());
        
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
        this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
        this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
        
        // Returns a new version, as when the artifact is published it gets a new version
        final InferredOWLOntologyID publishedArtifact = this.testClient.publishArtifact(newArtifact);
        
        this.testClient.logout();
        
        final Collection<InferredOWLOntologyID> results = this.testClient.listPublishedArtifacts();
        Assert.assertFalse(results.isEmpty());
        Assert.assertEquals(-1, results.size());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listUnpublishedArtifacts()}.
     */
    @Test
    public final void testListUnpublishedArtifactsEmpty() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final Collection<InferredOWLOntologyID> results = this.testClient.listUnpublishedArtifacts();
        this.log.info("unpublished artifacts which should be empty: {}", results);
        Assert.assertTrue(results.isEmpty());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listUnpublishedArtifacts()}.
     */
    @Ignore
    @Test
    public final void testListUnpublishedArtifactsMultiple() throws Exception
    {
        // TODO: Create 50 artifacts
        Assert.fail("TODO: Implement me!");
        
        final Collection<InferredOWLOntologyID> results = this.testClient.listUnpublishedArtifacts();
        Assert.assertFalse(results.isEmpty());
        Assert.assertEquals(50, results.size());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#listUnpublishedArtifacts()}.
     */
    @Test
    public final void testListUnpublishedArtifactsSingle() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
        Assert.assertNotNull("Test resource missing", input);
        
        final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
        Assert.assertNotNull(newArtifact);
        Assert.assertNotNull(newArtifact.getOntologyIRI());
        Assert.assertNotNull(newArtifact.getVersionIRI());
        
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
        this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
        this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
        
        final Collection<InferredOWLOntologyID> results = this.testClient.listUnpublishedArtifacts();
        Assert.assertFalse(results.isEmpty());
        Assert.assertEquals(1, results.size());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#login(java.lang.String, String)}
     * .
     */
    @Test
    public final void testLogin() throws Exception
    {
        Assert.assertFalse(this.testClient.isLoggedIn());
        
        Assert.assertTrue("Client was not successfully logged in",
                this.testClient.login("testAdminUser", "testAdminPassword"));
        
        Assert.assertTrue(this.testClient.isLoggedIn());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#logout()}.
     */
    @Test
    public final void testLogout() throws Exception
    {
        this.testClient.setPoddServerUrl(this.getTestPoddServerUrl());
        
        Assert.assertFalse(this.testClient.isLoggedIn());
        
        Assert.assertTrue("Client was not successfully logged in",
                this.testClient.login("testAdminUser", "testAdminPassword"));
        
        Assert.assertTrue(this.testClient.isLoggedIn());
        
        Assert.assertTrue("Client was not successfully logged out", this.testClient.logout());
        
        Assert.assertFalse(this.testClient.isLoggedIn());
    }
    
    /**
     * Test method for {@link com.github.podd.client.api.PoddClient#publishArtifact(OWLOntologyID)}
     * .
     */
    @Ignore
    @Test
    public final void testPublishArtifact() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#setPoddServerUrl(java.lang.String)}.
     */
    @Test
    public final void testSetPoddServerUrl() throws Exception
    {
        Assert.assertNotNull(this.testClient.getPoddServerUrl());
        
        this.testClient.setPoddServerUrl(null);
        
        Assert.assertNull(this.testClient.getPoddServerUrl());
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#unpublishArtifact(InferredOWLOntologyID)} .
     */
    @Ignore
    @Test
    public final void testUnpublishArtifact()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#updateArtifact(InferredOWLOntologyID, InputStream, RDFFormat)}
     * .
     */
    @Ignore
    @Test
    public final void testUpdateArtifact() throws Exception
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.client.api.PoddClient#uploadNewArtifact(java.io.InputStream, org.openrdf.rio.RDFFormat)}
     * .
     */
    @Test
    public final void testUploadNewArtifact() throws Exception
    {
        this.testClient.login(AbstractPoddClientTest.TEST_ADMIN_USER, AbstractPoddClientTest.TEST_ADMIN_PASSWORD);
        
        final InputStream input = this.getClass().getResourceAsStream("/test/artifacts/basicProject-1.rdf");
        Assert.assertNotNull("Test resource missing", input);
        
        final InferredOWLOntologyID newArtifact = this.testClient.uploadNewArtifact(input, RDFFormat.RDFXML);
        Assert.assertNotNull(newArtifact);
        Assert.assertNotNull(newArtifact.getOntologyIRI());
        Assert.assertNotNull(newArtifact.getVersionIRI());
        
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8096);
        this.testClient.downloadArtifact(newArtifact, outputStream, RDFFormat.RDFJSON);
        final Model model =
                this.parseRdf(new ByteArrayInputStream(outputStream.toByteArray()), RDFFormat.RDFJSON,
                        BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES);
        
        Assert.assertTrue(model.contains(newArtifact.getOntologyIRI().toOpenRDFURI(), RDF.TYPE, OWL.ONTOLOGY));
        Assert.assertTrue(model.contains(newArtifact.getOntologyIRI().toOpenRDFURI(), OWL.VERSIONIRI, newArtifact
                .getVersionIRI().toOpenRDFURI()));
    }
    
    private static final int BASIC_PROJECT_1_EXPECTED_CONCRETE_TRIPLES = 26;
}
