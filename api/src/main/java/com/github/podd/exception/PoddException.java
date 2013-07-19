package com.github.podd.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDFS;

import com.github.podd.utils.PoddRdfConstants;

/**
 * This class extends <code>java.lang.Exception</code> to provide a PODD specific checked exception
 * base class.
 * 
 * This exception class is abstract and cannot be directly instantiated.
 * 
 * @author kutila
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class PoddException extends Exception
{
    private static final long serialVersionUID = -6240755031638346731L;
    
    public PoddException(final String msg)
    {
        super(msg);
    }
    
    public PoddException(final String msg, final Throwable throwable)
    {
        super(msg, throwable);
    }
    
    public PoddException(final Throwable throwable)
    {
        super(throwable);
    }
    
    /**
     * Retrieve details about this Exception instance as a {@link Model}. This method should be
     * overridden by sub-classes to provide more specific details.
     * 
     * @return A non-empty {@link Model} containing details about this Exception
     */
    public Model getDetailsAsModel()
    {
        final Model model = new LinkedHashModel();
        
        final BNode errorUri = PoddRdfConstants.VF.createBNode();
        model.add(errorUri, PoddRdfConstants.ERR_EXCEPTION_CLASS,
                PoddRdfConstants.VF.createLiteral(this.getClass().getName()));
        
        if(this.getMessage() != null)
        {
            model.add(errorUri, RDFS.LABEL, PoddRdfConstants.VF.createLiteral(this.getMessage()));
        }
        
        final StringWriter sw = new StringWriter();
        this.printStackTrace(new PrintWriter(sw));
        model.add(errorUri, RDFS.COMMENT, PoddRdfConstants.VF.createLiteral(sw.toString()));
        
        return model;
    }
}
