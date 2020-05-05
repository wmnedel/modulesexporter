package org.jahia.modules.model;

import java.io.Serializable;

/**    
 * Serialized class with information about jahia environments
 */
public class EnvironmentInfo implements Serializable {
    private static final long serialVersionUID = 29383204L;

    private boolean srcStartedOnly;

    public boolean isSrcStartedOnly() { return srcStartedOnly; }

    public void setSrcStartedOnly(boolean srcStartedOnly) { this.srcStartedOnly = srcStartedOnly; }
}
