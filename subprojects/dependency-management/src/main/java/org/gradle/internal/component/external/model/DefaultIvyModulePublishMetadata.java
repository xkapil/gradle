/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.component.external.model;

import com.google.common.collect.Lists;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.component.external.descriptor.ModuleDescriptorState;
import org.gradle.internal.component.external.descriptor.MutableModuleDescriptorState;
import org.gradle.internal.component.local.model.BuildableLocalComponentMetadata;
import org.gradle.internal.component.model.DefaultIvyArtifactName;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.Exclude;
import org.gradle.internal.component.model.IvyArtifactName;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultIvyModulePublishMetadata implements BuildableIvyModulePublishMetadata, BuildableLocalComponentMetadata {
    private final ModuleComponentIdentifier id;
    private final MutableModuleDescriptorState descriptor;
    private final Map<ModuleComponentArtifactIdentifier, IvyModuleArtifactPublishMetadata> artifactsById = new LinkedHashMap<ModuleComponentArtifactIdentifier, IvyModuleArtifactPublishMetadata>();

    public DefaultIvyModulePublishMetadata(ModuleComponentIdentifier id, String status) {
        this.id = id;
        this.descriptor = new MutableModuleDescriptorState(id, status, true);
    }

    public DefaultIvyModulePublishMetadata(ModuleComponentIdentifier id, ModuleDescriptorState moduleDescriptor) {
        this.id = id;
        this.descriptor = (MutableModuleDescriptorState) moduleDescriptor;
    }

    public ModuleComponentIdentifier getId() {
        return id;
    }

    public MutableModuleDescriptorState getModuleDescriptor() {
        return descriptor;
    }

    @Override
    public void addConfiguration(String name, String description, Set<String> extendsFrom, Set<String> hierarchy, boolean visible, boolean transitive, TaskDependency buildDependencies) {
        List<String> sortedExtends = Lists.newArrayList(extendsFrom);
        Collections.sort(sortedExtends);
        descriptor.addConfiguration(name, transitive, visible, sortedExtends);
    }

    @Override
    public void addExclude(Exclude exclude) {
        descriptor.addExclude(exclude);
    }

    @Override
    public void addDependency(DependencyMetadata dependency) {
        descriptor.addDependency(normalizeVersionForIvy(dependency));
    }

    /**
     * [1.0] is a valid version in maven, but not in Ivy: strip the surrounding '[' and ']' characters for ivy publish.
     */
    private static DependencyMetadata normalizeVersionForIvy(DependencyMetadata dependency) {
        String version = dependency.getRequested().getVersion();
        if (version.startsWith("[") && version.endsWith("]") && version.indexOf(',') == -1) {
            String normalizedVersion = version.substring(1, version.length() - 1);
            return dependency.withRequestedVersion(normalizedVersion);
        }
        return dependency;
    }

    @Override
    public void addArtifacts(String configuration, Iterable<? extends PublishArtifact> artifacts) {
        for (PublishArtifact artifact : artifacts) {
            DefaultIvyArtifactName ivyName = DefaultIvyArtifactName.forPublishArtifact(artifact);
            IvyModuleArtifactPublishMetadata ivyArtifact = getOrCreate(ivyName, artifact.getFile());
            ((DefaultIvyModuleArtifactPublishMetadata) ivyArtifact).addConfiguration(configuration);
        }
    }

    public void addArtifact(IvyArtifactName artifact, File file) {
        DefaultIvyModuleArtifactPublishMetadata publishMetadata = new DefaultIvyModuleArtifactPublishMetadata(id, artifact, file);
        artifactsById.put(publishMetadata.getId(), publishMetadata);
    }

    public void addArtifact(IvyModuleArtifactPublishMetadata artifact) {
        artifactsById.put(artifact.getId(), artifact);
    }

    private IvyModuleArtifactPublishMetadata getOrCreate(IvyArtifactName ivyName, File artifactFile) {
        for (IvyModuleArtifactPublishMetadata artifactPublishMetadata : artifactsById.values()) {
            if (artifactPublishMetadata.getArtifactName().equals(ivyName)) {
                return artifactPublishMetadata;
            }
        }
        DefaultIvyModuleArtifactPublishMetadata artifact = new DefaultIvyModuleArtifactPublishMetadata(id, ivyName, artifactFile);
        artifactsById.put(artifact.getId(), artifact);
        return artifact;
    }

    public Collection<IvyModuleArtifactPublishMetadata> getArtifacts() {
        return artifactsById.values();
    }

}
