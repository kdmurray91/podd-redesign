/**
 * 
 */
package com.github.podd.impl.purl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.api.purl.PoddPurlProcessor;
import com.github.podd.api.purl.PoddPurlReference;
import com.github.podd.exception.PurlProcessorNotHandledException;

/**
 * A simple permanent URI generator implementation.
 * <p/>
 * The conversion process replaces the temporary URI prefix with a prefix of the form
 * <code>{new_prefix}{unique-id}</code>, where {new_prefix} is the prefix assigned during construction
 * or the default prefix of <code>http://purl.org/podd/</code>, and {unique-id} is a random unique ID
 * internally generated by this generator.
 * 
 * @author kutila
 * 
 */
public class SimplePoddPurlProcessor implements PoddPurlProcessor
{
    public static final String DEFAULT_PREFIX = "http://example.org/purl/";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private Set<String> supportedTemporaryUriPrefixes = new HashSet<String>();
    
    private final String prefix;
    
    public SimplePoddPurlProcessor()
    {
        this(DEFAULT_PREFIX); 
    }
    
    public SimplePoddPurlProcessor(String prefix)
    {
        this.prefix = prefix;
    }
    
    @Override
    public void addTemporaryUriHandler(final String temporaryUriPrefix)
    {
        if(temporaryUriPrefix == null)
        {
            throw new NullPointerException("Temporary URI prefix cannot be NULL");
        }
        this.supportedTemporaryUriPrefixes.add(temporaryUriPrefix);
    }
    
    @Override
    public boolean canHandle(final URI inputUri)
    {
        // since this Purl generator cannot create new PURLs from scratch
        if(inputUri == null)
        {
            return false;
        }
        
        final String inputStr = inputUri.stringValue();
        for(final String tempPrefix : this.supportedTemporaryUriPrefixes)
        {
            if(inputStr.startsWith(tempPrefix))
            {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<String> getTemporaryUriHandlers()
    {
        return new ArrayList<String>(this.supportedTemporaryUriPrefixes);
    }
    
    @Override
    public PoddPurlReference handleTranslation(final URI inputUri) throws PurlProcessorNotHandledException
    {
        if(inputUri == null)
        {
            throw new NullPointerException("NULL URI cannot be handled by this Purl Processor");
        }
        
        String thePrefix = null;
        final String inputStr = inputUri.stringValue();
        for(final String tempPrefix : this.supportedTemporaryUriPrefixes)
        {
            if(inputStr.startsWith(tempPrefix))
            {
                thePrefix = tempPrefix;
                break;
            }
        }
        if(thePrefix == null)
        {
            throw new PurlProcessorNotHandledException(this, inputUri,
                    "The input URI cannot be handled by this Purl Processor");
        }
        
        // generate the PURL
        final StringBuilder b = new StringBuilder();
        b.append(this.prefix);
        b.append(UUID.randomUUID().toString());
        b.append("/");
        b.append(inputStr.substring(thePrefix.length()));
        
        final URI purl = ValueFactoryImpl.getInstance().createURI(b.toString());
        
        this.log.info("Generated PURL {}", purl);
        
        return new SimplePoddPurlReference(inputUri, purl);
    }
    
    @Override
    public void removeTemporaryUriHandler(final String temporaryUriPrefix)
    {
        this.supportedTemporaryUriPrefixes.remove(temporaryUriPrefix);
    }
}
