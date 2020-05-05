package org.jahia.modules;

import java.io.Serializable;
import java.util.List;

public class ResultMessage implements Serializable {

    String bundleName;
    String version;
    String message;

    private static final long serialVersionUID = 1;

    /**     
     * Constructor for ResultMessage
     * @param bundleName    Name of the bundle
     * @param version       Bundle version
     * @param message       Custom message to be printed
     */
    public ResultMessage(String bundleName, String version, String message) {
        this.bundleName = bundleName;
        this.version = version;
        this.message = message;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
