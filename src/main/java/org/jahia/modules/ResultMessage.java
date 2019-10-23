package org.jahia.modules;

import java.io.Serializable;

/**    
 * Serialized class to provide a report of the modules migration
 */
public class ResultMessage implements Serializable {

    String bundleName;
    String version;

    String bundleKey;
    String message;

    private static final long serialVersionUID = 1;

    /**     
     * Constructor from ResultMessage
     * @param bundleName    Name of the bundle
     * @param version       Bundle version
     * @param bundleKey     Bundle key
     * @param message       Custom message to be printed
     */
    public ResultMessage(String bundleName, String version, String bundleKey, String message) {
        this.bundleName = bundleName;
        this.version = version;
        this.bundleKey = bundleKey;
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

    public String getBundleKey() {
        return bundleKey;
    }

    public void setBundleKey(String bundleKey) {
        this.bundleKey = bundleKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
