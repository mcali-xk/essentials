/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.List;

/**
 * @version "$Id$"
 */
public interface Document {


    List<String> getProperties();

    void setProperties(List<String> properties);

    void addProperty(String value);

    /**
     * Name of the content (node name)
     *
     * @return node name
     */
    String getName();

    void setName(String name);


    /**
     * Returns content parent path
     *
     * @return absolute parent content path
     */
    String getParentPath();


    String getPath();

    /**
     * Sets content path
     *
     * @param path
     */
    void setParentPath(String path);


}
