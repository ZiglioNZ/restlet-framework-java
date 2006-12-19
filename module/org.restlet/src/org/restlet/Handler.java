/*
 * Copyright 2005-2006 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package org.restlet;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.restlet.data.Method;
import org.restlet.data.ReferenceList;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Result;

/**
 * Restlet capable of handling calls using a target resource. At this point in
 * the processing, all the necessary information should be ready in order to
 * find the resource that is the actual target of the request and to handle the
 * required method on it.<br/> <br/> It is comparable to an HttpServlet class
 * as it provides facility methods to handle the most common method names. The
 * calls are then automatically dispatched to the appropriate handle*() method
 * (where the '*' character corresponds to the method name).<br/> <br/> The
 * handleGet(), handlePost(), handlePut(), handleDelete()n handleOptions() and
 * handleHead() methods have a default implementation in this class, but the
 * dispatching is dynamically done for all other methods. For example, if you
 * want to support a MOVE method for a WebDAV server, you just have to add a
 * handleMove(Resource, Request, Response) method in your subclass of Handler
 * and it will be automatically be used at runtime.<br/> <br/> If no matching
 * handle*() method is found, then a Status.CLIENT_ERROR_METHOD_NOT_ALLOWED is
 * returned.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public abstract class Handler extends Restlet {
    /** Indicates if the best content is automatically negotiated. */
    private boolean negotiateContent;

    /**
     * Constructor.
     */
    public Handler() {
        this(null);
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     */
    public Handler(Context context) {
        super(context);
        this.negotiateContent = true;
    }

    /**
     * Finds the target Resource if available. The default value is null, but
     * this method is intended to be overriden in subclasses.
     * 
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     * @return The target resource if available or null.
     */
    public abstract Resource findTarget(Request request, Response response);

    /**
     * Handles a call.
     * 
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    public void handle(Request request, Response response) {
        init(request, response);

        if (isStarted()) {
            Resource target = findTarget(request, response);

            if (target == null) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            } else {
                Method method = request.getMethod();

                if (method == null) {
                    response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                            "No method specified");
                } else {
                    if (!allowMethod(method, target)) {
                        response
                                .setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                        updateAllowedMethods(response, target);
                    } else {
                        if (method.equals(Method.GET)) {
                            handleGet(target, request, response);
                        } else if (method.equals(Method.HEAD)) {
                            handleHead(target, request, response);
                        } else if (method.equals(Method.POST)) {
                            handlePost(target, request, response);
                        } else if (method.equals(Method.PUT)) {
                            handlePut(target, request, response);
                        } else if (method.equals(Method.DELETE)) {
                            handleDelete(target, request, response);
                        } else if (method.equals(Method.OPTIONS)) {
                            handleOptions(target, request, response);
                        } else {
                            java.lang.reflect.Method handleMethod = getHandleMethod(method);
                            if (handleMethod != null) {
                                invoke(this, handleMethod, request, response);
                            } else {
                                response
                                        .setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Indicates if a method is allowed on a target resource.
     * 
     * @param method
     *            The method to test.
     * @param target
     *            The target resource.
     * @return True if a method is allowed on a target resource.
     */
    private boolean allowMethod(Method method, Resource target) {
        boolean result = false;

        if (target != null) {
            if (method.equals(Method.GET) || method.equals(Method.HEAD)) {
                result = target.allowGet();
            } else if (method.equals(Method.POST)) {
                result = target.allowPost();
            } else if (method.equals(Method.PUT)) {
                result = target.allowPut();
            } else if (method.equals(Method.DELETE)) {
                result = target.allowDelete();
            } else if (method.equals(Method.OPTIONS)) {
                result = true;
            } else {
                // Dynamically introspect the target resource to detect a
                // matching "allow" method.
                java.lang.reflect.Method allowMethod = getAllowMethod(method,
                        target);
                if (allowMethod != null) {
                    result = (Boolean) invoke(target, allowMethod);
                }
            }
        }

        return result;
    }

    /**
     * Updates the set of allowed methods on the response based on a target
     * resource.
     * 
     * @param response
     *            The response to update.
     * @param target
     *            The target resource.
     */
    private void updateAllowedMethods(Response response, Resource target) {
        Set<Method> allowedMethods = response.getAllowedMethods();
        for (java.lang.reflect.Method classMethod : target.getClass()
                .getMethods()) {
            if (classMethod.getName().startsWith("allow")
                    && (classMethod.getParameterTypes().length == 0)) {
                if ((Boolean) invoke(target, classMethod)) {
                    Method allowedMethod = Method.valueOf(classMethod.getName()
                            .substring(5));
                    allowedMethods.add(allowedMethod);
                }
            }
        }
    }

    /**
     * Returns the handle method matching the given method name.
     * 
     * @param method
     *            The method to match.
     * @return The handle method matching the given method name.
     */
    private java.lang.reflect.Method getHandleMethod(Method method) {
        return getMethod("handle", method, this, Request.class, Response.class);
    }

    /**
     * Returns the allow method matching the given method name.
     * 
     * @param method
     *            The method to match.
     * @param target
     *            The target resource.
     * @return The allow method matching the given method name.
     */
    private java.lang.reflect.Method getAllowMethod(Method method,
            Resource target) {
        return getMethod("allow", method, target);
    }

    /**
     * Returns the method matching the given prefix and method name.
     * 
     * @param prefix
     *            The method prefix to match (ex: "allow" or "handle").
     * @param method
     *            The method to match.
     * @return The method matching the given prefix and method name.
     */
    private java.lang.reflect.Method getMethod(String prefix, Method method,
            Object target, Class... classes) {
        java.lang.reflect.Method result = null;
        StringBuilder sb = new StringBuilder();
        String methodName = method.getName().toLowerCase();

        if ((methodName != null) && (methodName.length() > 0)) {
            sb.append("handle");
            sb.append(Character.toUpperCase(methodName.charAt(0)));
            sb.append(methodName.substring(1));
        }

        try {
            result = target.getClass().getMethod(sb.toString(), classes);
        } catch (SecurityException e) {
            getLogger().log(
                    Level.WARNING,
                    "Couldn't access the " + prefix + " method for \"" + method
                            + "\"", e);
        } catch (NoSuchMethodException e) {
            getLogger().log(
                    Level.WARNING,
                    "Couldn't find the " + prefix + " method for \"" + method
                            + "\"", e);
        }

        return result;
    }

    /**
     * Invokes a method with the given arguments.
     * 
     * @param target
     *            The target object.
     * @param method
     *            The method to invoke.
     * @param args
     *            The arguments to pass.
     * @return Invocation result.
     */
    private Object invoke(Object target, java.lang.reflect.Method method,
            Object... args) {
        Object result = null;

        if (method != null) {
            try {
                result = method.invoke(target, args);
            } catch (IllegalArgumentException e) {
                getLogger().log(
                        Level.WARNING,
                        "Couldn't invoke the handle method for \"" + method
                                + "\"", e);
            } catch (IllegalAccessException e) {
                getLogger().log(
                        Level.WARNING,
                        "Couldn't access the handle method for \"" + method
                                + "\"", e);
            } catch (InvocationTargetException e) {
                getLogger().log(
                        Level.WARNING,
                        "Couldn't invoke the handle method for \"" + method
                                + "\"", e);
            }
        }

        return result;
    }

    /**
     * Handles a DELETE call invoking the 'delete' method of the target resource
     * (as provided by the 'findTarget' method).
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handleDelete(Resource target, Request request,
            Response response) {
        Result result = target.delete();
        response.setStatus(result.getStatus());
        response.setRedirectRef(result.getRedirectRef());
        response.setEntity(result.getEntity());
    }

    /**
     * Handles a GET call by automatically returning the best entity available
     * from the target resource (as provided by the 'findTarget' method). The
     * content negotiation is based on the client's preferences available in the
     * handled call and can be turned off using the "negotiateContent" property.
     * If it is disabled and multiple variants are available for the target
     * resource, then a 300 (Multiple Choices) status will be returned with the
     * list of variants URI if available.
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handleGet(Resource target, Request request, Response response) {
        if (isNegotiateContent()) {
            response.setEntity(target);
        } else {
            List<Representation> variants = target.getVariants();
            if (variants.size() == 0) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            } else if (variants.size() == 1) {
                response.setEntity(variants.get(0));
            } else {
                ReferenceList variantRefs = new ReferenceList();

                for (Representation variant : variants) {
                    if (variant.getIdentifier() != null) {
                        variantRefs.add(variant.getIdentifier());
                    } else {
                        getLogger()
                                .warning(
                                        "A resource with multiple variants should provide and identifier for each variants when content negotiation is turned off");
                    }
                }

                if (variantRefs.size() > 0) {
                    // Return the list of variants
                    response.setStatus(Status.REDIRECTION_MULTIPLE_CHOICES);
                    response.setEntity(variantRefs.getTextRepresentation());
                } else {
                    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                }
            }
        }
    }

    /**
     * Handles a HEAD call, using a logic similat to the handleGet method.
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handleHead(Resource target, Request request,
            Response response) {
        handleGet(target, request, response);
    }

    /**
     * Handles an OPTIONS call introspecting the target resource (as provided by
     * the 'findTarget' method).
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handleOptions(Resource target, Request request,
            Response response) {
        // HTTP spec says that OPTIONS should return the list of allowed methods
        updateAllowedMethods(response, target);
        response.setStatus(Status.SUCCESS_OK);
    }

    /**
     * Handles a POST call invoking the 'post' method of the target resource (as
     * provided by the 'findTarget' method).
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handlePost(Resource target, Request request,
            Response response) {
        if (request.isEntityAvailable()) {
            Result result = target.post(request.getEntity());
            response.setStatus(result.getStatus());
            response.setRedirectRef(result.getRedirectRef());
            response.setEntity(result.getEntity());
        } else {
            response.setStatus(new Status(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing request entity"));
        }
    }

    /**
     * Handles a PUT call invoking the 'put' method of the target resource (as
     * provided by the 'findTarget' method).
     * 
     * @param target
     *            The target resource (never null).
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    protected void handlePut(Resource target, Request request, Response response) {
        if (request.isEntityAvailable()) {
            Result result = target.put(request.getEntity());
            response.setStatus(result.getStatus());
            response.setRedirectRef(result.getRedirectRef());
            response.setEntity(result.getEntity());

            // HTTP spec says that PUT may return the list of allowed methods
            updateAllowedMethods(response, target);
        } else {
            response.setStatus(new Status(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Missing request entity"));
        }
    }

    /**
     * Indicates if the best content is automatically negotiated. Default value
     * is true.
     * 
     * @return True if the best content is automatically negotiated.
     */
    public boolean isNegotiateContent() {
        return this.negotiateContent;
    }

    /**
     * Indicates if the best content is automatically negotiated. Default value
     * is true.
     * 
     * @param negotiateContent
     *            True if the best content is automatically negotiated.
     */
    public void setNegotiateContent(boolean negotiateContent) {
        this.negotiateContent = negotiateContent;
    }

}
