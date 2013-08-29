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
package com.github.podd.restlet;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import info.aduna.iteration.Iterations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.ModelException;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.restlet.Context;
import org.restlet.ext.crypto.DigestAuthenticator;
import org.restlet.ext.crypto.DigestVerifier;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.LocalVerifier;
import org.restlet.security.Realm;
import org.restlet.security.Role;
import org.restlet.security.User;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyManagerFactoryRegistry;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ansell.propertyutil.PropertyUtil;
import com.github.ansell.restletutils.FixedRedirectCookieAuthenticator;
import com.github.podd.api.PoddOWLManager;
import com.github.podd.api.PoddSesameManager;
import com.github.podd.api.file.DataReferenceManager;
import com.github.podd.api.file.DataReferenceProcessorFactory;
import com.github.podd.api.file.DataReferenceProcessorRegistry;
import com.github.podd.api.file.PoddDataRepositoryManager;
import com.github.podd.api.purl.PoddPurlManager;
import com.github.podd.api.purl.PoddPurlProcessorFactory;
import com.github.podd.api.purl.PoddPurlProcessorFactoryRegistry;
import com.github.podd.exception.PoddException;
import com.github.podd.impl.PoddArtifactManagerImpl;
import com.github.podd.impl.PoddOWLManagerImpl;
import com.github.podd.impl.PoddRepositoryManagerImpl;
import com.github.podd.impl.PoddSchemaManagerImpl;
import com.github.podd.impl.PoddSesameManagerImpl;
import com.github.podd.impl.file.FileReferenceManagerImpl;
import com.github.podd.impl.file.PoddFileRepositoryManagerImpl;
import com.github.podd.impl.file.SSHFileReferenceProcessorFactoryImpl;
import com.github.podd.impl.purl.PoddPurlManagerImpl;
import com.github.podd.impl.purl.UUIDPurlProcessorFactoryImpl;
import com.github.podd.utils.PoddRdfConstants;
import com.github.podd.utils.PoddRoles;
import com.github.podd.utils.PoddUser;
import com.github.podd.utils.PoddUserStatus;
import com.github.podd.utils.PoddWebConstants;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public class ApplicationUtils
{
    private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);
    
    /**
     * @param application
     * @param nextRepository
     * @throws RepositoryException
     * @throws RDFHandlerException
     */
    private static void dumpSchemaGraph(final PoddWebServiceApplication application, final Repository nextRepository)
        throws RepositoryException, RDFHandlerException
    {
        RepositoryConnection conn = null;
        
        try
        {
            conn = nextRepository.getConnection();
            
            Model model =
                    new LinkedHashModel(Iterations.asList(conn.getStatements(null, null, null, true, application
                            .getPoddRepositoryManager().getSchemaManagementGraph())));
            for(Namespace nextNamespace : Iterations.asSet(conn.getNamespaces()))
            {
                model.setNamespace(nextNamespace);
            }
            Rio.write(model, System.out, RDFFormat.TURTLE);
        }
        finally
        {
            if(conn != null)
            {
                conn.close();
            }
        }
    }
    
    public static ChallengeAuthenticator getNewAuthenticator(final Realm nextRealm, final Context newChildContext,
            final PropertyUtil propertyUtil)
    {
        ChallengeAuthenticator result = null;
        
        // FIXME: read from a property
        final String authMethod =
                propertyUtil.get(PoddWebConstants.PROPERTY_CHALLENGE_AUTH_METHOD,
                        PoddWebConstants.DEF_CHALLENGE_AUTH_METHOD);
        
        if(authMethod.equalsIgnoreCase("digest"))
        {
            ApplicationUtils.log.info("Using digest authenticator");
            // FIXME: Stub implementation
            result = new DigestAuthenticator(newChildContext, nextRealm.getName(), "s3cret");
            
            if(nextRealm.getVerifier() instanceof DigestVerifier)
            {
                // NOTE: The verifier in this case must support digest verification by being an
                // instance of DigestVerifier
                result.setVerifier(nextRealm.getVerifier());
            }
            else if(nextRealm.getVerifier() instanceof LocalVerifier)
            {
                // else we need to map the verifier in
                ((DigestAuthenticator)result).setWrappedVerifier((LocalVerifier)nextRealm.getVerifier());
            }
            else
            {
                throw new RuntimeException("Verifier was not valid for use with DigestAuthenticator verifier="
                        + nextRealm.getVerifier().toString());
            }
            
            result.setEnroler(nextRealm.getEnroler());
            
            result.setOptional(true);
            // Boolean.valueOf(PropertyUtil.getProperty(OasProperties.PROPERTY_CHALLENGE_AUTH_OPTIONAL,
            // OasProperties.DEFAULT_CHALLENGE_AUTH_OPTIONAL)));
        }
        else if(authMethod.equalsIgnoreCase("cookie"))
        {
            ApplicationUtils.log.info("Using cookie authenticator");
            
            // FIXME: Stub implementation
            final byte[] secretKey = "s3cr3t2345667123".getBytes(StandardCharsets.UTF_8);
            
            result = new FixedRedirectCookieAuthenticator(newChildContext, nextRealm.getName(), secretKey);
            
            ((FixedRedirectCookieAuthenticator)result).setLoginPath(PoddWebConstants.PATH_LOGIN_SUBMIT);
            ((FixedRedirectCookieAuthenticator)result).setLogoutPath(PoddWebConstants.PATH_LOGOUT);
            
            // FIXME: Make this configurable
            ((FixedRedirectCookieAuthenticator)result).setCookieName(PoddWebConstants.COOKIE_NAME);
            // FIXME: Make this configurable
            ((FixedRedirectCookieAuthenticator)result).setIdentifierFormName("username");
            // FIXME: Make this configurable
            ((FixedRedirectCookieAuthenticator)result).setSecretFormName("password");
            ((FixedRedirectCookieAuthenticator)result).setInterceptingLogin(true);
            ((FixedRedirectCookieAuthenticator)result).setInterceptingLogout(true);
            ((FixedRedirectCookieAuthenticator)result).setFixedRedirectUri(PoddWebConstants.PATH_REDIRECT_LOGGED_IN);
            
            result.setMultiAuthenticating(false);
            
            result.setVerifier(nextRealm.getVerifier());
            result.setEnroler(nextRealm.getEnroler());
            result.setOptional(true);
            
        }
        else if(authMethod.equalsIgnoreCase("http"))
        {
            // FIXME: Implement a stub here
            ApplicationUtils.log.error("FIXME: Implement HTTP ChallengeAuthenticator authMethod={}", authMethod);
            throw new RuntimeException("FIXME: Implement HTTP ChallengeAuthenticator");
        }
        else
        {
            ApplicationUtils.log.error("Did not recognise ChallengeAuthenticator method authMethod={}", authMethod);
            throw new RuntimeException("Did not recognise ChallengeAuthenticator method");
        }
        
        return result;
    }
    
    public static Repository getNewRepository() throws RepositoryException
    {
        // FIXME: Enable this before deploying
        final String repositoryUrl = ""; // PropertyUtil.getProperty(OasProperties.PROPERTY_SESAME_URL,
                                         // "");
        
        // if we weren't able to find a repository URL in the configuration, we setup an
        // in-memory store
        if(repositoryUrl.trim().isEmpty())
        {
            final Repository repository = new SailRepository(new MemoryStore());
            
            try
            {
                repository.initialize();
                
                ApplicationUtils.log.info("Created an in memory store as repository for PODD");
                
                return repository;
            }
            catch(final RepositoryException ex)
            {
                repository.shutDown();
                throw new RuntimeException("Could not initialise Sesame In Memory repository");
            }
        }
        else
        {
            final Repository repository = new HTTPRepository(repositoryUrl);
            
            try
            {
                repository.initialize();
                
                ApplicationUtils.log.info("Using sesame http repository as repository for PODD: {}", repositoryUrl);
                
                return repository;
            }
            catch(final RepositoryException ex)
            {
                repository.shutDown();
                throw new RuntimeException("Could not initialise Sesame HTTP repository with URL=" + repositoryUrl);
            }
        }
    }
    
    public static Configuration getNewTemplateConfiguration(final Context newChildContext)
    {
        final Configuration result = new Configuration();
        result.setDefaultEncoding("UTF-8");
        result.setURLEscapingCharset("UTF-8");
        
        // FIXME: Make this configurable
        result.setTemplateLoader(new ContextTemplateLoader(newChildContext, "clap://class/templates"));
        
        final BeansWrapper myWrapper = new BeansWrapper();
        myWrapper.setSimpleMapWrapper(true);
        result.setObjectWrapper(myWrapper);
        
        return result;
    }
    
    public static void setupApplication(final PoddWebServiceApplication application, final Context applicationContext)
        throws OpenRDFException
    {
        ApplicationUtils.log.debug("application {}", application);
        ApplicationUtils.log.debug("applicationContext {}", applicationContext);
        
        final List<Role> roles = application.getRoles();
        roles.clear();
        roles.addAll(PoddRoles.getRoles());
        
        final Repository nextRepository = ApplicationUtils.getNewRepository();
        
        application.setPoddRepositoryManager(new PoddRepositoryManagerImpl(nextRepository));
        application.getPoddRepositoryManager().setSchemaManagementGraph(PoddWebServiceApplicationImpl.SCHEMA_MGT_GRAPH);
        application.getPoddRepositoryManager().setArtifactManagementGraph(
                PoddWebServiceApplicationImpl.ARTIFACT_MGT_GRAPH);
        
        // File Reference manager
        final DataReferenceProcessorRegistry nextFileRegistry = new DataReferenceProcessorRegistry();
        // clear any automatically added entries that may come from META-INF/services entries on the
        // classpath
        nextFileRegistry.clear();
        final DataReferenceProcessorFactory nextFileProcessorFactory = new SSHFileReferenceProcessorFactoryImpl();
        nextFileRegistry.add(nextFileProcessorFactory);
        
        // File Reference Manager
        final DataReferenceManager nextDataReferenceManager = new FileReferenceManagerImpl();
        nextDataReferenceManager.setDataProcessorRegistry(nextFileRegistry);
        
        // PURL manager
        final PoddPurlProcessorFactoryRegistry nextPurlRegistry = new PoddPurlProcessorFactoryRegistry();
        nextPurlRegistry.clear();
        final PoddPurlProcessorFactory nextPurlProcessorFactory = new UUIDPurlProcessorFactoryImpl();
        
        final String purlPrefix = application.getPropertyUtil().get(PoddWebConstants.PROPERTY_PURL_PREFIX, null);
        ((UUIDPurlProcessorFactoryImpl)nextPurlProcessorFactory).setPrefix(purlPrefix);
        
        nextPurlRegistry.add(nextPurlProcessorFactory);
        
        final PoddPurlManager nextPurlManager = new PoddPurlManagerImpl();
        nextPurlManager.setPurlProcessorFactoryRegistry(nextPurlRegistry);
        
        final PoddOWLManager nextOWLManager = new PoddOWLManagerImpl();
        nextOWLManager.setReasonerFactory(OWLReasonerFactoryRegistry.getInstance().getReasonerFactory("Pellet"));
        final OWLOntologyManager nextOWLOntologyManager = OWLOntologyManagerFactoryRegistry.createOWLOntologyManager();
        if(nextOWLOntologyManager == null)
        {
            ApplicationUtils.log.error("OWLOntologyManager was null");
        }
        nextOWLManager.setOWLOntologyManager(nextOWLOntologyManager);
        
        // File Repository Manager
        final PoddDataRepositoryManager nextDataRepositoryManager = new PoddFileRepositoryManagerImpl();
        nextDataRepositoryManager.setRepositoryManager(application.getPoddRepositoryManager());
        nextDataRepositoryManager.setOWLManager(nextOWLManager);
        try
        {
            final Model aliasConfiguration = application.getAliasesConfiguration(application.getPropertyUtil());
            nextDataRepositoryManager.init(aliasConfiguration);
        }
        catch(PoddException | IOException e)
        {
            ApplicationUtils.log.error("Fatal Error!!! Could not initialize File Repository Manager", e);
        }
        
        application.setPoddDataRepositoryManager(nextDataRepositoryManager);
        
        final PoddSesameManager poddSesameManager = new PoddSesameManagerImpl();
        
        application.setPoddSchemaManager(new PoddSchemaManagerImpl());
        application.getPoddSchemaManager().setOwlManager(nextOWLManager);
        application.getPoddSchemaManager().setRepositoryManager(application.getPoddRepositoryManager());
        application.getPoddSchemaManager().setSesameManager(poddSesameManager);
        
        application.setPoddArtifactManager(new PoddArtifactManagerImpl());
        application.getPoddArtifactManager().setRepositoryManager(application.getPoddRepositoryManager());
        application.getPoddArtifactManager().setDataReferenceManager(nextDataReferenceManager);
        application.getPoddArtifactManager().setDataRepositoryManager(nextDataRepositoryManager);
        application.getPoddArtifactManager().setPurlManager(nextPurlManager);
        application.getPoddArtifactManager().setOwlManager(nextOWLManager);
        application.getPoddArtifactManager().setSchemaManager(application.getPoddSchemaManager());
        application.getPoddArtifactManager().setSesameManager(poddSesameManager);
        
        /*
         * Since the schema ontology upload feature is not yet supported, necessary schemas are
         * uploaded here at application starts up.
         */
        try
        {
            String schemaManifest =
                    application.getPropertyUtil().get(PoddRdfConstants.KEY_SCHEMAS,
                            PoddRdfConstants.PATH_DEFAULT_SCHEMAS);
            Model model = null;
            
            try (final InputStream schemaManifestStream = application.getClass().getResourceAsStream(schemaManifest);)
            {
                RDFFormat format = Rio.getParserFormatForFileName(schemaManifest, RDFFormat.RDFXML);
                model = Rio.parse(schemaManifestStream, "", format);
            }
            
            Set<URI> schemaOntologyUris = new HashSet<>();
            for(Resource nextOntology : model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects())
            {
                // Check to see if this is actually a version, in which case ignore it for now
                if(nextOntology instanceof URI && !model.contains(null, OWL.VERSIONIRI, nextOntology))
                {
                    schemaOntologyUris.add((URI)nextOntology);
                }
            }
            ConcurrentMap<URI, URI> currentVersionsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
            ConcurrentMap<URI, Set<URI>> allVersionsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
            ConcurrentMap<URI, Set<URI>> importsMap = new ConcurrentHashMap<>(schemaOntologyUris.size());
            
            List<URI> importOrder = new ArrayList<>(schemaOntologyUris.size());
            
            for(URI nextSchemaOntologyUri : schemaOntologyUris)
            {
                mapCurrentVersion(model, currentVersionsMap, nextSchemaOntologyUri);
            }
            
            for(URI nextSchemaOntologyUri : schemaOntologyUris)
            {
                mapAllVersions(model, currentVersionsMap, allVersionsMap, nextSchemaOntologyUri);
            }
            
            for(URI nextSchemaOntologyUri : schemaOntologyUris)
            {
                mapAndSortImports(model, currentVersionsMap, allVersionsMap, importsMap, importOrder,
                        nextSchemaOntologyUri);
            }
            
            log.info("importOrder: {}", importOrder);
            
            for(URI nextOrderedImport : importOrder)
            {
                String classpathLocation =
                        model.filter(nextOrderedImport, PoddRdfConstants.PODD_SCHEMA_CLASSPATH, null).objectLiteral()
                                .stringValue();
                RDFFormat format = Rio.getParserFormatForFileName(classpathLocation);
                try (final InputStream input = ApplicationUtils.class.getResourceAsStream(classpathLocation);)
                {
                    application.getPoddSchemaManager().uploadSchemaOntology(input, format);
                }
            }
            
            // TODO: Use a manifest file to load up the current versions here
            /*
             * application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_DCTERMS),
             * RDFFormat.RDFXML); application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_FOAF),
             * RDFFormat.RDFXML); application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_USER),
             * RDFFormat.RDFXML); application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_BASE),
             * RDFFormat.RDFXML); application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_SCIENCE),
             * RDFFormat.RDFXML); application.getPoddSchemaManager().uploadSchemaOntology(
             * ApplicationUtils.class.getResourceAsStream(PoddRdfConstants.PATH_PODD_PLANT),
             * RDFFormat.RDFXML);
             */
            // Enable the following for debugging
            // dumpSchemaGraph(application, nextRepository);
        }
        catch(IOException | OpenRDFException | OWLException | PoddException e)
        {
            ApplicationUtils.log.error("Fatal Error!!! Could not load schema ontologies", e);
        }
        
        // FIXME: Stub implementation in memory, based on the example restlet MemoryRealm class,
        // need to create a realm implementation that backs onto a database for persistence
        
        // OasMemoryRealm has extensions so that getClientInfo().getUser() will contain first name,
        // last name, and email address as necessary
        // FIXME: Restlet MemoryRealm creates a DefaultVerifier class that is not compatible with
        // DigestAuthenticator.setWrappedVerifier
        final PoddSesameRealmImpl nextRealm =
                new PoddSesameRealmImpl(nextRepository, PoddRdfConstants.DEF_USER_MANAGEMENT_GRAPH);
        
        // FIXME: Make this configurable
        nextRealm.setName("PODDRealm");
        
        final URI testAdminUserHomePage = PoddRdfConstants.VF.createURI("http://www.example.com/testAdmin");
        final PoddUser testAdminUser =
                new PoddUser("testAdminUser", "testAdminPassword".toCharArray(), "Test Admin", "User",
                        "test.admin.user@example.com", PoddUserStatus.ACTIVE, testAdminUserHomePage, "UQ",
                        "Orcid-Test-Admin");
        final URI testAdminUserUri = nextRealm.addUser(testAdminUser);
        nextRealm.map(testAdminUser, PoddRoles.ADMIN.getRole());
        
        final Set<Role> testAdminUserRoles = nextRealm.findRoles(testAdminUser);
        
        ApplicationUtils.log.debug("testAdminUserRoles: {}, {}", testAdminUserRoles, testAdminUserRoles.size());
        
        final User findUser = nextRealm.findUser("testAdminUser");
        
        ApplicationUtils.log.debug("findUser: {}", findUser);
        ApplicationUtils.log.debug("findUser.getFirstName: {}", findUser.getFirstName());
        ApplicationUtils.log.debug("findUser.getLastName: {}", findUser.getLastName());
        ApplicationUtils.log.debug("findUser.getName: {}", findUser.getName());
        ApplicationUtils.log.debug("findUser.getIdentifier: {}", findUser.getIdentifier());
        
        // TODO: Define groups here also
        
        // final MapVerifier verifier = new MapVerifier();
        // final ConcurrentHashMap<String, char[]> hardcodedLocalSecrets = new
        // ConcurrentHashMap<String, char[]>();
        // hardcodedLocalSecrets.put("testUser", "testPassword".toCharArray());
        // verifier.setLocalSecrets(hardcodedLocalSecrets);
        
        // final Context authenticatorChildContext = applicationContext.createChildContext();
        final ChallengeAuthenticator newAuthenticator =
                ApplicationUtils.getNewAuthenticator(nextRealm, applicationContext, application.getPropertyUtil());
        application.setAuthenticator(newAuthenticator);
        
        application.setRealm(nextRealm);
        
        // TODO: Is this necessary?
        // FIXME: Is this safe?
        // applicationContext.setDefaultVerifier(newAuthenticator.getVerifier());
        // applicationContext.setDefaultEnroler(newAuthenticator.getEnroler());
        
        // applicationContext.setDefaultVerifier(nextRealm.getVerifier());
        // applicationContext.setDefaultEnroler(nextRealm.getEnroler());
        
        // final Context templateChildContext = applicationContext.createChildContext();
        final Configuration newTemplateConfiguration = ApplicationUtils.getNewTemplateConfiguration(applicationContext);
        application.setTemplateConfiguration(newTemplateConfiguration);
        
        // create a custom error handler using our overridden PoddStatusService together with the
        // freemarker configuration
        final PoddStatusService statusService = new PoddStatusService(newTemplateConfiguration);
        application.setStatusService(statusService);
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param allVersionsMap
     * @param importsMap
     * @param importOrder
     * @param nextSchemaOntologyUri
     */
    private static void mapAndSortImports(Model model, ConcurrentMap<URI, URI> currentVersionsMap,
            ConcurrentMap<URI, Set<URI>> allVersionsMap, ConcurrentMap<URI, Set<URI>> importsMap,
            List<URI> importOrder, URI nextSchemaOntologyUri)
    {
        Set<Value> imports = model.filter(nextSchemaOntologyUri, OWL.IMPORTS, null).objects();
        Set<URI> nextImportsSet = new HashSet<>();
        Set<URI> putIfAbsent = importsMap.putIfAbsent(nextSchemaOntologyUri, nextImportsSet);
        if(putIfAbsent != null)
        {
            nextImportsSet = putIfAbsent;
        }
        int maxIndex = 0;
        if(imports.isEmpty())
        {
            if(!nextImportsSet.isEmpty())
            {
                log.error("Found inconsistent imports set: {} {}", nextSchemaOntologyUri, nextImportsSet);
            }
        }
        else
        {
            for(Value nextImport : imports)
            {
                if(nextImport instanceof URI)
                {
                    if(currentVersionsMap.containsKey(nextImport))
                    {
                        // Map down to the current version to ensure that we can load
                        // multiple versions simultaneously (if possible with the rest of
                        // the system)
                        nextImportsSet.add(currentVersionsMap.get(nextImport));
                    }
                    else
                    {
                        boolean foundAllVersion = false;
                        // Attempt to verify if the version exists
                        for(URI nextAllVersions : allVersionsMap.keySet())
                        {
                            if(nextAllVersions.equals(nextImport))
                            {
                                foundAllVersion = true;
                                // this should not normally occur, as the current versions
                                // map should also contain this key
                                nextImport = currentVersionsMap.get(nextAllVersions);
                                nextImportsSet.add((URI)nextImport);
                            }
                            else if(allVersionsMap.get(nextAllVersions).contains(nextImport))
                            {
                                nextImportsSet.add((URI)nextImport);
                                foundAllVersion = true;
                            }
                        }
                        
                        if(!foundAllVersion)
                        {
                            log.warn("Could not find import: {} imports {}", nextSchemaOntologyUri, nextImport);
                        }
                        else
                        {
                            nextImportsSet.add((URI)nextImport);
                        }
                    }
                    int nextIndex = importOrder.indexOf((URI)nextImport);
                    if(nextIndex >= maxIndex)
                    {
                        maxIndex = nextIndex + 1;
                    }
                }
            }
        }
        log.info("adding import for {} at {}", nextSchemaOntologyUri, maxIndex);
        // TODO: FIXME: This will not allow for multiple versions of a single schema
        // ontology at the same time
        importOrder.add(maxIndex, currentVersionsMap.get(nextSchemaOntologyUri));
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param allVersionsMap
     * @param nextSchemaOntologyUri
     */
    private static void mapAllVersions(Model model, ConcurrentMap<URI, URI> currentVersionsMap,
            ConcurrentMap<URI, Set<URI>> allVersionsMap, URI nextSchemaOntologyUri)
    {
        Set<Value> allVersions = model.filter(nextSchemaOntologyUri, OWL.VERSIONIRI, null).objects();
        Set<URI> nextAllVersions = new HashSet<>();
        Set<URI> putIfAbsent = allVersionsMap.putIfAbsent(nextSchemaOntologyUri, nextAllVersions);
        if(putIfAbsent != null)
        {
            nextAllVersions = putIfAbsent;
        }
        // If they specified a current version add it to the set
        if(currentVersionsMap.containsKey(nextSchemaOntologyUri))
        {
            nextAllVersions.add(currentVersionsMap.get(nextSchemaOntologyUri));
        }
        for(Value nextVersionURI : allVersions)
        {
            if(nextVersionURI instanceof URI)
            {
                nextAllVersions.add((URI)nextVersionURI);
            }
            else
            {
                ApplicationUtils.log.error("Version was not a URI: {} {}", nextSchemaOntologyUri, nextVersionURI);
            }
        }
        
        if(nextAllVersions.isEmpty())
        {
            ApplicationUtils.log.error("Could not find any version information for schema ontology: {}",
                    nextSchemaOntologyUri);
        }
    }
    
    /**
     * @param model
     * @param currentVersionsMap
     * @param nextSchemaOntologyUri
     */
    private static void mapCurrentVersion(Model model, ConcurrentMap<URI, URI> currentVersionsMap,
            URI nextSchemaOntologyUri)
    {
        try
        {
            URI nextCurrentVersionURI =
                    model.filter(nextSchemaOntologyUri, PoddRdfConstants.OMV_CURRENT_VERSION, null).objectURI();
            
            if(nextCurrentVersionURI == null)
            {
                ApplicationUtils.log.error("Did not find a current version for schema ontology: {}",
                        nextSchemaOntologyUri);
            }
            else
            {
                URI putIfAbsent = currentVersionsMap.putIfAbsent(nextSchemaOntologyUri, nextCurrentVersionURI);
                if(putIfAbsent != null)
                {
                    ApplicationUtils.log.error("Found multiple version URIs for schema ontology: {} old={} new={}",
                            nextSchemaOntologyUri, putIfAbsent, nextCurrentVersionURI);
                }
            }
        }
        catch(ModelException e)
        {
            ApplicationUtils.log.error("Could not find a single unique current version for schema ontology: {}",
                    nextSchemaOntologyUri);
        }
    }
    
    /**
     * Adds a Test User to the PODD Realm.
     * 
     * @param application
     */
    public static void setupTestUser(final PoddWebServiceApplication application)
    {
        final PoddSesameRealm nextRealm = application.getRealm();
        
        final URI testUserHomePage = PoddRdfConstants.VF.createURI("http://www.example.com/testUser");
        final PoddUser testUser =
                new PoddUser("anotherUser", "anotherPassword".toCharArray(), "Test", "User", "test.user@example.com",
                        PoddUserStatus.ACTIVE, testUserHomePage, "CSIRO", "Orcid-Test-User");
        final URI testUserUri = nextRealm.addUser(testUser);
        nextRealm.map(testUser, PoddRoles.PROJECT_CREATOR.getRole());
        nextRealm.map(testUser, PoddRoles.PROJECT_ADMIN.getRole(), PoddRdfConstants.TEST_ARTIFACT);
        
        ApplicationUtils.log.debug("Added Test User to PODD: {} <{}>", testUser.getIdentifier(), testUserUri);
    }
    
    private ApplicationUtils()
    {
    }
    
}
