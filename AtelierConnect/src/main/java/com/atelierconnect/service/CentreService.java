package com.atelierconnect.service;

import com.atelierconnect.entity.Centre;

import java.util.List;

public interface CentreService {
    Centre create(Centre site);
    Centre getById(Long id);
    List<Centre> getAll();
    Centre update(Long id, Centre site);
    void delete(Long id);
}
