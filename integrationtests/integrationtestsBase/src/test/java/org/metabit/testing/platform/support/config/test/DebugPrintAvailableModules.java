package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.util.ServiceLoader;

public class DebugPrintAvailableModules
{
    @Test
    void debupPrintMConfigEffectivelyAvailableModules()
        {
        System.out.println("mConfig logging interfaces:");
        ServiceLoader<ConfigLoggingInterface> loggers = ServiceLoader.load(org.metabit.platform.support.config.interfaces.ConfigLoggingInterface.class);
        for (ConfigLoggingInterface serviceInstance : loggers)
            {
            System.out.printf("\t%s\n", serviceInstance.getServiceModuleName());
            }

        System.out.println("mConfig storages:");
        ServiceLoader<org.metabit.platform.support.config.interfaces.ConfigStorageInterface> sl2 = ServiceLoader.load(org.metabit.platform.support.config.interfaces.ConfigStorageInterface.class);
        for (ConfigStorageInterface serviceInstance : sl2)
            {
            System.out.printf("\t%s\t%s\n", serviceInstance.getStorageID(), serviceInstance.getStorageName());
            }

        System.out.println("mConfig file formats:");
        ServiceLoader<org.metabit.platform.support.config.interfaces.ConfigFormatInterface> sl4 = ServiceLoader.load(org.metabit.platform.support.config.interfaces.ConfigFormatInterface.class);
        for (ConfigFormatInterface serviceInstance : sl4)
            {
            System.out.printf("\t%s\n", serviceInstance.getFormatID());
            }
        return;
        }


}
