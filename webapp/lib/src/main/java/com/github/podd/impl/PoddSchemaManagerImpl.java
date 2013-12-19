/**
 * PODD is an OWL ontology database used for scientific project management
 * 
 * Copyright (C) 2009-2013 The University Of Queensland
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.podd.impl;

import info.aduna.iteration.Iterations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.ModelException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.api.PoddOWLManager;
import com.github.podd.api.PoddRepositoryManager;
import com.github.podd.api.PoddSchemaManager;
import com.github.podd.api.PoddSesameManager;
import com.github.podd.exception.EmptyOntologyException;
import com.github.podd.exception.PoddException;
import com.github.podd.exception.PoddRuntimeException;
import com.github.podd.exception.SchemaManifestException;
import com.github.podd.exception.UnmanagedSchemaException;
import com.github.podd.exception.UnmanagedSchemaIRIException;
import com.github.podd.exception.UnmanagedSchemaOntologyIDException;
import com.github.podd.restlet.ApplicationUtils;
import com.github.podd.utils.InferredOWLOntologyID;
import com.github.podd.utils.OntologyUtils;
import com.github.podd.utils.PODD;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public class PoddSchemaManagerImpl implements PoddSchemaManager
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private PoddRepositoryManager repositoryManager;
    private PoddSesameManager sesameManager;
    private PoddOWLManager owlManager;
    
    /**
     * 
     */
    public PoddSchemaManagerImpl()
    {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void downloadSchemaOntology(final InferredOWLOntologyID ontologyId, final OutputStream outputStream,
            final RDFFormat format, final boolean includeInferred) throws UnmanagedSchemaException,
        RepositoryException, OpenRDFException
    {
        if(ontologyId.getOntologyIRI() == null || ontologyId.getVersionIRI() == null)
        {
            throw new PoddRuntimeException("Ontology IRI and Version IRI cannot be null");
        }
        
        final InferredOWLOntologyID schemaOntologyID = this.getSchemaOntologyID(ontologyId);
        
        if(includeInferred && schemaOntologyID.getInferredOntologyIRI() == null)
        {
            throw new PoddRuntimeException("Inferred Ontology IRI cannot be null");
        }
        
        List<URI> contexts;
        
        if(includeInferred)
        {
            contexts =
                    Arrays.asList(schemaOntologyID.getVersionIRI().toOpenRDFURI(), schemaOntologyID
                            .getInferredOntologyIRI().toOpenRDFURI());
        }
        else
        {
            contexts = Arrays.asList(schemaOntologyID.getVersionIRI().toOpenRDFURI());
        }
        
        RepositoryConnection connection = null;
        
        try
        {
            connection = this.repositoryManager.getManagementRepository().getConnection();
            
            final RepositoryResult<Statement> statements =
                    connection.getStatements(null, null, null, includeInferred, contexts.toArray(new Resource[] {}));
            final Model model = new LinkedHashModel(Iterations.asList(statements));
            final RepositoryResult<Namespace> namespaces = connection.getNamespaces();
            for(final Namespace nextNs : Iterations.asSet(namespaces))
            {
                model.setNamespace(nextNs);
            }
            Rio.write(model, Rio.createWriter(format, outputStream));
        }
        finally
        {
            if(connection != null)
            {
                connection.close();
            }
        }
    }
    
    @Override
    public Set<InferredOWLOntologyID> getCurrentSchemaOntologies() throws OpenRDFException
    {
        RepositoryConnection conn = null;
        
        try
        {
            conn = this.repositoryManager.getManagementRepository().getConnection();
            
            return this.sesameManager.getAllCurrentSchemaOntologyVersions(conn,
                    this.repositoryManager.getSchemaManagementGraph());
        }
        finally
        {
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public InferredOWLOntologyID getCurrentSchemaOntologyVersion(final IRI schemaOntologyIRI)
        throws UnmanagedSchemaIRIException, OpenRDFException
    {
        if(schemaOntologyIRI == null)
        {
            throw new UnmanagedSchemaIRIException(schemaOntologyIRI, "NULL is not a managed schema ontology");
        }
        
        RepositoryConnection conn = null;
        try
        {
            conn = this.repositoryManager.getManagementRepository().getConnection();
            conn.begin();
            
            return this.sesameManager.getCurrentSchemaVersion(schemaOntologyIRI, conn,
                    this.repositoryManager.getSchemaManagementGraph());
        }
        finally
        {
            if(conn != null && conn.isActive())
            {
                conn.rollback();
            }
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public Set<InferredOWLOntologyID> getSchemaOntologies() throws OpenRDFException
    {
        RepositoryConnection conn = null;
        
        try
        {
            conn = this.repositoryManager.getManagementRepository().getConnection();
            
            return this.sesameManager.getAllSchemaOntologyVersions(conn,
                    this.repositoryManager.getSchemaManagementGraph());
        }
        finally
        {
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public OWLOntology getSchemaOntology(final IRI schemaOntologyIRI) throws UnmanagedSchemaIRIException
    {
        throw new RuntimeException("TODO: Implement getSchemaOntology(IRI)");
    }
    
    @Override
    public OWLOntology getSchemaOntology(final OWLOntologyID schemaOntologyID)
        throws UnmanagedSchemaOntologyIDException
    {
        throw new RuntimeException("TODO: Implement getSchemaOntology(OWLOntologyID)");
    }
    
    @Override
    public InferredOWLOntologyID getSchemaOntologyID(final OWLOntologyID owlOntologyID)
        throws UnmanagedSchemaOntologyIDException, OpenRDFException
    {
        if(owlOntologyID == null)
        {
            throw new UnmanagedSchemaOntologyIDException(owlOntologyID, "NULL is not a managed schema ontology");
        }
        
        RepositoryConnection conn = null;
        try
        {
            conn = this.repositoryManager.getManagementRepository().getConnection();
            conn.begin();
            
            final InferredOWLOntologyID version =
                    this.sesameManager.getSchemaVersion(owlOntologyID.getVersionIRI(), conn,
                            this.repositoryManager.getSchemaManagementGraph());
            
            // Check that the ontology IRI matches or return an error
            if(version.getOntologyIRI().equals(owlOntologyID.getOntologyIRI()))
            {
                return version;
            }
            else
            {
                throw new UnmanagedSchemaOntologyIDException(owlOntologyID,
                        "Version did not match the ontology IRI specified for the ontology.");
            }
        }
        catch(final UnmanagedSchemaIRIException e)
        {
            throw new UnmanagedSchemaOntologyIDException(owlOntologyID,
                    "Could not find the version specified for the ontology.", e);
        }
        finally
        {
            if(conn != null && conn.isActive())
            {
                conn.rollback();
            }
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public InferredOWLOntologyID getSchemaOntologyVersion(final IRI schemaVersionIRI)
        throws UnmanagedSchemaIRIException, OpenRDFException
    {
        if(schemaVersionIRI == null)
        {
            throw new UnmanagedSchemaIRIException(schemaVersionIRI, "NULL is not a managed schema ontology");
        }
        
        RepositoryConnection conn = null;
        try
        {
            conn = this.repositoryManager.getManagementRepository().getConnection();
            conn.begin();
            
            return this.sesameManager.getSchemaVersion(schemaVersionIRI, conn,
                    this.repositoryManager.getSchemaManagementGraph());
        }
        finally
        {
            if(conn != null && conn.isActive())
            {
                conn.rollback();
            }
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public void setCurrentSchemaOntologyVersion(final OWLOntologyID schemaOntologyID)
        throws UnmanagedSchemaOntologyIDException, IllegalArgumentException
    {
        throw new RuntimeException("TODO: Implement setCurrentSchemaOntologyVersion");
    }
    
    @Override
    public void setOwlManager(final PoddOWLManager owlManager)
    {
        synchronized(this)
        {
            this.owlManager = owlManager;
        }
    }
    
    @Override
    public void setRepositoryManager(final PoddRepositoryManager repositoryManager)
    {
        this.repositoryManager = repositoryManager;
    }
    
    @Override
    public void setSesameManager(final PoddSesameManager sesameManager)
    {
        this.sesameManager = sesameManager;
    }
    
    @Override
    public List<InferredOWLOntologyID> uploadSchemaOntologies(final Model model) throws OpenRDFException, IOException,
        OWLException, PoddException
    {
        final Set<URI> schemaOntologyUris = new HashSet<>();
        final Set<URI> schemaVersionUris = new HashSet<>();
        
        OntologyUtils.extractOntologyAndVersions(model, schemaOntologyUris, schemaVersionUris);
        
        OntologyUtils.validateSchemaManifestImports(model, schemaVersionUris);
        
        final List<URI> importOrder = OntologyUtils.orderImports(model, schemaOntologyUris, schemaVersionUris);
        
        Map<URI, Set<OWLOntologyID>> allImports = OntologyUtils.getImports(model, schemaVersionUris);
        
        return this.uploadSchemaOntologiesInOrder(model, importOrder);
    }
    
    /**
     * Given the manifest {@link Model} and the overall order of imports based on ontology version
     * IRIs, import all of the ontologies which have new versions.
     * 
     * @param model
     * @param nextImportOrder
     * @throws ModelException
     * @throws OpenRDFException
     * @throws IOException
     * @throws OWLException
     * @throws PoddException
     */
    public List<InferredOWLOntologyID> uploadSchemaOntologiesInOrder(final Model model, final List<URI> nextImportOrder)
        throws ModelException, OpenRDFException, IOException, OWLException, PoddException
    {
        // Deduplicate the import list, preserving order for the first occurrences of the version
        // URIs.
        final List<URI> importOrder = new ArrayList<>(new LinkedHashSet<>(nextImportOrder));
        
        final Set<InferredOWLOntologyID> currentSchemaOntologies = this.getSchemaOntologies();
        
        for(final InferredOWLOntologyID nextCurrentSchemaOntology : currentSchemaOntologies)
        {
            final List<URI> tempList = new ArrayList<>(importOrder);
            for(int i = 0; i < tempList.size(); i++)
            {
                final URI nextImport = tempList.get(i);
                if(nextImport.equals(nextCurrentSchemaOntology.getVersionIRI().toOpenRDFURI()))
                {
                    // Do not reimport schema ontologies that we already have
                    importOrder.remove(i);
                }
            }
        }
        
        final List<InferredOWLOntologyID> result = new ArrayList<>(importOrder.size());
        Objects.requireNonNull(model, "Schema Ontology model was null");
        
        RepositoryConnection conn = null;
        
        try
        {
            // TODO: Should we store these copies in a separate repository
            // again, to reduce bloat in
            // the management repository??
            conn = this.repositoryManager.getManagementRepository().getConnection();
            conn.begin();
            
            for(final URI nextOrderedImport : importOrder)
            {
                final String classpathLocation =
                        model.filter(nextOrderedImport, PODD.PODD_SCHEMA_CLASSPATH, null).objectLiteral().stringValue();
                final RDFFormat fileFormat = Rio.getParserFormatForFileName(classpathLocation, RDFFormat.RDFXML);
                try (final InputStream inputStream = ApplicationUtils.class.getResourceAsStream(classpathLocation);)
                {
                    if(inputStream == null)
                    {
                        throw new SchemaManifestException(IRI.create(nextOrderedImport),
                                "Could not find schema at designated classpath location: "
                                        + nextOrderedImport.stringValue());
                    }
                    
                    // TODO: When this is supported we should fetch the current version if possible
                    // and use it here
                    final OWLOntologyID schemaOntologyID = null;
                    // TODO: Call this method directly from other methods so that the whole
                    // transaction can
                    // be rolled back if there are any failures!
                    final InferredOWLOntologyID nextResult =
                            this.uploadSchemaOntologyInternal(schemaOntologyID, inputStream, fileFormat, conn,
                                    this.repositoryManager.getSchemaManagementGraph());
                    
                    result.add(nextResult);
                }
            }
            
            conn.commit();
            
            return result;
        }
        catch(final Throwable e)
        {
            if(conn != null && conn.isActive())
            {
                conn.rollback();
            }
            
            throw e;
        }
        finally
        {
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
    }
    
    @Override
    public InferredOWLOntologyID uploadSchemaOntology(final InputStream inputStream, final RDFFormat fileFormat)
        throws OpenRDFException, IOException, OWLException, PoddException
    {
        return this.uploadSchemaOntology(null, inputStream, fileFormat);
    }
    
    /*
     * FIXME: check if the version exists in the repository before upload
     */
    @Override
    public InferredOWLOntologyID uploadSchemaOntology(final OWLOntologyID schemaOntologyID,
            final InputStream inputStream, final RDFFormat fileFormat) throws OpenRDFException, IOException,
        OWLException, PoddException
    {
        Objects.requireNonNull(inputStream, "Schema Ontology input stream was null");
        
        RepositoryConnection conn = null;
        
        try
        {
            // TODO: Should we store these copies in a separate repository
            // again, to reduce bloat in
            // the management repository??
            conn = this.repositoryManager.getManagementRepository().getConnection();
            conn.begin();
            
            // TODO: Call this method directly from other methods so that the whole transaction can
            // be rolled back if there are any failures!
            final InferredOWLOntologyID result =
                    this.uploadSchemaOntologyInternal(schemaOntologyID, inputStream, fileFormat, conn,
                            this.repositoryManager.getSchemaManagementGraph());
            
            conn.commit();
            
            return result;
        }
        catch(final Throwable e)
        {
            if(conn != null && conn.isActive())
            {
                conn.rollback();
            }
            
            throw e;
        }
        finally
        {
            if(conn != null && conn.isOpen())
            {
                conn.close();
            }
        }
        
    }
    
    /**
     * @param schemaOntologyID
     * @param inputStream
     * @param fileFormat
     * @param conn
     * @return
     * @throws OWLException
     * @throws IOException
     * @throws PoddException
     * @throws EmptyOntologyException
     * @throws RepositoryException
     * @throws OWLRuntimeException
     * @throws OpenRDFException
     */
    private InferredOWLOntologyID uploadSchemaOntologyInternal(final OWLOntologyID schemaOntologyID,
            final InputStream inputStream, final RDFFormat fileFormat, final RepositoryConnection conn,
            final URI schemaManagementGraph) throws OWLException, IOException, PoddException, EmptyOntologyException,
        RepositoryException, OWLRuntimeException, OpenRDFException
    {
        final OWLOntologyDocumentSource owlSource =
                new StreamDocumentSource(inputStream, fileFormat.getDefaultMIMEType());
        InferredOWLOntologyID nextInferredOntology = this.owlManager.loadAndInfer(owlSource, conn, schemaOntologyID);
        
        // update the link in the schema ontology management graph
        // TODO: This may not be the right method for this purpose
        this.sesameManager.updateCurrentManagedSchemaOntologyVersion(nextInferredOntology, true, conn,
                schemaManagementGraph);
        
        // TODO: Why are we not able to return nextInferredOntology here
        final InferredOWLOntologyID result =
                new InferredOWLOntologyID(nextInferredOntology.getOntologyIRI(), nextInferredOntology.getVersionIRI(),
                        nextInferredOntology.getOntologyIRI());
        return result;
    }
}
