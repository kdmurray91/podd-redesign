/**
 * 
 */
package com.github.podd.restlet.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.github.ansell.restletutils.RestletUtilMediaType;
import com.github.ansell.restletutils.test.RestletTestUtils;
import com.github.podd.utils.PoddWebConstants;

/**
 * Test various forms of GetArtifact
 * 
 * @author kutila
 * 
 */
public class GetArtifactResourceImplTest extends AbstractResourceImplTest
{
    
    /**
     * Test access without artifactID parameter gives a BAD_REQUEST error.
     */
    @Test
    public void testErrorGetArtifactWithoutArtifactId() throws Exception
    {
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        try
        {
            getArtifactClientResource.get(MediaType.TEXT_HTML);
            Assert.fail("Should have thrown a ResourceException with Status Code 400");
        }
        catch(final ResourceException e)
        {
            Assert.assertEquals("Not the expected HTTP status code", Status.CLIENT_ERROR_BAD_REQUEST, e.getStatus());
        }
    }
    
    /**
     * Test unauthenticated access gives an UNAUTHORIZED error.
     */
    @Test
    public void testErrorGetArtifactWithoutAuthentication() throws Exception
    {
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER,
                "http://purl.org/podd/ns/artifact/artifact89");
        
        try
        {
            getArtifactClientResource.get(MediaType.TEXT_HTML);
            Assert.fail("Should have thrown a ResourceException with Status Code 401");
        }
        catch(final ResourceException e)
        {
            Assert.assertEquals("Not the expected HTTP status code", Status.CLIENT_ERROR_UNAUTHORIZED, e.getStatus());
        }
    }
    
    /**
     * Test authenticated access to get Artifact in HTML
     */
    @Test
    public void testGetArtifactBasicHtml() throws Exception
    {
        // prepare: add an artifact
        final String artifactUri = this.loadTestArtifact("/test/artifacts/basic-2-internal-objects.rdf");
        
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER, artifactUri);
        
        final Representation results =
                RestletTestUtils.doTestAuthenticatedRequest(getArtifactClientResource, Method.GET, null,
                        MediaType.TEXT_HTML, Status.SUCCESS_OK, this.testWithAdminPrivileges);
        
        final String body = results.getText();
        
        // verify:
         System.out.println(body);
        Assert.assertTrue("Page does not identify Administrator", body.contains("Administrator"));
        Assert.assertFalse("Page contained a 404 error", body.contains("ERROR: 404"));
        
        Assert.assertTrue("Missing: Project Details", body.contains("Project Details"));
        Assert.assertTrue("Missng: ANZSRC FOR Code", body.contains("ANZSRC FOR Code:"));
        Assert.assertTrue("Missng: Project#2012...", body.contains("Project#2012-0006_ Cotton Leaf Morphology"));
        
        this.assertFreemarker(body);
    }

    /**
     * Test authenticated access to get an internal podd object in HTML
     */
    @Test
    public void testGetArtifactInternalObjectHtml() throws Exception
    {
        // prepare: add an artifact
        final String artifactUri = this.loadTestArtifact("/test/artifacts/basic-1.rdf");
        
        final String objectUri = "urn:poddinternal:7616392e-802b-4c5d-953d-bf81da5a98f4:0";
        
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER, artifactUri);
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_OBJECT_IDENTIFIER, objectUri);
        
        final Representation results =
                RestletTestUtils.doTestAuthenticatedRequest(getArtifactClientResource, Method.GET, null,
                        MediaType.TEXT_HTML, Status.SUCCESS_OK, this.testWithAdminPrivileges);
        
        final String body = results.getText();
        
        // verify:
        Assert.assertTrue("Page does not identify Administrator", body.contains("Administrator"));
        Assert.assertFalse("Page contained a 404 error", body.contains("ERROR: 404"));
        
        Assert.assertTrue("Missing: Analysis Details", body.contains("Analysis Details"));
        Assert.assertTrue("Missng title: poddScience#Analysis 0", body.contains("poddScience#Analysis 0"));
        
        this.assertFreemarker(body);
    }
    
    
    /**
     * Test authenticated access to get Artifact in RDF/XML
     */
    @Test
    public void testGetArtifactBasicRdf() throws Exception
    {
        // prepare: add an artifact
        final String artifactUri = this.loadTestArtifact("/test/artifacts/basicProject-1-internal-object.rdf");
        
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER, artifactUri);
        
        final Representation results =
                RestletTestUtils.doTestAuthenticatedRequest(getArtifactClientResource, Method.GET, null,
                        MediaType.APPLICATION_RDF_XML, Status.SUCCESS_OK, this.testWithAdminPrivileges);
        
        final String body = results.getText();
        
        // verify: received contents are in RDF
        Assert.assertTrue("Result does not have RDF", body.contains("<rdf:RDF"));
        Assert.assertTrue("Result does not have RDF", body.endsWith("</rdf:RDF>"));
        
        // verify: received contents have artifact URI
        Assert.assertTrue("Result does not contain artifact URI", body.contains(artifactUri));
    }
    
    /**
     * Test authenticated access to get Artifact in RDF/Turtle
     */
    @Test
    public void testGetArtifactBasicTurtle() throws Exception
    {
        // prepare: add an artifact
        final String artifactUri = this.loadTestArtifact("/test/artifacts/basicProject-1-internal-object.rdf");
        
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER, artifactUri);
        
        final Representation results =
                RestletTestUtils.doTestAuthenticatedRequest(getArtifactClientResource, Method.GET, null,
                        MediaType.APPLICATION_RDF_TURTLE, Status.SUCCESS_OK, this.testWithAdminPrivileges);
        
        final String body = results.getText();
        
        // verify: received contents are in Turtle
        Assert.assertTrue("Result does not have @prefix", body.contains("@prefix"));
        
        // verify: received contents have artifact's ontology and version IRIs
        Assert.assertTrue("Result does not contain artifact URI", body.contains(artifactUri));
    }
    
    /**
     * Test authenticated access to get Artifact in RDF/JSON
     */
    @Ignore
    @Test
    public void testGetArtifactBasicJson() throws Exception
    {
        // prepare: add an artifact
        final String artifactUri = this.loadTestArtifact("/test/artifacts/basicProject-1-internal-object.rdf");
        
        final ClientResource getArtifactClientResource =
                new ClientResource(this.getUrl(PoddWebConstants.PATH_ARTIFACT_GET_BASE));
        
        getArtifactClientResource.addQueryParameter(PoddWebConstants.KEY_ARTIFACT_IDENTIFIER, artifactUri);
        
        final Representation results =
                RestletTestUtils.doTestAuthenticatedRequest(getArtifactClientResource, Method.GET, null,
                        RestletUtilMediaType.APPLICATION_RDF_JSON, Status.SUCCESS_OK, this.testWithAdminPrivileges);
        
        final String body = results.getText();
        // System.out.println(body);
        
        // verify: received contents are in RDF/JSON
        // Assert.assertTrue("Result does not have @prefix", body.contains("@prefix"));
        
        // verify: received contents have artifact's ontology and version IRIs
        Assert.assertTrue("Result does not contain artifact URI", body.contains(artifactUri));
    }
    
}
