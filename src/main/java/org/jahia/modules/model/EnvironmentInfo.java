package org.jahia.modules.model;


import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**    
 * Serialized class with information about jahia environments
 */
public class EnvironmentInfo implements Serializable {
    private static final long serialVersionUID = 29383204L;
    private static final int DEFAULTPORT = 443;

    @NotEmpty(message = "Please enter the target instance URL")
    private String remoteHost;
    @NotEmpty(message = "Please enter the target instance tools user name")
    private String remoteToolsUser;
    @NotEmpty(message = "Please enter the target instance tools password")
    private String remoteToolsPwd;

    @NotEmpty(message = "Invalid httpScheme")
    private String remoteScheme = "https";

    @NotNull(message = "Port can't be empty")
    @Range(min = 1, max = 65535, message = "Invalid Port Number")
    private Integer remotePort = DEFAULTPORT;


    private boolean autoStart;

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }


    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteToolsUser() {
        return remoteToolsUser;
    }

    public void setRemoteToolsUser(String remoteToolsUser) {
        this.remoteToolsUser = remoteToolsUser;
    }

    public String getRemoteToolsPwd() {
        return remoteToolsPwd;
    }

    public void setRemoteToolsPwd(String remoteToolsPwd) {
        this.remoteToolsPwd = remoteToolsPwd;
    }


    public String getRemoteScheme() {
        return remoteScheme;
    }

    public void setRemoteScheme(String remoteScheme) {
        this.remoteScheme = remoteScheme;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }


    @Override
    public String toString() {
        return "EnvironmentInfo{" +
                "remoteHost='" + remoteHost + '\'' +
                "remotePort='" + remotePort + '\'' +
                ", remoteToolsUser='" + remoteToolsUser + '\'' +
                '}';
    }


}
