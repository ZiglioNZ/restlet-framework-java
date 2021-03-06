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

package org.restlet.ext.oauth;

/**
 * A POJO representing a OAuth client_id. Each client can have collected a
 * number of authenticated users to allow working on their behalf.
 * 
 * Implementors should implement the storage and retrieval.
 * 
 * @author Kristoffer Gronowski
 */
public abstract class Client extends UserStore {

    /**
     * Client id that the client has registered at the auth provider.
     * 
     * @return the stored client id
     */
    public abstract String getClientId();

    /**
     * Client secret that the client has registered at the auth provider.
     * 
     * @return the stored client secret
     */

    public abstract String getClientSecret();

    /**
     * Redirect URL that the client has registered at the auth provider.
     * 
     * @return redirect callback url for code and token flows.
     */
    public abstract String getRedirectUri();

    /**
     * Human readable name of the application that this client represents It can
     * be useful for UI components to be presented.
     * 
     * @return name of the application.
     */
    public abstract String getApplicationName();
}
