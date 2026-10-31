package com.padell.padell.service;

import com.padell.padell.entity.Centre;

import java.util.List;

public interface SiteService {
    Centre create(Centre site);
    Centre getById(Long id);
    List<Centre> getAll();
    Centre update(Long id, Centre site);
    void delete(Long id);
}
