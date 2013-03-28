/**
 * 
 */
package com.github.podd.api.file.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.openrdf.model.URI;

import com.github.podd.api.file.FileReferenceProcessor;
import com.github.podd.api.file.SSHFileReference;
import com.github.podd.api.file.SSHFileReferenceProcessor;
import com.github.podd.api.test.TestConstants;
import com.github.podd.utils.PoddRdfConstants;

/**
 * @author kutila
 * 
 */
public abstract class AbstractSSHFileReferenceProcessorTest extends
        AbstractFileReferenceProcessorTest<SSHFileReference>
{
    @Override
    protected final FileReferenceProcessor<SSHFileReference> getNewFileReferenceProcessor()
    {
        return this.getNewSSHFileReferenceProcessor();
    }
    
    protected abstract SSHFileReferenceProcessor getNewSSHFileReferenceProcessor();
    
    @Override
    protected void verify2FileReferences(final Collection<SSHFileReference> fileReferences)
    {
        Assert.assertNotNull("NULL collection of file references", fileReferences);
        Assert.assertEquals("Expected 2 file references to verify", 2, fileReferences.size());
        
        final List<String> objectIriList =
                Arrays.asList("http://purl.org/podd-test/130326f/object-rice-scan-34343-a",
                        "http://purl.org/podd-test/130326f/object-rice-scan-34343-b");
        
        final List<String> labelList = Arrays.asList("Rice tree scan 003454-98", "Rice tree scan 003454-99");
        final List<String> filenameList = Arrays.asList("plant_003456-233445.bag.zip", "plant_003456-233446.bag.zip");
        
        for(final SSHFileReference sshFileReference : fileReferences)
        {
            Assert.assertNull("Artifact ID should be NULL", sshFileReference.getArtifactID());
            Assert.assertNull("Parent IRI should be NULL", sshFileReference.getParentIri());
            Assert.assertTrue("File Reference URI is not an expected one",
                    objectIriList.contains(sshFileReference.getObjectIri().toString()));
            Assert.assertTrue("Label is not an expected one", labelList.contains(sshFileReference.getLabel()));
            Assert.assertTrue("File name is not an expected one", filenameList.contains(sshFileReference.getFilename()));
        }
    }
    
    @Override
    protected String getPathToResourceWith2FileReferences()
    {
        return TestConstants.TEST_ARTIFACT_PURLS_2_FILE_REFS;
    }
    
    @Override
    protected Set<URI> getExpectedFileReferenceTypes()
    {
        return Collections.singleton(PoddRdfConstants.PODDBASE_FILE_REFERENCE_TYPE_SSH);
    }
    
}
