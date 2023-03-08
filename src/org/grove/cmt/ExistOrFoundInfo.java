package org.grove.cmt;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ExistOrFoundInfo<T> {
    private boolean existOrFound;
    private T value;

    public boolean valueIsSet() {
        return existOrFound;
    }

    public T getValue() {
        return value;
    }

    public ExistOrFoundInfo<T> setValue(T value) {
        this.existOrFound = true;
        this.value = value;
        return this;
    }

    public T getValueOr(T value) {
        if (valueIsSet())
            return this.value;
        return value;
    }
}
