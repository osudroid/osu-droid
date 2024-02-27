package ru.nsu.ccfit.zuev.osu.helper;

import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Created by Fuuko on 2015/2/22.
 */
public class TextButton extends Sprite {

    private final Text buttonText;

    public TextButton(Font font, String text) {
        this(font, text, 1.0f);
    }

    public TextButton(Font font, String text, float scale) {
        super(0, 0, ResourceManager.getInstance().getTexture(
                "button"), (VertexBufferObjectManager) null);
        buttonText = new Text(0, 0, font, text, 50, null);
        buttonText.setScale(scale);
        setColor(201 / 255f, 31 / 255f, 55 / 255f);
        this.setWidth(buttonText.getWidthScaled() + 80);
        this.setHeight(buttonText.getHeightScaled() + 20);

        float textX = (this.getWidth() - buttonText.getWidth()) / 2;
        float textY = (this.getHeight() - buttonText.getHeight()) / 2;
        buttonText.setPosition(textX, textY);
        setAlpha(0.7f);
        attachChild(buttonText);
    }

    @Override
    public void setWidth(float pWidth) {
        this.mWidth = pWidth;
        float textX = (this.getWidth() - buttonText.getWidth()) / 2;
        float textY = (this.getHeight() - buttonText.getHeight()) / 2;
        buttonText.setPosition(textX, textY);
        onUpdateVertices();
    }

    @Override
    public void setHeight(float pHeight) {
        this.mHeight = pHeight;
        float textX = (this.getWidth() - buttonText.getWidth()) / 2;
        float textY = (this.getHeight() - buttonText.getHeight()) / 2;
        buttonText.setPosition(textX, textY);
        onUpdateVertices();
    }

    public void setTextColor(float pRed, float pGreen, float pBlue) {
        buttonText.setColor(pRed, pGreen, pBlue);
    }

    public void setText(String text) {
        buttonText.setText(text);
        float textX = (this.getWidth() - buttonText.getWidth()) / 2;
        float textY = (this.getHeight() - buttonText.getHeight()) / 2;
        buttonText.setPosition(textX, textY);
    }
}
