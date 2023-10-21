package de.canchanchara.keycloak.storage.churchtools.model;

import java.util.List;

public class PersonListDto {

    private List<PersonDto> data;
    private MetaDto meta;

    public MetaDto getMeta() {
        return meta;
    }

    public void setMeta(MetaDto meta) {
        this.meta = meta;
    }

    public List<PersonDto> getData() {
        return data;
    }

    public void setData(List<PersonDto> data) {
        this.data = data;
    }
}
