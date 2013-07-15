/**
 * 
 */
package com.github.podd.impl.file.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyManagerFactoryRegistry;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactoryRegistry;

import com.github.podd.api.PoddOWLManager;
import com.github.podd.api.PoddRepositoryManager;
import com.github.podd.api.file.DataReference;
import com.github.podd.api.file.PoddDataRepository;
import com.github.podd.api.file.PoddDataRepositoryManager;
import com.github.podd.api.file.test.AbstractPoddFileRepositoryManagerTest;
import com.github.podd.exception.FileReferenceNotSupportedException;
import com.github.podd.impl.PoddOWLManagerImpl;
import com.github.podd.impl.PoddRepositoryManagerImpl;
import com.github.podd.impl.file.PoddFileRepositoryManagerImpl;
import com.github.podd.impl.file.SSHFileRepositoryImpl;
import com.github.podd.utils.PoddRdfConstants;

/**
 * This concrete test class uses SSH File References and a test SSH file repository to run through
 * the abstract PoddDataRepositoryManager tests.
 * 
 * @author kutila
 */
public class PoddFileRepositoryManagerImplTest extends AbstractPoddFileRepositoryManagerTest
{
    
    @Rule
    public final TemporaryFolder tempDirectory = new TemporaryFolder();
    
    /** SSH File Repository server for tests */
    protected SSHService sshd;
    
    private Path sshDir = null;
    
    @Override
    protected DataReference buildFileReference(final String alias, final String fileIdentifier)
    {
        try
        {
            return SSHService
                    .getNewFileReference(
                            alias,
                            fileIdentifier,
                            this.tempDirectory.newFolder(
                                    "poddfilerepositoryimpltest-resources-" + UUID.randomUUID().toString()).toPath());
        }
        catch(final IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected PoddDataRepository<?> buildFileRepositoryInstance(final String alias, final Model model)
    {
        // prepare: create a mock PoddDataRepository which can only return the test alias string
        return new PoddDataRepository<DataReference>()
            {
                
                @Override
                public boolean canHandle(final DataReference reference)
                {
                    return false;
                }
                
                @Override
                public String getAlias()
                {
                    return alias;
                }
                
                @Override
                public Model getAsModel()
                {
                    return model;
                }
                
                @Override
                public Set<URI> getTypes()
                {
                    return null;
                }
                
                @Override
                public boolean validate(final DataReference reference) throws FileReferenceNotSupportedException,
                    IOException
                {
                    return false;
                }
            };
    }
    
    @Override
    protected Model buildModelForFileRepository(final URI aliasUri, final String... aliases)
    {
        final Model model = new LinkedHashModel();
        for(final String alias : aliases)
        {
            model.add(aliasUri, PoddRdfConstants.PODD_DATA_REPOSITORY_ALIAS, ValueFactoryImpl.getInstance()
                    .createLiteral(alias));
        }
        model.add(aliasUri, RDF.TYPE, PoddRdfConstants.PODD_DATA_REPOSITORY);
        
        // SSH implementation specific configurations
        model.add(aliasUri, RDF.TYPE, PoddRdfConstants.PODD_SSH_FILE_REPOSITORY);
        model.add(aliasUri, PoddRdfConstants.PODD_DATA_REPOSITORY_PROTOCOL, ValueFactoryImpl.getInstance()
                .createLiteral(SSHFileRepositoryImpl.PROTOCOL_SSH));
        model.add(aliasUri, PoddRdfConstants.PODD_DATA_REPOSITORY_HOST,
                ValueFactoryImpl.getInstance().createLiteral(SSHService.TEST_SSH_HOST));
        model.add(aliasUri, PoddRdfConstants.PODD_DATA_REPOSITORY_PORT,
                ValueFactoryImpl.getInstance().createLiteral(this.sshd.TEST_SSH_SERVICE_PORT));
        model.add(aliasUri, PoddRdfConstants.PODD_FILE_REPOSITORY_FINGERPRINT, ValueFactoryImpl.getInstance()
                .createLiteral(SSHService.TEST_SSH_FINGERPRINT));
        model.add(aliasUri, PoddRdfConstants.PODD_FILE_REPOSITORY_USERNAME, ValueFactoryImpl.getInstance()
                .createLiteral(SSHService.TEST_SSH_USERNAME));
        model.add(aliasUri, PoddRdfConstants.PODD_FILE_REPOSITORY_SECRET,
                ValueFactoryImpl.getInstance().createLiteral(SSHService.TEST_SSH_SECRET));
        
        return model;
    }
    
    @Override
    protected PoddDataRepositoryManager getNewPoddFileRepositoryManager() throws OpenRDFException
    {
        // create a Repository Manager with an internal memory Repository
        final Repository testRepository = new SailRepository(new MemoryStore());
        testRepository.initialize();
        
        final PoddRepositoryManager repositoryManager = new PoddRepositoryManagerImpl();
        repositoryManager.setRepository(testRepository);
        repositoryManager.setFileRepositoryManagementGraph(PoddRdfConstants.DEFAULT_FILE_REPOSITORY_MANAGEMENT_GRAPH);
        
        // create an OWL Manager
        final PoddOWLManager owlManager = new PoddOWLManagerImpl();
        owlManager.setReasonerFactory(OWLReasonerFactoryRegistry.getInstance().getReasonerFactory("Pellet"));
        final OWLOntologyManager manager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        Assert.assertNotNull("Null implementation of OWLOntologymanager", manager);
        owlManager.setOWLOntologyManager(manager);
        
        // create the PoddDataRepositoryManager for testing
        final PoddDataRepositoryManager testFileRepositoryManager = new PoddFileRepositoryManagerImpl();
        testFileRepositoryManager.setRepositoryManager(repositoryManager);
        testFileRepositoryManager.setOWLManager(owlManager);
        
        return testFileRepositoryManager;
    }
    
    @Before
    @Override
    public void setUp() throws Exception
    {
        this.sshDir = this.tempDirectory.newFolder("podd-filerepository-manager-impl-test").toPath();
        this.sshd = new SSHService();
        this.sshd.startTestSSHServer(this.sshDir);
        super.setUp();
    }
    
    @Override
    protected void startRepositorySource() throws Exception
    {
    }
    
    @Override
    protected void stopRepositorySource() throws Exception
    {
        if(this.sshd != null)
        {
            this.sshd.stopTestSSHServer(this.sshDir);
        }
    }
    
}
