package ut.com.carolynvs.bamboo.plugin.gitversion;

import org.junit.Test;
import com.carolynvs.bamboo.plugin.gitversion.MyPluginComponent;
import com.carolynvs.bamboo.plugin.gitversion.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}