<#-- @ftlvariable name="baseUrl" type="java.lang.String" -->
<#-- @ftlvariable name="user" type="podd.model.user.User" -->

<div id="title_pane">
    <h3>Upload new Project</h3>
</div>

<div id="content_pane">
<#if user??>
    <#if artifact??>
    	<p>Project successfully uploaded.</p>
	    <div class="fieldset" id="upload">
			<div class="legend">Project Details</div>
			<ol> 
				<li><span class="bold">Ontology IRI: </span>${artifact.ontologyIRI}</li>
				<li><span class="bold">Version IRI: </span>${artifact.versionIRI}</li>
				<li><span class="bold">Inferred IRI: </span>${artifact.inferredOntologyIRI}</li>
			</div>
			<div id="buttonwrapper">
		    	<a href="${baseUrl}/artifact/base?artifacturi=${artifact.ontologyIRI}" class="padded">View Project</a>
		    	<a href="${baseUrl}/artifact/roles?artifacturi=${artifact.ontologyIRI?url}">Edit Participants</a>
		    </div>
		</div>
	<#else>    
	    <form name="f" enctype="multipart/form-data" action="${baseUrl}/artifact/new" method="POST">
	    <div class="fieldset" id="upload">
			<div class="legend">Upload new artifact</div>
			<ol> 
				<li> 
					<label for="user">Select file: </label> 
					<input id="user" class="medium" name="artifact_file" type="file"> 
				</li> 
	            <#if errorMessage?? && errorMessage?has_content>
	                <li><h4 class="errorMsg">${errorMessage}</h4></li>
	            </#if>
	            <#if message?? && message?has_content>
	                <li><h4>${message!""}</h4></li>
	            </#if>
	            <#if detailedMessage?? && detailedMessage?has_content>
	                <li><p>${detailedMessage}</p></li>
	            </#if>
	        </ol>
	    </div>
		
		<div id="buttonwrapper">
			<button type="submit">Upload</button>
		</div>
	    </form>	
   	</#if>
    
 
    
<#else>
    <p>Welcome to PODD, please <a href="${baseUrl}/loginpage">login</a>.</p>
</#if>
</div>  <!-- content pane -->