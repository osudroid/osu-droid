package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;

public class GameUtil
{

    public static IEntityModifier newFadeModifier(float time, boolean isHidden)
    {
        if (isHidden)
        {
            return new SequenceEntityModifier(
                    new FadeInModifier(time / 4 * GameHelper.getTimeMultiplier()),
                    new FadeOutModifier(time / 4 * GameHelper.getTimeMultiplier())
            );
        }

        return new FadeInModifier(time / 2 * GameHelper.getTimeMultiplier());
    }
}
