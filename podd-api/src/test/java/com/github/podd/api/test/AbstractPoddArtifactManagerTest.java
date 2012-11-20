/**
 * 
 */
package com.github.podd.api.test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.semanticweb.owlapi.model.OWLOntologyID;

import com.github.podd.api.PoddArtifactManager;
import com.github.podd.api.PoddProcessorStage;
import com.github.podd.api.file.PoddFileReference;
import com.github.podd.api.file.PoddFileReferenceManager;
import com.github.podd.api.file.PoddFileReferenceProcessor;
import com.github.podd.api.file.PoddFileReferenceProcessorFactory;
import com.github.podd.api.file.PoddFileReferenceProcessorFactoryRegistry;
import com.github.podd.utils.InferredOWLOntologyID;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public abstract class AbstractPoddArtifactManagerTest
{
    protected abstract PoddArtifactManager getNewArtifactManager();
    
    protected abstract PoddFileReferenceManager getNewFileReferenceManager();
    
    protected abstract PoddFileReferenceProcessorFactory getNewHttpFileReferenceProcessorFactory();
    
    protected abstract PoddFileReferenceProcessorFactory getNewSSHFileReferenceProcessorFactory();
    
    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("null")
    @Before
    public void setUp() throws Exception
    {
        final PoddFileReferenceProcessorFactoryRegistry testRegistry = new PoddFileReferenceProcessorFactoryRegistry();
        // clear any automatically added entries that may come from META-INF/services entries on the
        // classpath
        testRegistry.clear();
        // In practice, the following factories would be automatically added to the registry,
        // however for testing we want to explicitly add the ones we want to support for each test
        testRegistry.add(this.getNewSSHFileReferenceProcessorFactory());
        testRegistry.add(this.getNewHttpFileReferenceProcessorFactory());
        
        final PoddFileReferenceManager testFileReferenceManager = this.getNewFileReferenceManager();
        testFileReferenceManager.setProcessorFactoryRegistry(testRegistry);
        
        final PoddArtifactManager testArtifactManager = this.getNewArtifactManager();
        testArtifactManager.setFileReferenceManager(testFileReferenceManager);
        
        final InputStream inputStream = this.getClass().getResourceAsStream("/testArtifact.rdf");
        // MIME type should be either given by the user, detected from the content type on the
        // request, or autodetected using the Any23 Mime Detector
        final String mimeType = "application/rdf+xml";
        final RDFFormat format = Rio.getParserFormatForMIMEType(mimeType, RDFFormat.RDFXML);
        final InferredOWLOntologyID resultArtifactId = testArtifactManager.loadArtifact(inputStream, format);
        
        // INSIDE the loadArtifact method...
        
        // testArtifactManager needs to create artifactId before attempting to extract file
        // references
        final OWLOntologyID tempArtifactId = null;
        
        // connection to the repository that the file reference is being stored in
        final RepositoryConnection conn = null;
        
        // calls, to setup the results collection
        final Set<PoddFileReference> results =
                testArtifactManager.getFileReferenceManager().extractFileReferences(conn,
                        tempArtifactId.getVersionIRI().toOpenRDFURI());
        
        // INSIDE the extractFileReferences method....
        final Set<PoddFileReference> internalResults =
                Collections.newSetFromMap(new ConcurrentHashMap<PoddFileReference, Boolean>());
        
        // TODO: This needs to be a constant
        final URI poddFileReferenceType =
                conn.getValueFactory().createURI("http://purl.org/podd/ns/poddBase#PoddFileReference");
        
        for(final PoddFileReferenceProcessorFactory nextProcessorFactory : testArtifactManager
                .getFileReferenceManager().getProcessorFactoryRegistry().getByStage(PoddProcessorStage.RDF_PARSING))
        {
            final StringBuilder sparqlQuery = new StringBuilder();
            sparqlQuery.append("CONSTRUCT { ");
            sparqlQuery.append(nextProcessorFactory.getSPARQLConstructBGP());
            sparqlQuery.append(" } WHERE { ");
            sparqlQuery.append(nextProcessorFactory.getSPARQLConstructWhere());
            sparqlQuery.append(" } ");
            if(!nextProcessorFactory.getSPARQLGroupBy().isEmpty())
            {
                sparqlQuery.append(" GROUP BY ");
                sparqlQuery.append(nextProcessorFactory.getSPARQLGroupBy());
            }
            
            final GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
            
            // Create a new dataset to specify the contexts that the query will be allowed to access
            final DatasetImpl dataset = new DatasetImpl();
            // The following URIs are passed in as the context to the
            // extractFileReferences(RepositoryConnection,URI...) method
            final URI artifactGraphUri = null;
            
            // for(URI artifactGraphUri : contexts)
            dataset.addDefaultGraph(artifactGraphUri);
            dataset.addNamedGraph(artifactGraphUri);
            // end for loop
            
            // if the stage is after inferencing, the inferred graph URI would be one of the
            // contexts as well
            final URI artifactInferredGraphUri = null;
            dataset.addDefaultGraph(artifactInferredGraphUri);
            dataset.addNamedGraph(artifactInferredGraphUri);
            
            // set the dataset for the query to be our artificially constructed dataset
            graphQuery.setDataset(dataset);
            
            final GraphQueryResult queryResult = graphQuery.evaluate();
            
            // If the query matched anything, then for each of the file references in the resulting
            // construct statements, we create a file reference and add it to the results
            if(!queryResult.hasNext())
            {
                final Graph graph = new GraphImpl();
                while(queryResult.hasNext())
                {
                    graph.add(queryResult.next());
                }
                
                final Iterator<Statement> match = graph.match(null, RDF.TYPE, poddFileReferenceType);
                
                while(match.hasNext())
                {
                    final Statement nextStatement = match.next();
                    
                    final Graph fileReferenceGraph = new GraphImpl();
                    
                    // For generality, in practice for now, use graph.match as below...
                    nextProcessorFactory.getSPARQLConstructWhere((URI)nextStatement.getSubject());
                    
                    final Iterator<Statement> match2 = graph.match(nextStatement.getSubject(), null, null);
                    
                    while(match2.hasNext())
                    {
                        fileReferenceGraph.add(match2.next());
                    }
                    
                    // This processor factory matches the graph that we wish to use, so we create a
                    // processor instance now to create the file reference
                    // NOTE: This object cannot be shared as we do not specify that it needs to be
                    // threadsafe
                    final PoddFileReferenceProcessor processor = nextProcessorFactory.getProcessor();
                    
                    // create a reference out of the resulting graph
                    internalResults.add(processor.createReference(fileReferenceGraph));
                }
            }
        }
        
        // return the results, setting the results variable to be the same as the internalResults
        // variable from inside of extractFileReferences
        // Ie, return internalResults; results = internalResults;
    }
    
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#deregisterProcessor(com.github.podd.api.PoddProcessorFactory, com.github.podd.api.PoddProcessorStage)}
     * .
     */
    @Test
    public final void testDeregisterProcessor()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#getProcessors(com.github.podd.api.PoddProcessorStage)}
     * .
     */
    @Test
    public final void testGetProcessors()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#loadArtifact(java.io.InputStream, org.openrdf.rio.RDFFormat)}
     * .
     */
    @Test
    public final void testLoadArtifact()
    {
        
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#publishArtifact(org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     */
    @Test
    public final void testPublishArtifact()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#registerProcessor(com.github.podd.api.PoddProcessorFactory, com.github.podd.api.PoddProcessorStage)}
     * .
     */
    @Test
    public final void testRegisterProcessor()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
    /**
     * Test method for
     * {@link com.github.podd.api.PoddArtifactManager#updateSchemaImport(org.semanticweb.owlapi.model.OWLOntologyID, org.semanticweb.owlapi.model.OWLOntologyID)}
     * .
     */
    @Test
    public final void testUpdateSchemaImport()
    {
        Assert.fail("Not yet implemented"); // TODO
    }
    
}