package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.util.modifier.IModifier;

public class ModifierListener implements IEntityModifier.IEntityModifierListener {

    @Override
    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

    @Override
    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {}
}
