package org.keycloak.quickstart.storage.user.churchtools.model;

import java.util.List;

public class SearchResultDto {

    private List<SearchResultDataDto> data;

    private MetaDto meta;

    public List<SearchResultDataDto> getData() {
        return data;
    }

    public void setData(List<SearchResultDataDto> data) {
        this.data = data;
    }

    public MetaDto getMeta() {
        return meta;
    }

    public void setMeta(MetaDto meta) {
        this.meta = meta;
    }
}
