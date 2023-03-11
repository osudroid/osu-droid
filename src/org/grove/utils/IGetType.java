package org.grove.utils;
import java.lang.reflect.Type;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public interface IGetType {
    default public Type getType() {
        return this.getClass().getComponentType();
    }
}
