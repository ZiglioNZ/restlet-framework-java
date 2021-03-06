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

package org.restlet.ext.crypto;

import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.crypto.internal.AwsVerifier;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.LocalVerifier;
import org.restlet.security.Verifier;

/**
 * Authenticator supporting the {@link ChallengeScheme#HTTP_AWS_S3} scheme.
 * 
 * @author Jean-Philippe Steinmetz <caskater47@gmail.com>
 */
public class AwsAuthenticator extends ChallengeAuthenticator {
    /**
     * Creates a new HttpAwsS3Authenticator instance.
     * 
     * @param context
     *            The context
     * @param realm
     *            The authentication realm
     */
    public AwsAuthenticator(Context context, String realm) {
	this(context, false, realm);
    }

    /**
     * Creates a new HttpAwsS3Authenticator instance.
     * 
     * @param context
     *            The context
     * @param optional
     *            Indicates if the authentication success is optional
     * @param realm
     *            The authentication realm
     */
    public AwsAuthenticator(Context context, boolean optional,
	    String realm) {
	this(context, optional, realm, new AwsVerifier(null));
    }

    /**
     * Creates a new HttpAwsS3Authenticator instance.
     * 
     * @param context
     *            The context
     * @param optional
     *            Indicates if the authentication success is optional
     * @param realm
     *            The authentication realm
     * @param verifier
     */
    public AwsAuthenticator(Context context, boolean optional,
	    String realm, Verifier verifier) {
	super(context, optional, ChallengeScheme.HTTP_AWS_S3, realm, verifier);
    }

    /**
     * Returns the maximum age of a request, in milliseconds, before it is
     * considered stale.
     * <p>
     * A negative or zero value indicates no age restriction. The default value
     * is 15 minutes.
     */
    public long getMaxRequestAge() {
	return getVerifier().getMaxRequestAge();
    }

    /**
     * Sets the maximum age of a request, in milliseconds, before it is
     * considered stale.
     * <p>
     * A negative or zero value indicates no age restriction. The default value
     * is 15 minutes.
     */
    public void setMaxRequestAge(long value) {
	getVerifier().setMaxRequestAge(value);
    }

    @Override
    public AwsVerifier getVerifier() {
	return (AwsVerifier) super.getVerifier();
    }

    /**
     * Sets the internal verifier. In general you shouldn't replace it but
     * instead set the {@code wrappedVerifier} via the
     * {@link #setWrappedVerifier(LocalVerifier)} method.
     */
    @Override
    public void setVerifier(Verifier verifier) {
	if (!(verifier instanceof AwsVerifier))
	    throw new IllegalArgumentException();

	super.setVerifier(verifier);
    }

    /**
     * Returns the secret verifier that will be wrapped by the real verifier
     * supporting all the HTTP AWS verifications.
     * 
     * @return the local wrapped verifier
     */
    public LocalVerifier getWrappedVerifier() {
	return getVerifier().getWrappedVerifier();
    }

    /**
     * Sets the secret verifier that will be wrapped by the real verifier
     * supporting all the HTTP AWS verifications.
     * 
     * @param verifier
     *            The local verifier to wrap
     */
    public void setWrappedVerifier(LocalVerifier verifier) {
	getVerifier().setWrappedVerifier(verifier);
    }
}
