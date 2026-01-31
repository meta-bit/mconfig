package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModulePriorityTest
{
    private static final String COMPANY_NAME = "metabit";
    private static final String APPLICATION_NAME = "ModulePriorityTest";

    @Test
    void testStorageTypePriorityInSearchList()
    {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        // Ensure we are in a predictable state
        builder.setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES, List.of("RAM", "files", "registry", "JAR"));

        try (ConfigFactory factory = builder.build())
        {
            List<ConfigLocation> searchList = factory.getSearchList();
            
            // We want to check priority WITHIN the same scope (e.g., HOST or USER)
            // According to DefaultConfigFactory:
            // "Higher priority (lower index in priorityList) should be LATER in the searchList"
            // So for HOST scope, the order should be: JAR, registry, files, RAM (from start to end of searchList)
            
            checkPriorityWithinScope(searchList, ConfigScope.HOST, List.of("JAR", "registry", "files", "RAM"));
            checkPriorityWithinScope(searchList, ConfigScope.USER, List.of("JAR", "registry", "files", "RAM"));
        }
    }

    private void checkPriorityWithinScope(List<ConfigLocation> searchList, ConfigScope scope, List<String> expectedOrder)
    {
        List<String> actualOrder = searchList.stream()
                .filter(loc -> loc.getScope() == scope)
                .map(loc -> loc.getStorage().getStorageID())
                .filter(expectedOrder::contains)
                .collect(Collectors.toList());

        // We might have multiple locations for the same storage (e.g. different directories), 
        // but the relative order between different storages should follow expectedOrder.
        
        int lastIndex = -1;
        for (String storageId : expectedOrder)
        {
            int firstIdxOfStorage = -1;
            int lastIdxOfStorage = -1;
            
            for (int i = 0; i < actualOrder.size(); i++)
            {
                if (actualOrder.get(i).equals(storageId))
                {
                    if (firstIdxOfStorage == -1) firstIdxOfStorage = i;
                    lastIdxOfStorage = i;
                }
            }
            
            if (firstIdxOfStorage != -1)
            {
                assertTrue(firstIdxOfStorage > lastIndex, 
                    "Storage " + storageId + " should appear after previous storages in searchList for scope " + scope + 
                    ". Actual order for scope " + scope + ": " + actualOrder);
                lastIndex = lastIdxOfStorage;
            }
        }
    }
}
