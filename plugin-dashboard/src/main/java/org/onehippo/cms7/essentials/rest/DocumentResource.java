/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.model.DocumentRestful;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @version "$Id$"
 */
@Api(value = "/documents", description = "Rest resource which provides information and actions for document types")
@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documents/")
public class DocumentResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentResource.class);

    @ApiOperation(
            value = "Fetches all project document types (including compounds)",
            response = RestfulList.class)
    @GET
    @Path("/")
    public List<DocumentRestful> getAllTypes(@Context ServletContext servletContext) {
        return fetchDocuments(servletContext, Type.ALL);
    }

    @ApiOperation(
            value = "Fetches all project document types (compounds are *excluded*)",
            response = RestfulList.class)
    @GET
    @Path("/documents")
    public List<DocumentRestful> getDocumentTypes(@Context ServletContext servletContext) {
        return fetchDocuments(servletContext, Type.DOCUMENT);
    }

    @ApiOperation(
            value = "Fetches all project compound types",
            response = RestfulList.class)
    @GET
    @Path("/compounds")
    public List<DocumentRestful> getCompounds(@Context ServletContext servletContext) {
        return fetchDocuments(servletContext, Type.COMPOUND);
    }

    @ApiOperation(
            value = "Returns all documents of the specified type",
            notes = "Specify the document type as {namespace}:{typename}.",
            response = PluginRestful.class)
    @ApiParam(name = "docType", value = "Document type", required = true)
    @GET
    @Path("/{docType}")
    public List<KeyValueRestful> getDocumentsByType(@Context ServletContext servletContext, @PathParam("docType") String docType) {
        final List<KeyValueRestful> valueLists = new ArrayList<>();
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();

        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query xpath = queryManager.createQuery("//content/documents//element(*, " + docType + ")", "xpath");
            final NodeIterator nodes = xpath.execute().getNodes();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final String path = node.getPath();
                valueLists.add(new KeyValueRestful(((HippoNode) node).getLocalizedName(), path));
            }
        } catch (RepositoryException e) {
            log.debug("Error fetching value lists", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return valueLists;
    }

    //*************************************************************************************
    // UTILS
    //*************************************************************************************
    private List<DocumentRestful> fetchDocuments(final ServletContext servletContext, final Type type) {
        final PluginContext context = getContext(servletContext);
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Session session = context.createSession();
        final Collection<ContentType> projectContentTypes = new HashSet<>();
        final List<DocumentRestful> documents = new ArrayList<>();
        try {
            final ContentTypeService service = new HippoContentTypeService(session);
            final ContentTypes contentTypes = service.getContentTypes();
            final SortedMap<String, Set<ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();
            for (Map.Entry<String, Set<ContentType>> entry : typesByPrefix.entrySet()) {
                final String key = entry.getKey();
                final Set<ContentType> value = entry.getValue();
                if (key.equals(namespacePrefix)) {
                    projectContentTypes.addAll(value);
                }
            }
            // add extra properties (field locations)
            for (ContentType doc : projectContentTypes) {
                Type myType = doc.isCompoundType() ? Type.COMPOUND : Type.DOCUMENT;

                if (type == Type.ALL) {
                    documents.add(new DocumentRestful(doc));
                } else if (myType == type) {
                    documents.add(new DocumentRestful(doc));
                }
            }
            for (DocumentRestful document : documents) {
                final String path = MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_/root", namespacePrefix, document.getName());
                if(session.nodeExists(path)){
                    final Node node = session.getNode(path);
                    final Set<String> locations = new HashSet<>();
                    if(node.hasProperty("wicket.extensions")){
                        final Value[] values = node.getProperty("wicket.extensions").getValues();
                        for (Value value : values) {
                            final String propVal = value.getString();
                            if(node.hasProperty(propVal)){
                                locations.add(node.getProperty(propVal).getString());
                            }
                        }
                    }
                    if(locations.isEmpty()){
                        locations.add("${cluster.id}.field");
                    }
                    document.setFieldLocations(locations);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error fetching document types", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        // sort documents by name:
        Collections.sort(documents, new DocumentNameComparator());
        return documents;
    }

    public enum Type{
        ALL,
        COMPOUND,
        DOCUMENT,
        ASSET,
        GALLERY
    }
    private static class DocumentNameComparator implements java.util.Comparator<DocumentRestful> {
        @Override
        public int compare(final DocumentRestful first, final DocumentRestful second) {
            String name1 = first.getName();
            String name2 = second.getName();
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        }
    }

}
