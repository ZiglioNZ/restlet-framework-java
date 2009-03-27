/**
 * Copyright 2005-2009 Noelios Technologies.
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

package org.restlet.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.CookieSetting;
import org.restlet.data.Dimension;
import org.restlet.data.Language;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.representation.Representation;
import org.restlet.representation.RepresentationInfo;
import org.restlet.representation.Variant;
import org.restlet.util.Series;

/**
 * Server-side resource. TODO<br>
 * <br>
 * Concurrency note: contrary to the {@link org.restlet.Uniform} class and its
 * main {@link Restlet} subclass where a single instance can handle several
 * calls concurrently, one instance of {@link ServerResource} is created for
 * each call handled and accessed by only one thread at a time.
 * 
 * @author Jerome Louvel
 */
public class ServerResource extends UniformResource {

    /** Indicates if the identified resource exists. */
    private boolean exists;

    /** Indicates if conditional handling is enabled. */
    private boolean conditional;

    /** Indicates if content negotiation of response entities is enabled. */
    private boolean negotiated;

    /** The modifiable list of variants. */
    private volatile Map<Method, Object> variants;

    /**
     * Initializer block to ensure that the basic properties of the Resource are
     * initialized consistently across constructors.
     */
    {
        this.negotiated = true;
        this.variants = null;
    }

    /**
     * Special constructor used by IoC frameworks. Note that the
     * {@link #init(Context, Request, Response)}() method MUST be invoked right
     * after the creation of the handler in order to keep a behavior consistent
     * with the normal {@link #ServerResource(Context, Request, Response)}
     * constructor.
     */
    public ServerResource() {
    }

    /**
     * Normal constructor. This constructor will invoke the
     * {@link #init(Context, Request, Response)} method by default.
     * 
     * @param context
     *            The parent context.
     * @param request
     *            The request to handle.
     * @param response
     *            The response to return.
     */
    public ServerResource(Context context, Request request, Response response) {
        init(context, request, response);
    }

    /**
     * Deletes the resource and all its representations. The default behavior is
     * to set the response status to {@link Status#SERVER_ERROR_INTERNAL}.
     * 
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7">HTTP
     *      DELETE method</a>
     */
    public Representation delete() throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * Deletes the resource and all its representations. The default behavior is
     * to set the response status to {@link Status#SERVER_ERROR_INTERNAL}.
     * 
     * @param variant
     *            The variant of the response entity.
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7">HTTP
     *      DELETE method</a>
     */
    public Representation delete(Variant variant) throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * 
     * @param method
     * @return
     * @throws ResourceException
     */
    protected Representation doConditionalHandle(Method method)
            throws ResourceException {
        Representation result = null;

        if (!isExists() && getConditions().hasSome()
                && getConditions().getMatch().contains(Tag.ALL)) {
            setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED,
                    "A non existing resource can't match any tag.");
        } else {
            if (isNegotiated()) {
                result = doNegotiatedHandle(method);
            } else {
                result = doHandle(method);
            }

            if (result == null) {
                if ((getStatus() == null)
                        || (getStatus().isSuccess() && !Status.SUCCESS_NO_CONTENT
                                .equals(getStatus()))) {
                    setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                } else {
                    // Keep the current status as the developer might prefer a
                    // special status like 'method not authorized'.
                }
            } else {
                // The given representation (even if null) must meet the request
                // conditions (if any).
                if (getRequest().getConditions().hasSome()) {
                    final Status status = getRequest().getConditions()
                            .getStatus(getRequest().getMethod(), result);

                    if (status != null) {
                        setStatus(status);
                        result = null;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Handles a call without content negotiation of the response entity. The
     * default behavior is to dispatch the call to one of the {@link #get()},
     * {@link #post(Representation)}, {@link #put(Representation)},
     * {@link #delete()}, {@link #head()} or {@link #options()} methods.
     * 
     * @param method
     * @return
     * @throws ResourceException
     */
    protected Representation doHandle(Method method) throws ResourceException {
        Representation result = null;

        if (method == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "No method specified");
        } else {
            if (method.equals(Method.GET)) {
                result = get();
            } else if (method.equals(Method.POST)) {
                result = post(getRequest().getEntity());
            } else if (method.equals(Method.PUT)) {
                result = put(getRequest().getEntity());
            } else if (method.equals(Method.DELETE)) {
                result = delete();
            } else if (method.equals(Method.HEAD)) {
                result = head();
            } else if (method.equals(Method.OPTIONS)) {
                result = options();
            } else {
                setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                updateAllowedMethods();
            }
        }

        return result;
    }

    /**
     * Handles a call without content negotiation of the response entity. The
     * default behavior is to dispatch the call to one of the
     * {@link #get(Variant)}, {@link #post(Representation,Variant)},
     * {@link #put(Representation,Variant)}, {@link #delete(Variant)},
     * {@link #head(Variant)} or {@link #options(Variant)} methods.
     * 
     * @param method
     * @return
     * @throws ResourceException
     */
    protected Representation doHandle(Method method, Variant variant)
            throws ResourceException {
        Representation result = null;

        if (method == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "No method specified");
        } else {
            if (method.equals(Method.GET)) {
                result = get(variant);
            } else if (method.equals(Method.POST)) {
                result = post(getRequest().getEntity(), variant);
            } else if (method.equals(Method.PUT)) {
                result = put(getRequest().getEntity(), variant);
            } else if (method.equals(Method.DELETE)) {
                result = delete(variant);
            } else if (method.equals(Method.HEAD)) {
                result = head(variant);
            } else if (method.equals(Method.OPTIONS)) {
                result = options(variant);
            } else {
                setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                updateAllowedMethods();
            }
        }

        return result;
    }

    /**
     * 
     * @param method
     * @return
     * @throws ResourceException
     */
    protected Representation doNegotiatedHandle(Method method)
            throws ResourceException {
        Representation result = null;
        Variant preferredVariant = getPreferredVariant();

        if (preferredVariant == null) {
            // No variant was found matching the client preferences
            setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
            result = getAvailableVariants();
        } else {
            // Update the variant dimensions used for content negotiation
            updateDimensions();
            result = doHandle(method, preferredVariant);
        }

        return result;
    }

    /**
     * Represents the resource using content negotiation to select the best
     * variant based on the client preferences. By default it calls the
     * {@link #get(Variant)} method with the preferred variant returned by
     * {@link #getPreferredVariant()}.
     * 
     * @return The best representation.
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3">HTTP
     *      GET method</a>
     */
    public Representation get() throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * Returns a full representation for a given variant previously returned via
     * the getVariants() method. The default implementation directly returns the
     * variant in case the variants are already full representations. In all
     * other cases, you will need to override this method in order to provide
     * your own implementation. <br>
     * <br>
     * 
     * This method is very useful for content negotiation when it is too costly
     * to initialize all the potential representations. It allows a resource to
     * simply expose the available variants via the getVariants() method and to
     * actually server the one selected via this method.
     * 
     * @param variant
     *            The variant whose full representation must be returned.
     * @return The full representation for the variant.
     * @see #getVariants()
     */
    public Representation get(Variant variant) throws ResourceException {
        Representation result = null;

        if (variant instanceof Representation) {
            result = (Representation) variant;
        }

        return result;
    }

    @Override
    public Set<Method> getAllowedMethods() {
        return getResponse().getAllowedMethods();
    }

    protected Representation getAvailableVariants() {
        Representation result = null;

        // The list of all variants is transmitted to the client
        // final ReferenceList refs = new ReferenceList(variants.size());
        // for (final Variant variant : variants) {
        // if (variant.getIdentifier() != null) {
        // refs.add(variant.getIdentifier());
        // }
        // }
        //
        // result = refs.getTextRepresentation();
        return result;
    }

    /**
     * Returns information about the resource's representation. Those metadata
     * are important for conditional method processing. The advantage over the
     * complete {@link Representation} class is that it is much lighter to
     * create.<br>
     * <br>
     * By default, the {@link #get()} method is invoked.
     * 
     * @return Information about the best representation.
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3">HTTP
     *      GET method</a>
     */
    public RepresentationInfo getInfo() throws ResourceException {
        return get();
    }

    /**
     * Returns information about the resource's representation. Those metadata
     * are important for conditional method processing. The advantage over the
     * complete {@link Representation} class is that it is much lighter to
     * create.<br>
     * <br>
     * By default, the {@link #get(Variant)} method is invoked.
     * 
     * @param variant
     *            The variant whose representation information must be returned.
     * @return Information about the best representation.
     */
    public RepresentationInfo getInfo(Variant variant) throws ResourceException {
        return get(variant);
    }

    /**
     * Returns the preferred variant according to the client preferences
     * specified in the request.
     * 
     * @return The preferred variant.
     */
    public Variant getPreferredVariant() {
        Variant result = null;
        List<Variant> variants = (List<Variant>) getVariants().get(getMethod());

        if ((variants != null) && (!variants.isEmpty())) {
            Language language = null;
            // Compute the preferred variant. Get the default language
            // preference from the Application (if any).
            final Application app = Application.getCurrent();

            if (app != null) {
                language = app.getMetadataService().getDefaultLanguage();
            }

            result = getClientInfo().getPreferredVariant(variants, language);

        }

        return result;
    }

    /**
     * Returns the modifiable list of variants. Creates a new instance if no one
     * has been set. A variant can be a purely descriptive representation, with
     * no actual content that can be served. It can also be a full
     * representation in case a resource has only one variant or if the
     * initialization cost is very low.<br>
     * <br>
     * Note that the order in which the variants are inserted in the list
     * matters. For example, if the client has no preference defined, or if the
     * acceptable variants have the same quality level for the client, the first
     * acceptable variant in the list will be returned.<br>
     * <br>
     * It is recommended to not override this method and to simply use it at
     * construction time to initialize the list of available variants.
     * Overriding it may reconstruct the list for each call which can be
     * expensive.
     * 
     * @return The list of variants.
     * @see #getRepresentation(Variant)
     */
    public Map<Method, Object> getVariants() {
        // Lazy initialization with double-check.
        Map<Method, Object> v = this.variants;
        if (v == null) {
            synchronized (this) {
                v = this.variants;
                if (v == null) {
                    this.variants = v = new TreeMap<Method, Object>();
                }
            }
        }
        return v;
    }

    /**
     * Handles any call to this resource. The default implementation check the
     * {@link #isConditional()} and {@link #isNegotiated()} method to determine
     * which one of the {@link #doConditionalHandle()},
     * {@link #doNegotiatedHandle()} and {@link #doHandle()} methods should be
     * invoked. It also catches any {@link ResourceException} thrown and updates
     * the response status using the
     * {@link #setStatus(Status, Throwable, String)} method.
     */
    @Override
    public Representation handle() {
        Representation result = null;

        try {
            if (isConditional()) {
                result = doConditionalHandle(getMethod());
            } else if (isNegotiated()) {
                result = doNegotiatedHandle(getMethod());
            } else {
                result = doHandle(getMethod());
            }
        } catch (ResourceException re) {
            setStatus(re.getStatus(), re.getCause(), re.getLocalizedMessage());
        }

        return result;
    }

    /**
     * Handles the {@link Method#HEAD} uniform method. By default, it just
     * invokes {@link #get()}. The Restlet connector will use the result
     * representation to extract the metadata and not return the actual content
     * to the client.
     */
    public Representation head() throws ResourceException {
        return get();
    }

    public Representation head(Variant variant) throws ResourceException {
        return get(variant);
    }

    /**
     * Indicates if conditional handling is enabled.
     * 
     * @return True if conditional handling is enabled.
     */
    public boolean isConditional() {
        return conditional;
    }

    /**
     * Indicates if the identified resource exists.
     * 
     * @return True if the identified resource exists.
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * Indicates if the authenticated subject associated to the current request
     * is in the given role name.
     * 
     * @param roleName
     *            The role name to test.
     * @return True if the authenticated subject is in the given role.
     */
    public boolean isInRole(String roleName) {
        return getClientInfo().isInRole(getApplication().findRole(roleName));
    }

    /**
     * Indicates if content negotiation of response entities is enabled.
     * 
     * @return True if content negotiation of response entities is enabled.
     */
    public boolean isNegotiated() {
        return this.negotiated;
    }

    /**
     * Indicates the communication options available for this resource. The
     * default implementation is based on the HTTP specification which says that
     * OPTIONS should return the list of allowed methods in the Response
     * headers.
     * 
     * @return
     */
    public Representation options() throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * 
     * @param variant
     *            The variant of the response entity.
     * @return
     * @throws ResourceException
     */
    public Representation options(Variant variant) throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * Posts a representation to the resource at the target URI reference. The
     * default behavior is to set the response status to
     * {@link Status#SERVER_ERROR_INTERNAL}.
     * 
     * @param entity
     *            The posted entity.
     * @return The optional result entity.
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">HTTP
     *      POST method</a>
     */
    public Representation post(Representation entity) throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * Posts a representation to the resource at the target URI reference. The
     * default behavior is to set the response status to
     * {@link Status#SERVER_ERROR_INTERNAL}.
     * 
     * @param entity
     *            The posted entity.
     * @param variant
     *            The variant of the response entity.
     * @return The optional result entity.
     * @throws ResourceException
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">HTTP
     *      POST method</a>
     */
    public Representation post(Representation entity, Variant variant)
            throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    public Representation put(Representation representation)
            throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * 
     * @param representation
     * @param variant
     *            The variant of the response entity.
     * @return
     * @throws ResourceException
     */
    public Representation put(Representation representation, Variant variant)
            throws ResourceException {
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    /**
     * Sets the set of methods allowed on the requested resource. The set
     * instance set must be thread-safe (use {@link CopyOnWriteArraySet} for
     * example.
     * 
     * @param allowedMethods
     *            The set of methods allowed on the requested resource.
     * @see Response#setAllowedMethods(Set)
     */
    public void setAllowedMethods(Set<Method> allowedMethods) {
        getResponse().setAllowedMethods(allowedMethods);
    }

    /**
     * Sets the authentication request sent by an origin server to a client.
     * 
     * @param request
     *            The authentication request sent by an origin server to a
     *            client.
     * @see Response#setChallengeRequest(ChallengeRequest)
     */
    public void setChallengeRequest(ChallengeRequest request) {
        getResponse().setChallengeRequest(request);
    }

    /**
     * Sets the list of authentication requests sent by an origin server to a
     * client. The list instance set must be thread-safe (use
     * {@link CopyOnWriteArrayList} for example.
     * 
     * @param requests
     *            The list of authentication requests sent by an origin server
     *            to a client.
     * @see Response#setChallengeRequests(List)
     */
    public void setChallengeRequests(List<ChallengeRequest> requests) {
        getResponse().setChallengeRequests(requests);
    }

    /**
     * Indicates if conditional handling is enabled.
     * 
     * @param conditional
     *            True if conditional handling is enabled.
     */
    public void setConditional(boolean conditional) {
        this.conditional = conditional;
    }

    /**
     * Sets the cookie settings provided by the server.
     * 
     * @param cookieSettings
     *            The cookie settings provided by the server.
     * @see Response#setCookieSettings(Series)
     */
    public void setCookieSettings(Series<CookieSetting> cookieSettings) {
        getResponse().setCookieSettings(cookieSettings);
    }

    /**
     * Sets the set of dimensions on which the response entity may vary. The set
     * instance set must be thread-safe (use {@link CopyOnWriteArraySet} for
     * example.
     * 
     * @param dimensions
     *            The set of dimensions on which the response entity may vary.
     * @see Response#setDimensions(Set)
     */
    public void setDimensions(Set<Dimension> dimensions) {
        getResponse().setDimensions(dimensions);
    }

    /**
     * Indicates if the identified resource exists.
     * 
     * @param exists
     *            Indicates if the identified resource exists.
     */
    public void setExists(boolean exists) {
        this.exists = exists;
    }

    /**
     * Sets the reference that the client should follow for redirections or
     * resource creations.
     * 
     * @param locationRef
     *            The reference to set.
     * @see Response#setLocationRef(Reference)
     */
    public void setLocationRef(Reference locationRef) {
        getResponse().setLocationRef(locationRef);
    }

    /**
     * Sets the reference that the client should follow for redirections or
     * resource creations. If you pass a relative location URI, it will be
     * resolved with the current base reference of the request's resource
     * reference (see {@link Request#getResourceRef()} and
     * {@link Reference#getBaseRef()}.
     * 
     * @param locationUri
     *            The URI to set.
     * @see Response#setLocationRef(String)
     */
    public void setLocationRef(String locationUri) {
        getResponse().setLocationRef(locationUri);
    }

    /**
     * Indicates if content negotiation of response entities is enabled.
     * 
     * @param negotiateContent
     *            True if content negotiation of response entities is enabled.
     */
    public void setNegotiated(boolean negotiateContent) {
        this.negotiated = negotiateContent;
    }

    /**
     * Sets the server-specific information.
     * 
     * @param serverInfo
     *            The server-specific information.
     * @see Response#setServerInfo(ServerInfo)
     */
    public void setServerInfo(ServerInfo serverInfo) {
        getResponse().setServerInfo(serverInfo);
    }

    /**
     * Sets the status.
     * 
     * @param status
     *            The status to set.
     * @see Response#setStatus(Status)
     */
    public void setStatus(Status status) {
        getResponse().setStatus(status);
    }

    /**
     * Sets the status.
     * 
     * @param status
     *            The status to set.
     * @param message
     *            The status message.
     * @see Response#setStatus(Status, String)
     */
    public void setStatus(Status status, String message) {
        getResponse().setStatus(status, message);
    }

    /**
     * Sets the status.
     * 
     * @param status
     *            The status to set.
     * @param throwable
     *            The related error or exception.
     * @see Response#setStatus(Status, Throwable)
     */
    public void setStatus(Status status, Throwable throwable) {
        getResponse().setStatus(status, throwable);
    }

    /**
     * Sets the status.
     * 
     * @param status
     *            The status to set.
     * @param throwable
     *            The related error or exception.
     * @param message
     *            The status message.
     * @see Response#setStatus(Status, Throwable, String)
     */
    public void setStatus(Status status, Throwable throwable, String message) {
        getResponse().setStatus(status, throwable, message);
    }

    /**
     * Sets the modifiable list of variants.
     * 
     * @param variants
     *            The modifiable list of variants.
     */
    public void setVariants(Map<Method, Object> variants) {
        this.variants = variants;
    }

    /**
     * Invoked when the list of allowed methods needs to be updated. The
     * {@link #getAllowedMethods()} or the {@link #setAllowedMethods(Set)}
     * methods should be used. The default implementation does nothing.
     * 
     */
    protected void updateAllowedMethods() {
    }

    protected void updateDimensions() {
        getDimensions().clear();
        getDimensions().add(Dimension.CHARACTER_SET);
        getDimensions().add(Dimension.ENCODING);
        getDimensions().add(Dimension.LANGUAGE);
        getDimensions().add(Dimension.MEDIA_TYPE);

    }

}
