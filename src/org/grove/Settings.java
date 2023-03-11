/*
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 *
 */

package org.grove;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */

public class Settings {

    /**
     * Domain: Server Url/Ip
     */
    private static final String Domain       = ""; // TODO Add Domain

    /**
     * DataToken: Allows Request to Server
     */
    private static final String DataToken    = ""; // TODO Add DataToken

    /**
     * KeyToken: Login Token if logged in
     */
    private static UUID KeyToken             = null; // TODO Add KeyToken

    /**
     * ApiClassName: Loaded file in path
     */
    private static final String ApiClassName = ""; // TODO Add ApiClassName;

    /**
     * ApiClassPath: Gets the Class file from Server
     */
    private static final String ApiClassPath = ""; // TODO Add ApiClassPath

    /**
     * LoadApiHashFromDomain:
     * If false: Does not load the hash from Server
     * If true: loads the hash from Server
     * */
    private static final boolean LoadApiHashFromDomain = false;

//--------------------------------------------------------------------------------------------------------------------//

    /**
     * Domain: Server Url/Ip
     */
    public static String getDomain() { return Domain; }

    /**
     * DataToken: Allows Request to Server
     */
    public static String getDataToken() { return DataToken; }

    /**
     * KeyToken: Login Token
     */
    public static UUID getKeyToken() { return KeyToken; }

    public static void setKeyToken(UUID keyToken) { KeyToken = keyToken; }

    /**
     * ApiClassName: Loaded file in path
     */
    public static String getApiClassName() { return ApiClassName; }

    /**
     * ApiClassPath: Gets the Class file from Server
     */
    public static String getApiClassPath() { return ApiClassPath; }

    /**
     * LoadApiHashFromDomain:
     * If false: Does not load the hash from Server
     * If true: loads the hash from Server
     * */
    public static boolean getLoadApiHashFromDomain() {return LoadApiHashFromDomain; }

}
