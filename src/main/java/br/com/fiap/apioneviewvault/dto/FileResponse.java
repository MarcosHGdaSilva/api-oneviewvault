package br.com.fiap.apioneviewvault.dto;

import java.util.UUID;

public class FileResponse {
    private UUID link;
    
    public FileResponse(UUID link){
        this.link = link;
    }

    public UUID getLink() {
        return link;
    }

    public void setLink(UUID link){
        this.link = link;
    }
}
