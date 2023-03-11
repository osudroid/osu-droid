package org.grove.hash;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ApiHashNothing {
    private static ApiHashNothing apiHashNothing;

    public static ApiHashNothing getInstance() {
        if (apiHashNothing == null) {
            apiHashNothing = new ApiHashNothing();
        }
        return apiHashNothing;
    }

    public String createHash(String data, String token) throws Exception {
        return "fH9tfG99dG99dG9tdH9tfH9tfH9tfH9tfG99dG9tfH99fG99dG9tfG99dG99dH9tdO99dG99fG99fH99dH/tdG99dG99dG9tfG99dG99dH9tfH9tfH99fG99fG99dH99fO9tdH99fH99dG99dH99dH99fG99fO9tfG99dG99dH8=";
    }
}
