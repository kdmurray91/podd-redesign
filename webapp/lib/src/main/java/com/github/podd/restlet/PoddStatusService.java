/**
 * 
 */
package com.github.podd.restlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.representation.AppendableRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.utils.PoddRdfConstants;

import freemarker.template.Configuration;

/**
 * This status service is based on the standard Restlet guide for returning custom error pages.
 * 
 * Set this to Application via application.setStatusService().
 * 
 * TODO: create the templates used (i.e. PropertyUtils.PROPERTY_TEMPLATE...)
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 * copied from the OAS project (https://github.com/ansell/oas)
 */
public class PoddStatusService extends StatusService
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private Configuration freemarkerConfiguration;
    
    /**
     * 
     */
    public PoddStatusService(final Configuration freemarkerConfiguration)
    {
        super();
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
    
    /**
     * @param enabled
     */
    public PoddStatusService(final Configuration freemarkerConfiguration, final boolean enabled)
    {
        super(enabled);
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.service.StatusService#getRepresentation(org.restlet.data.Status,
     * org.restlet.Request, org.restlet.Response)
     */
    @Override
    public Representation getRepresentation(final Status status, final Request request, final Response response)
    {
        // identify Media Type to send response in
        MediaType preferredMediaType = MediaType.TEXT_PLAIN;
        float maxQuality = 0;
        final List<Preference<MediaType>> acceptedMediaTypes = request.getClientInfo().getAcceptedMediaTypes();
        
        for (Preference<MediaType> pref : acceptedMediaTypes)
        {
            float quality = pref.getQuality();
            if (quality > maxQuality)
            {
                preferredMediaType = pref.getMetadata(); 
                maxQuality = quality;
            }
        }
        
        Representation representation = null;
        if (MediaType.APPLICATION_RDF_XML.equals(preferredMediaType))
        {
            representation = this.getRepresentationRdf(status, request, response);
        }
        else //if (MediaType.TEXT_HTML.equals(preferredMediaType))
        {
            representation = this.getRepresentationHtml(status, request, response);
        }
 
        return representation;
    }
    
    /**
     * Returns an Error page representation in RDF/XML.
     * 
     */
    private Representation getRepresentationRdf(Status status, Request request, Response response)
    {
        final Model model = new LinkedHashModel();
        
        final URI errorUri =
                ValueFactoryImpl.getInstance().createURI("http://purl.org/podd/error#", UUID.randomUUID().toString());
        model.add(errorUri, PoddRdfConstants.HTTP_STATUS_CODE_VALUE,
                ValueFactoryImpl.getInstance().createLiteral(status.getCode()));
        model.add(errorUri, PoddRdfConstants.HTTP_REASON_PHRASE,
                ValueFactoryImpl.getInstance().createLiteral(status.getReasonPhrase()));
        
        if(status.getThrowable() != null && status.getThrowable().getMessage() != null)
        {
            model.add(errorUri, RDFS.LABEL,
                    ValueFactoryImpl.getInstance().createLiteral(status.getThrowable().getMessage()));
        }
        String errorDescription = status.getDescription();
        if(status.getThrowable() != null)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter writer = new PrintWriter(sw);
            status.getThrowable().printStackTrace(writer);
            errorDescription = sw.toString();
        }
        model.add(errorUri, RDFS.COMMENT, ValueFactoryImpl.getInstance().createLiteral(errorDescription));
        
        // get a String representation of the statements in the Model
        final StringWriter out = new StringWriter();
        final RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, out);
        try
        {
            // writer.handleNamespace("http", PoddRdfConstants.HTTP);
            // writer.handleNamespace("rdfs", RDFS.NAMESPACE);
            writer.startRDF();
            for(final Statement st : model)
            {
                writer.handleStatement(st);
            }
            writer.endRDF();
        }
        catch(OpenRDFException e)
        {
            this.log.error("Error writing RDF content in status service", e);
            // We're already trying to send back an error. So ignore this?
        }
        
        return new AppendableRepresentation(out.toString(), MediaType.APPLICATION_RDF_XML, Language.DEFAULT,
                CharacterSet.UTF_8);
    }

    /**
     * Returns an Error page representation in text/html.
     * 
     */
    private Representation getRepresentationHtml(final Status status, final Request request, final Response response)
    {
        final Map<String, Object> dataModel = RestletUtils.getBaseDataModel(request);
        
        dataModel.put("contentTemplate", "error.html.ftl");
        dataModel.put("pageTitle", "An error occurred : HTTP " + status.getCode());
        dataModel.put("error_code", Integer.toString(status.getCode()));
        
        final StringBuilder message = new StringBuilder();
        if(status.getDescription() != null)
        {
            message.append(status.getDescription());
        }
        if(status.getThrowable() != null && status.getThrowable().getMessage() != null)
        {
            message.append(" (");
            message.append(status.getThrowable().getMessage());
            message.append(")");
        }
        dataModel.put("message", message.toString());
        
        return RestletUtils.getHtmlRepresentation("poddBase.html.ftl", dataModel, MediaType.TEXT_HTML,
                this.freemarkerConfiguration);
    }
}
