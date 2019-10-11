package org.jahia.modules;

import java.io.Serializable;

public class ResultMessage implements Serializable {

    String bundleName;
    String version;

    String bundleKey;
    String message;

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
