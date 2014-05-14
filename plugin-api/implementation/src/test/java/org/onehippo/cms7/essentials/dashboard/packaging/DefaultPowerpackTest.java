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

package org.onehippo.cms7.essentials.dashboard.packaging;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;

import com.google.common.eventbus.EventBus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class DefaultPowerpackTest extends BaseTest {



    @Test(expected = IllegalArgumentException.class)
    public void testPowerpackNoInjection() throws Exception {
        PowerpackPackage powerpackPackage = new DefaultPowerpack();
        powerpackPackage.execute(getContext());
    }

    @Test
    public void testPowerpack() throws Exception {
        PowerpackPackage powerpackPackage = new DefaultPowerpack();
        injector.autowireBean(powerpackPackage);
        final String instructionPath = powerpackPackage.getInstructionPath();
        assertEquals("Expected default path", instructionPath, DefaultPowerpack.DEFAULT_INSTRUCTIONS_PATH);
        final Map<String, Object> properties = powerpackPackage.getProperties();
        assertEquals("Expected empty property set", 0, properties.size());
        final Set<String> groupNames = powerpackPackage.groupNames();
        assertEquals("Expected default group names", DefaultPowerpack.DEFAULT_GROUPS.size(), groupNames.size());
        assertEquals("Expected default group name", DefaultPowerpack.DEFAULT_GROUPS.iterator().next(), groupNames.iterator().next());
        final InstructionParser parser = powerpackPackage.getInstructionParser();
        assertNotNull(parser);
        final EventBus bus = powerpackPackage.getEventBus();
        assertNotNull(bus);
        final Instructions instructions = powerpackPackage.getInstructions();
        assertEquals("Expected no instructions", null, instructions);

    }
}