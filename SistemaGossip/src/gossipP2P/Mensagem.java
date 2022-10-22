package gossipP2P;

import java.io.Serializable;

public class Mensagem implements Serializable{
    public String messageType;

    public String originIpAddress;
    public int originPort;

    public String filename;

    public byte[] fileContent;

    // Generic
    public Mensagem() {};

    // SEARCH
    public Mensagem(String messageType, String originIpAddress, int originPort, String filename) {
        this.messageType = messageType;
        this.originIpAddress = originIpAddress;
        this.originPort = originPort;
        this.filename = filename;
    }

    // RESPONSE
    public Mensagem(String messageType, String originIpAddress, int originPort, String filename, byte[] fileContent) {
        this.messageType = messageType;
        this.originIpAddress = originIpAddress;
        this.originPort = originPort;
        this.filename = filename;
        this.fileContent = fileContent;
    }

    // Overriding equals() method to check if two Mensagem objects are equal
    public boolean equals(Mensagem m) {
        if (m == null)
            return false;
        else {
            return this.messageType.equals(m.messageType) &&
                    this.originIpAddress.equals(m.originIpAddress) &&
                    this.originPort == m.originPort &&
                    this.filename.equals(m.filename);
        }
    }
}

