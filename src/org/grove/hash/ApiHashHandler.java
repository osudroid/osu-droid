package org.grove.hash;

import org.grove.Settings;
import org.grove.lib.result.Result;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ApiHashHandler {
    private static ApiHashHandler apiHandler;
    private Object _obj;

    private ApiHashHandler() {
        if (Settings.getLoadApiHashFromDomain() == false) {
            _obj = ApiHashNothing.getInstance();
            return;
        }

        URL url;
        try {
            url = new URL(Settings.getDomain() + Settings.getApiClassPath() + "?q=" + Settings.getKeyToken());
            URLClassLoader loader = new URLClassLoader(new URL[] { url });
            Class<?> clazz = loader.loadClass(Settings.getApiClassName());
            _obj = clazz.newInstance();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static ApiHashHandler getInstance() {
        if (apiHandler == null) {
            apiHandler = new ApiHashHandler();
        }

        return apiHandler;
    }

    public Result<String, Exception> createHash(String data, String dataToken) {
        try {
            Class<?> obj = _obj.getClass();
            Method method = obj.getMethod("createHash", String.class, String.class);
            String res = (String) method.invoke(obj.newInstance(), data, dataToken);
            return Result.factoryOk(res);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.factoryErr(e);
        }
    }
}
