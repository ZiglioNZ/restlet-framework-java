/**
 * Copyright 2005-2011 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.lucene.internal;

import java.util.Iterator;

import org.apache.solr.common.params.SolrParams;
import org.restlet.Request;
import org.restlet.data.Form;

/**
 * Wrap Restlet query parameters as Solr params.
 * 
 * @author Remi Dewitte <remi@gide.net>
 */
public class SolrRestletParams extends SolrParams {

    private static final long serialVersionUID = 1L;

    /** The wrapped Restlet request. */
    private final Request request;

    /**
     * Constructor.
     * 
     * @param request
     *            The wrapped Restlet request.
     */
    public SolrRestletParams(Request request) {
        this.request = request;
    }

    /**
     * Returns the request query form.
     * 
     * @return The request query form.
     */
    protected Form getForm() {
        return request.getResourceRef().getQueryAsForm();
    }

    /**
     * Reads parameter from the form returned {@link #getForm()}.
     * 
     */
    @Override
    public String get(String param) {
        return getForm().getFirstValue(param);
    }

    /**
     * Reads parameter names from the form returned {@link #getForm()}.
     * 
     */
    @Override
    public Iterator<String> getParameterNamesIterator() {
        return getForm().getNames().iterator();
    }

    /**
     * Reads parameter values from the form returned {@link #getForm()}.
     * 
     */
    @Override
    public String[] getParams(String param) {
        return getForm().getValuesArray(param);
    }

}