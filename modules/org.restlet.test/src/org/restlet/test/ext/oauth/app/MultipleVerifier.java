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

package org.restlet.test.ext.oauth.app;

import java.util.Map;

import org.restlet.security.SecretVerifier;

/**
 * @author esvmart
 * 
 */
public class MultipleVerifier extends SecretVerifier {

    private final Map<String, String> users;

    public MultipleVerifier(Map<String, String> users) {
        this.users = users;
    }

    @Override
    public int verify(String identifier, char[] secret) {
        if (users == null)
            return RESULT_UNKNOWN;

        if (identifier == null || secret == null)
            return RESULT_UNKNOWN;

        String pass = users.get(identifier);

        if (pass == null)
            return RESULT_UNKNOWN;

        return compare(pass.toCharArray(), secret) ? RESULT_VALID
                : RESULT_INVALID;
    }

}
