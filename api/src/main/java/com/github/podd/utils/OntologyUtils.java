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
package com.github.podd.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.util.ModelException;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.podd.exception.SchemaManifestException;
import com.github.podd.exception.UnmanagedArtifactIRIException;
import com.github.podd.exception.UnmanagedArtifactVersionException;

/**
 * Utilities for working with {@link InferredOWLOntologyID}
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class OntologyUtils
{
    private static final Logger log = LoggerFactory.getLogger(OntologyUtils.class);
    
    /**
     * Extracts ontology and version IRIs to separate sets, which are both given as parameters.
     * 
     * Only recognises ontology IRIs which have an "ontologyIRI, rdf:type, owl:Ontology" triple.
     * 
     * Only recognises version IRIs which have both "versionIRI, rdf:type, owl:Ontology" and
     * "ontologyIRI, owl:versionIRI, versionIRI"
     * 
     * @param model
     *            The input statements.
     * @param schemaOntologyUris
     *            An output container for all of the ontology IRIs which are not also version IRIs.
     * @param schemaVersionUris
     *            An output container for all of the ontology IRIs which are also versionIRIs.
     */
    public static void extractOntologyAndVersions(final Model model, final Set<URI> schemaOntologyUris,
            final Set<URI> schemaVersionUris)
    {
        for(final Resource nextOntology : model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects())
        {
            if(nextOntology instanceof URI)
            {
                if(model.contains(null, OWL.VERSIONIRI, nextOntology))
                {
                    schemaVersionUris.add((URI)nextOntology);
                }
                else
                {
                    schemaOntologyUris.add((URI)nextOntology);
                }
            }
        }
    }
    
    /**
     * Retrieves imports specified using {@link OWL#IMPORTS}, based on the given version IRIs. Also
     * checks to verify that there is an {@link OWL#VERSIONIRI} statement for each of the version
     * IRIs. <br>
     * This works with the format used by both the schema manifests and the schema management graph.
     * 
     * @param model
     * @param schemaVersionUris
     * @return
     * @throws SchemaManifestException
     */
    public static Map<URI, Set<OWLOntologyID>> getSchemaManifestImports(final Model model,
            final Set<URI> schemaOntologyUris, final Set<URI> schemaVersionUris) throws SchemaManifestException
    {
        final ConcurrentMap<URI, Set<OWLOntologyID>> result = new ConcurrentHashMap<>();
        final ConcurrentMap<URI, Set<URI>> importsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
        
        if(schemaVersionUris.isEmpty())
        {
            log.info("No schema version URIs to get imports for");
        }
        
        for(final URI nextSchemaVersionUri : schemaVersionUris)
        {
            final Set<Resource> ontologies = model.filter(null, OWL.VERSIONIRI, nextSchemaVersionUri).subjects();
            if(ontologies.isEmpty())
            {
                throw new SchemaManifestException(IRI.create(nextSchemaVersionUri),
                        "No mapping from version IRI to an ontology IRI");
            }
            if(ontologies.size() > 1)
            {
                OntologyUtils.log.error("Found multiple mappings from version IRI to ontology IRI: {} {}",
                        nextSchemaVersionUri, ontologies);
                throw new SchemaManifestException(IRI.create(nextSchemaVersionUri),
                        "Non-unique mapping from version IRI to an ontology IRI");
            }
            final Resource uniqueOntology = ontologies.iterator().next();
            
            if(!(uniqueOntology instanceof URI))
            {
                OntologyUtils.log.error("Found non-URI mapping from version IRI to ontology IRI: {} {}",
                        nextSchemaVersionUri, ontologies);
                throw new SchemaManifestException(IRI.create(nextSchemaVersionUri),
                        "Non-URI mapping from version IRI to an ontology IRI");
            }
            
            final Set<Value> imports = model.filter(nextSchemaVersionUri, OWL.IMPORTS, null).objects();
            
            Set<OWLOntologyID> nextSet = new LinkedHashSet<>();
            final Set<OWLOntologyID> putIfAbsent = result.putIfAbsent(nextSchemaVersionUri, nextSet);
            if(putIfAbsent != null)
            {
                nextSet = putIfAbsent;
            }
            
            Set<URI> nextImports = new LinkedHashSet<>();
            
            for(final Value nextImport : imports)
            {
                if(!(nextImport instanceof URI))
                {
                    OntologyUtils.log.error("Found non-URI import for version IRI: {} {}", nextSchemaVersionUri,
                            nextImport);
                    throw new SchemaManifestException(IRI.create(nextSchemaVersionUri),
                            "Non-URI import for version IRI");
                }
                
                final URI nextImportVersionURI = (URI)nextImport;
                
                nextImports.add(nextImportVersionURI);
            }
            
            List<URI> orderImports =
                    OntologyUtils.orderImports(model, schemaOntologyUris, schemaVersionUris, importsMap);
            
            // Iterate through universally ordered collection to find ordered imports for this
            // ontology
            // TODO: This may not be the most efficient way to do this
            for(URI nextOrderedVersion : orderImports)
            {
                if(nextImports.contains(nextOrderedVersion))
                {
                    try
                    {
                        final URI nextImportOntologyURI =
                                GraphUtil.getUniqueSubjectURI(model, OWL.VERSIONIRI, nextOrderedVersion);
                        
                        nextSet.add(new OWLOntologyID(nextImportOntologyURI, nextOrderedVersion));
                    }
                    catch(final GraphUtilException e)
                    {
                        OntologyUtils.log.error("Found non-unique ontology IRI for imported version IRI: {}",
                                nextOrderedVersion);
                        throw new SchemaManifestException(IRI.create(nextOrderedVersion),
                                "Non-URI import for version IRI");
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param allVersionsMap
     * @param nextSchemaOntologyUri
     */
    public static void mapAllVersions(final Model model, final ConcurrentMap<URI, URI> currentVersionsMap,
            final ConcurrentMap<URI, Set<URI>> allVersionsMap, final URI nextSchemaOntologyUri)
    {
        final Set<Value> allVersions = model.filter(nextSchemaOntologyUri, OWL.VERSIONIRI, null).objects();
        Set<URI> nextAllVersions = new HashSet<>();
        final Set<URI> putIfAbsent = allVersionsMap.putIfAbsent(nextSchemaOntologyUri, nextAllVersions);
        if(putIfAbsent != null)
        {
            nextAllVersions = putIfAbsent;
        }
        // If they specified a current version add it to the set
        if(currentVersionsMap.containsKey(nextSchemaOntologyUri))
        {
            nextAllVersions.add(currentVersionsMap.get(nextSchemaOntologyUri));
        }
        for(final Value nextVersionURI : allVersions)
        {
            if(nextVersionURI instanceof URI)
            {
                nextAllVersions.add((URI)nextVersionURI);
            }
            else
            {
                OntologyUtils.log.error("Version was not a URI: {} {}", nextSchemaOntologyUri, nextVersionURI);
            }
        }
        
        if(nextAllVersions.isEmpty())
        {
            OntologyUtils.log.error("Could not find any version information for schema ontology: {}",
                    nextSchemaOntologyUri);
        }
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param allVersionsMap
     * @param importsMap
     * @param importOrder
     * @param nextOntologyUri
     */
    public static void mapAndSortImports(final Model model, final ConcurrentMap<URI, URI> currentVersionsMap,
            final ConcurrentMap<URI, Set<URI>> allVersionsMap, final ConcurrentMap<URI, Set<URI>> importsMap,
            final List<URI> importOrder, final URI nextOntologyUri)
    {
        final Set<Value> imports = model.filter(nextOntologyUri, OWL.IMPORTS, null).objects();
        Set<URI> nextImportsSet = new LinkedHashSet<>();
        final Set<URI> putIfAbsent = importsMap.putIfAbsent(nextOntologyUri, nextImportsSet);
        if(putIfAbsent != null)
        {
            nextImportsSet = putIfAbsent;
        }
        int maxIndex = 0;
        if(imports.isEmpty())
        {
            if(!nextImportsSet.isEmpty())
            {
                OntologyUtils.log.error("Found inconsistent imports set: {} {}", nextOntologyUri, nextImportsSet);
            }
        }
        else
        {
            for(final Value nextImport : imports)
            {
                if(nextImport instanceof URI)
                {
                    if(nextImportsSet.contains(nextImport))
                    {
                        // avoid duplicates
                        continue;
                    }
                    
                    if(currentVersionsMap.containsKey(nextImport))
                    {
                        // Map down to the current version to ensure that we can load multiple
                        // versions simultaneously (if possible with the rest of the system)
                        nextImportsSet.add(currentVersionsMap.get(nextImport));
                    }
                    else if(currentVersionsMap.containsValue(nextImport))
                    {
                        nextImportsSet.add((URI)nextImport);
                    }
                    else
                    {
                        boolean foundAllVersion = false;
                        // Attempt to verify if the version exists
                        for(final Entry<URI, Set<URI>> nextEntry : allVersionsMap.entrySet())
                        {
                            final URI nextAllVersions = nextEntry.getKey();
                            
                            if(nextAllVersions.equals(nextImport))
                            {
                                // TODO: Should we fail if we are importing ontologies without
                                // current versions, such as this case
                                if(nextEntry.getValue().isEmpty())
                                {
                                    nextImportsSet.add((URI)nextImport);
                                }
                                else
                                {
                                    // Randomly choose one, as the ontology does not have a current
                                    // version, but it does have some version information
                                    // TODO: Should we just use the import instead of randomly
                                    // choosing here
                                    nextImportsSet.add(nextEntry.getValue().iterator().next());
                                }
                                foundAllVersion = true;
                            }
                            else if(nextEntry.getValue().contains(nextImport))
                            {
                                nextImportsSet.add((URI)nextImport);
                                foundAllVersion = true;
                            }
                        }
                        
                        if(!foundAllVersion)
                        {
                            OntologyUtils.log.warn("Could not find import: {} imports {}", nextOntologyUri, nextImport);
                        }
                        else
                        {
                            nextImportsSet.add((URI)nextImport);
                        }
                    }
                    final int nextIndex = importOrder.indexOf(nextImport);
                    if(nextIndex >= maxIndex)
                    {
                        maxIndex = nextIndex + 1;
                    }
                }
            }
        }
        OntologyUtils.log.debug("adding import for {} at {}", nextOntologyUri, maxIndex);
        // TODO: FIXME: This will not allow for multiple versions of a single schema ontology at the
        // same time if they have any shared import versions
        importOrder.add(maxIndex, nextOntologyUri);
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param nextSchemaOntologyUri
     * @throws SchemaManifestException
     */
    public static void mapCurrentVersion(final Model model, final ConcurrentMap<URI, URI> currentVersionsMap,
            final URI nextSchemaOntologyUri) throws SchemaManifestException
    {
        try
        {
            final URI nextCurrentVersionURI =
                    model.filter(nextSchemaOntologyUri, PODD.OMV_CURRENT_VERSION, null).objectURI();
            
            if(nextCurrentVersionURI == null)
            {
                OntologyUtils.log
                        .error("Did not find a current version for schema ontology: {}", nextSchemaOntologyUri);
                // throw new SchemaManifestException(IRI.create(nextSchemaOntologyUri),
                // "Did not find a current version for schema ontology: " +
                // nextSchemaOntologyUri.stringValue());
            }
            else
            {
                final URI putIfAbsent = currentVersionsMap.putIfAbsent(nextSchemaOntologyUri, nextCurrentVersionURI);
                if(putIfAbsent != null)
                {
                    OntologyUtils.log.error("Found multiple version URIs for schema ontology: {} old={} new={}",
                            nextSchemaOntologyUri, putIfAbsent, nextCurrentVersionURI);
                    throw new SchemaManifestException(IRI.create(nextSchemaOntologyUri),
                            "Found multiple version IRIs for schema ontology");
                }
            }
        }
        catch(final ModelException e)
        {
            OntologyUtils.log.error("Could not find a single unique current version for schema ontology: {}",
                    nextSchemaOntologyUri);
            throw new SchemaManifestException(IRI.create(nextSchemaOntologyUri),
                    "Could not find a single unique current version IRI for schema ontology");
        }
    }
    
    /**
     * Extracts the {@link InferredOWLOntologyID} instances that are represented as RDF
     * {@link Statement}s in the given {@link Model}.
     * 
     * @param input
     *            The input model containing RDF statements.
     * @return A Collection of {@link InferredOWLOntologyID} instances derived from the statements
     *         in the model.
     */
    public static List<InferredOWLOntologyID> modelToOntologyIDs(final Model input)
    {
        return OntologyUtils.modelToOntologyIDs(input, false, true);
    }
    
    /**
     * Extracts the {@link InferredOWLOntologyID} instances that are represented as RDF
     * {@link Statement}s in the given {@link Model}.
     * 
     * @param input
     *            The input model containing RDF statements.
     * @param allowVersionless
     *            True if the algorithm should recognise versionless ontologies, and false to ignore
     *            them.
     * @return A Collection of {@link InferredOWLOntologyID} instances derived from the statements
     *         in the model.
     */
    public static List<InferredOWLOntologyID> modelToOntologyIDs(final Model input, final boolean allowVersionless,
            final boolean includeInferred)
    {
        final List<InferredOWLOntologyID> results = new ArrayList<InferredOWLOntologyID>();
        
        final Model typedOntologies = input.filter(null, RDF.TYPE, OWL.ONTOLOGY);
        
        for(final Statement nextTypeStatement : typedOntologies)
        {
            if(nextTypeStatement.getSubject() instanceof URI)
            {
                final Model versions = input.filter(nextTypeStatement.getSubject(), OWL.VERSIONIRI, null);
                
                if(versions.isEmpty())
                {
                    if(allowVersionless)
                    {
                        results.add(new InferredOWLOntologyID(IRI.create((URI)nextTypeStatement.getSubject()), null,
                                null));
                    }
                }
                else
                {
                    for(final Statement nextVersion : versions)
                    {
                        if(nextVersion.getObject() instanceof URI)
                        {
                            final Model inferredOntologies =
                                    input.filter((URI)nextVersion.getObject(), PODD.PODD_BASE_INFERRED_VERSION, null);
                            
                            if(!includeInferred)
                            {
                                results.add(new InferredOWLOntologyID((URI)nextTypeStatement.getSubject(),
                                        (URI)nextVersion.getObject(), null));
                            }
                            else
                            {
                                if(inferredOntologies.isEmpty())
                                {
                                    // If there were no poddBase#inferredVersion statements, backup
                                    // by
                                    // trying to infer the versions using owl:imports
                                    final Model importsOntologies =
                                            input.filter(null, OWL.IMPORTS, nextVersion.getObject());
                                    
                                    if(importsOntologies.isEmpty())
                                    {
                                        results.add(new InferredOWLOntologyID((URI)nextTypeStatement.getSubject(),
                                                (URI)nextVersion.getObject(), null));
                                    }
                                    else
                                    {
                                        for(final Statement nextImportOntology : importsOntologies)
                                        {
                                            if(nextImportOntology.getSubject() instanceof URI)
                                            {
                                                results.add(new InferredOWLOntologyID((URI)nextTypeStatement
                                                        .getSubject(), (URI)nextVersion.getObject(),
                                                        (URI)nextImportOntology.getSubject()));
                                            }
                                            else
                                            {
                                                OntologyUtils.log.error("Found a non-URI import statement: {}",
                                                        nextImportOntology);
                                            }
                                            
                                        }
                                    }
                                }
                                else
                                {
                                    for(final Statement nextInferredOntology : inferredOntologies)
                                    {
                                        if(nextInferredOntology.getObject() instanceof URI)
                                        {
                                            results.add(new InferredOWLOntologyID((URI)nextTypeStatement.getSubject(),
                                                    (URI)nextVersion.getObject(), (URI)nextInferredOntology.getObject()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Serialises the given collection of {@link InferredOWLOntologyID} objects to RDF, adding the
     * {@link Statement}s to the given {@link RDFHandler}.
     * <p>
     * This method wraps the serialisation from {@link InferredOWLOntologyID#toRDF()}.
     * 
     * @param input
     *            The collection of {@link InferredOWLOntologyID} objects to render to RDF.
     * @param handler
     *            The handler for handling the RDF statements.
     * @throws RDFHandlerException
     *             If there is an error while handling the statements.
     */
    public static void ontologyIDsToHandler(final Collection<InferredOWLOntologyID> input, final RDFHandler handler)
        throws RDFHandlerException
    {
        for(final InferredOWLOntologyID nextOntology : input)
        {
            for(final Statement nextStatement : nextOntology.toRDF())
            {
                handler.handleStatement(nextStatement);
            }
        }
    }
    
    /**
     * Serialises the given collection of {@link InferredOWLOntologyID} objects to RDF, adding the
     * {@link Statement}s to the given {@link Model}, or creating a new Model if the given model is
     * null.
     * <p>
     * This method wraps the serialisation from {@link InferredOWLOntologyID#toRDF()}.
     * 
     * @param input
     *            The collection of {@link InferredOWLOntologyID} objects to render to RDF.
     * @param result
     *            The Model to contain the resulting statements, or null to have one created
     *            internally
     * @param includeInferredOntologyStatements
     * @return A model containing the RDF statements about the given ontologies.
     * @throws RDFHandlerException
     *             If there is an error while handling the statements.
     */
    public static Model ontologyIDsToModel(final Collection<InferredOWLOntologyID> input, final Model result,
            final boolean includeInferredOntologyStatements)
    {
        Model results = result;
        
        if(results == null)
        {
            results = new LinkedHashModel();
        }
        
        for(final InferredOWLOntologyID nextOntology : input)
        {
            OntologyUtils.ontologyIDToRDF(nextOntology, results, includeInferredOntologyStatements);
        }
        
        return results;
    }
    
    public static Model ontologyIDsToModel(final Collection<InferredOWLOntologyID> input, final Model result)
    {
        return OntologyUtils.ontologyIDsToModel(input, result, true);
    }
    
    public static Model ontologyIDToRDF(final OWLOntologyID ontology, final Model result,
            final boolean includeInferredOntologyStatements)
    {
        final ValueFactory vf = ValueFactoryImpl.getInstance();
        
        if(ontology.getOntologyIRI() != null)
        {
            result.add(vf.createStatement(ontology.getOntologyIRI().toOpenRDFURI(), RDF.TYPE, OWL.ONTOLOGY));
            if(ontology.getVersionIRI() != null)
            {
                result.add(vf.createStatement(ontology.getVersionIRI().toOpenRDFURI(), RDF.TYPE, OWL.ONTOLOGY));
                result.add(vf.createStatement(ontology.getOntologyIRI().toOpenRDFURI(), OWL.VERSIONIRI, ontology
                        .getVersionIRI().toOpenRDFURI()));
                if(includeInferredOntologyStatements && ontology instanceof InferredOWLOntologyID)
                {
                    final InferredOWLOntologyID inferredOntology = (InferredOWLOntologyID)ontology;
                    if(inferredOntology.getInferredOntologyIRI() != null)
                    {
                        result.add(vf.createStatement(inferredOntology.getInferredOntologyIRI().toOpenRDFURI(),
                                RDF.TYPE, OWL.ONTOLOGY));
                        result.add(vf.createStatement(inferredOntology.getVersionIRI().toOpenRDFURI(),
                                PODD.PODD_BASE_INFERRED_VERSION, inferredOntology.getInferredOntologyIRI()
                                        .toOpenRDFURI()));
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Orders the schema ontology imports into list that can be uploaded in order to give a good
     * chance that dependencies will be uploaded first.
     * 
     * @param model
     * @param schemaOntologyUris
     * @param schemaVersionUris
     * @return An ordered list of {@link URI}s that determine a useful order for uploading schema
     *         ontologies to ensure that dependencies are available internally when needed.
     * @throws SchemaManifestException
     */
    public static List<URI> orderImports(final Model model, final Set<URI> schemaOntologyUris,
            final Set<URI> schemaVersionUris, final ConcurrentMap<URI, Set<URI>> importsMap)
        throws SchemaManifestException
    {
        final List<URI> importOrder = new ArrayList<>(schemaOntologyUris.size());
        
        final ConcurrentMap<URI, URI> currentVersionsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
        final ConcurrentMap<URI, Set<URI>> allVersionsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
        
        // Find current version for each schema ontology
        for(final URI nextSchemaOntologyUri : schemaOntologyUris)
        {
            OntologyUtils.mapCurrentVersion(model, currentVersionsMap, nextSchemaOntologyUri);
        }
        
        // Find all versions for each schema ontology
        for(final URI nextSchemaOntologyUri : schemaOntologyUris)
        {
            OntologyUtils.mapAllVersions(model, currentVersionsMap, allVersionsMap, nextSchemaOntologyUri);
        }
        
        // Map the actual schema ontologies to the correct order, based on
        // current versions and all versions with the imports taken into account
        for(final URI nextVersionUri : schemaVersionUris)
        {
            OntologyUtils.mapAndSortImports(model, currentVersionsMap, allVersionsMap, importsMap, importOrder,
                    nextVersionUri);
        }
        
        OntologyUtils.log.debug("importOrder: {}", importOrder);
        return importOrder;
    }
    
    /**
     * Extracts the {@link InferredOWLOntologyID} instances that are represented as RDF
     * {@link Statement}s in the given {@link String}.
     * 
     * @param string
     *            The input string containing RDF statements.
     * @param format
     *            The format of RDF statements in the string
     * @return A Collection of {@link InferredOWLOntologyID} instances derived from the statements
     *         in the string.
     * @throws OpenRDFException
     * @throws IOException
     */
    public static Collection<InferredOWLOntologyID> stringToOntologyID(final String string, final RDFFormat format)
        throws OpenRDFException, IOException
    {
        final Model model = Rio.parse(new StringReader(string), "", format);
        
        return OntologyUtils.modelToOntologyIDs(model);
    }
    
    /**
     * This method performs a consistency check between the ontology imports specified in the
     * schema-manifest with the actual imports specified within the ontology.
     * 
     * If the two sets of imports are not equal, a {@link SchemaManifestException} is thrown.
     * 
     * @param manifestModel
     *            Contents of the schema-manifest
     * @param schemaVersionUris
     *            Ontology Version IRIs to be loaded
     * @throws IOException
     * @throws RDFParseException
     * @throws UnsupportedRDFormatException
     * @throws SchemaManifestException
     *             If the imports from the two locations are not consistent
     */
    public static void validateSchemaManifestImports(final Model manifestModel, final Set<URI> schemaOntologyUris,
            final Set<URI> schemaVersionUris) throws IOException, RDFParseException, UnsupportedRDFormatException,
        SchemaManifestException
    {
        for(final URI nextOntologyUri : schemaOntologyUris)
        {
            if(manifestModel.contains(nextOntologyUri, OWL.IMPORTS, null))
            {
                OntologyUtils.log.error("Schema ontology in manifest has owl:imports coming directly from it: {}",
                        nextOntologyUri);
                throw new SchemaManifestException(IRI.create(nextOntologyUri),
                        "Schema ontology in manifest has owl:imports coming directly from it");
            }
        }
        
        for(final URI nextVersionUri : schemaVersionUris)
        {
            final Set<Value> importsInManifest = manifestModel.filter(nextVersionUri, OWL.IMPORTS, null).objects();
            
            final String classpathLocation =
                    manifestModel.filter(nextVersionUri, PODD.PODD_SCHEMA_CLASSPATH, null).objectLiteral()
                            .stringValue();
            final RDFFormat format = Rio.getParserFormatForFileName(classpathLocation, RDFFormat.RDFXML);
            try (final InputStream input = OntologyUtils.class.getResourceAsStream(classpathLocation);)
            {
                if(input == null)
                {
                    throw new SchemaManifestException(IRI.create(nextVersionUri),
                            "Could not find schema at designated classpath location: " + nextVersionUri.stringValue());
                }
                final Model model = Rio.parse(input, "", format);
                final Set<Value> importsInOwlFile = model.filter(null, OWL.IMPORTS, null).objects();
                
                OntologyUtils.log.debug("Comparing: \n Manifest: {} \n Owl:      {}", importsInManifest,
                        importsInOwlFile);
                if(!importsInManifest.equals(importsInOwlFile))
                {
                    throw new SchemaManifestException(IRI.create(nextVersionUri),
                            "Schema manifest imports not consistent with actual imports");
                }
            }
        }
    }
    
    /**
     * Finds the schema imports for the given artifact.
     * 
     * Must not throw an {@link UnmanagedArtifactIRIException} or
     * {@link UnmanagedArtifactVersionException} if the artifact does not exist globally, as long as
     * it is managed correctly in the given model.
     * 
     * @param artifactID
     * @param results
     * @param model
     * @throws OpenRDFException
     * @throws SchemaManifestException
     * @throws IOException
     * @throws RDFParseException
     * @throws UnsupportedRDFormatException
     */
    public static Set<OWLOntologyID> getArtifactImports(final InferredOWLOntologyID artifactID, final Model model)
        throws OpenRDFException, SchemaManifestException, IOException, RDFParseException, UnsupportedRDFormatException
    {
        Objects.requireNonNull(artifactID);
        Objects.requireNonNull(model);
        
        final Set<OWLOntologyID> results = new LinkedHashSet<OWLOntologyID>();
        
        final Set<URI> schemaOntologyUris = new HashSet<>();
        final Set<URI> schemaVersionUris = new HashSet<>();
        final ConcurrentMap<URI, Set<URI>> importsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
        
        OntologyUtils.extractOntologyAndVersions(model, schemaOntologyUris, schemaVersionUris);
        
        List<InferredOWLOntologyID> ontologyIDs = OntologyUtils.modelToOntologyIDs(model, true, false);
        
        if(schemaOntologyUris.contains(artifactID.getOntologyIRI().toOpenRDFURI()))
        {
            List<URI> importsForOneOntology = orderImports(model, schemaOntologyUris, schemaVersionUris, importsMap);
            
            importsForOneOntology.remove(artifactID.getOntologyIRI().toOpenRDFURI());
            if(artifactID.getVersionIRI() != null)
            {
                importsForOneOntology.remove(artifactID.getVersionIRI().toOpenRDFURI());
            }
            
            for(URI nextImport : importsForOneOntology)
            {
                if(importsMap.containsKey(nextImport))
                {
                    for(OWLOntologyID nextOntologyID : ontologyIDs)
                    {
                        if(nextOntologyID.getOntologyIRI().toOpenRDFURI().equals(nextImport))
                        {
                            results.add(nextOntologyID);
                            break;
                        }
                        else if(nextOntologyID.getVersionIRI() != null
                                && nextOntologyID.getVersionIRI().toOpenRDFURI().equals(nextImport))
                        {
                            results.add(nextOntologyID);
                            break;
                        }
                    }
                }
            }
        }
        else
        {
            throw new SchemaManifestException(artifactID.getOntologyIRI(),
                    "Did not find the given ontology IRI in the model");
        }
        
        return results;
    }
    
    private OntologyUtils()
    {
    }
}
