package org.metabit.platform.support.config.impl.format.yaml.jackson;

public class YAMLTestPojo
{
    private String name;
    private String version;
    private boolean enabled;
    private int count;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
