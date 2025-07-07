package com.github.kiu345.eclipse.eclipseai.adapter;

import java.util.List;

import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;

public interface ChatAdapter {
    List<ModelDescriptor> getModels();
}
