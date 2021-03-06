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

package org.restlet.data;

import java.io.IOException;

import org.restlet.engine.util.SystemUtils;
import org.restlet.util.Couple;
import org.restlet.util.NamedValue;

/**
 * Multi-usage parameter. Note that the name and value properties are thread
 * safe, stored in volatile members.
 * 
 * @author Jerome Louvel
 */
@SuppressWarnings("deprecation")
public class Parameter extends Couple<String, String> implements
        Comparable<Parameter>, NamedValue<String> {

    /**
     * Creates a parameter.
     * 
     * @param name
     *            The parameter name buffer.
     * @param value
     *            The parameter value buffer (can be null).
     * @return The created parameter.
     * @throws IOException
     */
    public static Parameter create(CharSequence name, CharSequence value) {
        if (value != null) {
            return new Parameter(name.toString(), value.toString());
        } else {
            return new Parameter(name.toString(), null);
        }
    }

    /**
     * Default constructor.
     */
    public Parameter() {
        this(null, null);
    }

    /**
     * Preferred constructor.
     * 
     * @param name
     *            The name.
     * @param value
     *            The value.
     */
    public Parameter(String name, String value) {
        super(name, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.data.NamedValue#compareTo(org.restlet.data.Parameter)
     */
    public int compareTo(Parameter o) {
        return getName().compareTo(o.getName());
    }

    /**
     * Encodes the parameter into the target buffer.
     * 
     * @param buffer
     *            The target buffer.
     * @param characterSet
     *            The character set to use.
     * @throws IOException
     */
    public void encode(Appendable buffer, CharacterSet characterSet)
            throws IOException {
        if (getName() != null) {
            buffer.append(Reference.encode(getName(), characterSet));

            if (getValue() != null) {
                buffer.append('=');
                buffer.append(Reference.encode(getValue(), characterSet));
            }
        }
    }

    /**
     * Encodes the parameter as a string.
     * 
     * @param characterSet
     *            The character set to use.
     * @return The encoded string?
     * @throws IOException
     */
    public String encode(CharacterSet characterSet) throws IOException {
        StringBuilder sb = new StringBuilder();
        encode(sb, characterSet);
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        // if obj == this no need to go further
        boolean result = (obj == this);

        if (!result) {
            result = obj instanceof Parameter;

            // if obj isn't a parameter or is null don't evaluate further
            if (result) {
                Parameter that = (Parameter) obj;
                result = (((that.getName() == null) && (getName() == null)) || ((getName() != null) && getName()
                        .equals(that.getName())));

                // if names are both null or equal continue
                if (result) {
                    result = (((that.getValue() == null) && (getValue() == null)) || ((getValue() != null) && getValue()
                            .equals(that.getValue())));
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.data.NamedValue#getName()
     */
    public String getName() {
        return getFirst();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.data.NamedValue#getValue()
     */
    public String getValue() {
        return getSecond();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return SystemUtils.hashCode(getName(), getValue());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.data.NamedValue#setName(java.lang.String)
     */
    public void setName(String name) {
        setFirst(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.data.NamedValue#setValue(java.lang.String)
     */
    public void setValue(String value) {
        setSecond(value);
    }

    @Override
    public String toString() {
        return "[" + getName() + "=" + getValue() + "]";
    }

}
