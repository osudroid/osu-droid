package ru.nsu.ccfit.zuev.osu;

/**
 * 加解密工具类
 *
 * @author dgsrz (dgsrz@vip.qq.com)
 */
public final class SecurityUtils {
    /**
     * @return 得到整数x高20位对3389取模后的结果
     */
    public static int getHigh16Bits(int x) {
        return (x >> 12) % 3389;
    }
}