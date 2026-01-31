package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.mockups.MockConfigLocation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for ConfigSearchList to ensure priority ordering remains correct.
 */
class ConfigSearchListTest
    {
    private ConfigSearchList searchList;

    @BeforeEach
    void setUp()
        {
        searchList = new ConfigSearchList();
        }

    private ConfigLocation mockLocation(ConfigScope scope, String description)
        {
        return new MockConfigLocation(scope, description);
        }

    @Test
    void testEmptyList()
        {
        assertTrue(searchList.isEmpty());
        assertEquals(0, searchList.size());
        }

    @Test
    void testInsertAtScopeStartPriorityOrdering()
        {
        ConfigLocation machine = mockLocation(ConfigScope.HOST, "HOST");
        ConfigLocation user = mockLocation(ConfigScope.USER, "USER");
        ConfigLocation product = mockLocation(ConfigScope.PRODUCT, "PRODUCT");

        searchList.insertAtScopeStart(machine, ConfigScope.HOST);
        searchList.insertAtScopeStart(user, ConfigScope.USER);
        searchList.insertAtScopeStart(product, ConfigScope.PRODUCT);

        List<ConfigLocation> entries = searchList.getEntries();
        assertEquals(3, entries.size());
        assertEquals(ConfigScope.USER, entries.get(0).getScope());
        assertEquals(ConfigScope.HOST, entries.get(1).getScope());
        assertEquals(ConfigScope.PRODUCT, entries.get(2).getScope());
        }

    @Test
    void testInsertAtScopeEndPriorityOrdering()
        {
        ConfigLocation machine = mockLocation(ConfigScope.HOST, "HOST");
        ConfigLocation user = mockLocation(ConfigScope.USER, "USER");
        ConfigLocation product = mockLocation(ConfigScope.PRODUCT, "PRODUCT");

        searchList.insertAtScopeEnd(machine, ConfigScope.HOST);
        searchList.insertAtScopeEnd(user, ConfigScope.USER);
        searchList.insertAtScopeEnd(product, ConfigScope.PRODUCT);

        List<ConfigLocation> entries = searchList.getEntries();
        assertEquals(3, entries.size());
        assertEquals(ConfigScope.USER, entries.get(0).getScope());
        assertEquals(ConfigScope.HOST, entries.get(1).getScope());
        assertEquals(ConfigScope.PRODUCT, entries.get(2).getScope());
        }

    @Test
    void testPrependAndAppendWithinSameScope()
        {
        ConfigLocation user1 = mockLocation(ConfigScope.USER, "USER1");
        ConfigLocation user2 = mockLocation(ConfigScope.USER, "USER2");
        ConfigLocation user3 = mockLocation(ConfigScope.USER, "USER3");

        // First one in USER scope
        searchList.insertAtScopeStart(user1, ConfigScope.USER);
        
        // Prepend to USER scope -> should be before user1
        searchList.insertAtScopeStart(user2, ConfigScope.USER);
        
        // Append to USER scope -> should be after user1 (and user2)
        searchList.insertAtScopeEnd(user3, ConfigScope.USER);

        List<ConfigLocation> entries = searchList.getEntries();
        assertEquals(3, entries.size());
        assertEquals("USER2", entries.get(0).toString());
        assertEquals("USER1", entries.get(1).toString());
        assertEquals("USER3", entries.get(2).toString());
        }

    @Test
    void testComplexMixedOrdering()
        {
        ConfigLocation session = mockLocation(ConfigScope.SESSION, "SESSION");
        ConfigLocation user1 = mockLocation(ConfigScope.USER, "USER1");
        ConfigLocation user2 = mockLocation(ConfigScope.USER, "USER2");
        ConfigLocation machine = mockLocation(ConfigScope.HOST, "HOST");
        ConfigLocation product = mockLocation(ConfigScope.PRODUCT, "PRODUCT");

        searchList.insertAtScopeEnd(product, ConfigScope.PRODUCT); // [P]
        searchList.insertAtScopeStart(user1, ConfigScope.USER);    // [U1, P]
        searchList.insertAtScopeEnd(machine, ConfigScope.HOST); // [U1, M, P]
        searchList.insertAtScopeStart(session, ConfigScope.SESSION); // [S, U1, M, P]
        searchList.insertAtScopeEnd(user2, ConfigScope.USER);      // [S, U1, U2, M, P]

        List<ConfigLocation> entries = searchList.getEntries();
        assertEquals(5, entries.size());
        assertEquals(ConfigScope.SESSION, entries.get(0).getScope());
        assertEquals(ConfigScope.USER, entries.get(1).getScope());
        assertEquals("USER1", entries.get(1).toString());
        assertEquals(ConfigScope.USER, entries.get(2).getScope());
        assertEquals("USER2", entries.get(2).toString());
        assertEquals(ConfigScope.HOST, entries.get(3).getScope());
        assertEquals(ConfigScope.PRODUCT, entries.get(4).getScope());
        }
    }
