package ru.nsu.ccfit.zuev.osu.online;


import com.edlplan.ui.fragment.WebViewFragment;
import com.reco1l.osu.ui.MessageDialog;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;

import java.util.Locale;

public class OnlinePanel extends Entity {
    private final Entity onlineLayer = new Entity();
    private final Entity messageLayer = new Entity();
    private final Entity frontLayer = new Entity();

    public Rectangle rect;

    private final ChangeableText rankText, nameText, ppText, accText;
    private final ChangeableText messageText, submessageText;
    private Sprite avatar = null;

    public OnlinePanel() {
        rect = new Rectangle(0, 0, 410, 110) {
            boolean moved = false;
            float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    this.setColor(0.3f, 0.3f, 0.3f, 0.9f);
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    this.setColor(0.2f, 0.2f, 0.2f, 0.5f);
                    if (!moved && OnlineManager.getInstance().isStayOnline()) {

                        new MessageDialog()
                            .setMessage(StringTable.get(com.osudroid.resources.R.string.dialog_visit_osudroid_profile_page))
                            .addButton("Yes", dialog -> {
                                new WebViewFragment().setURL(WebViewFragment.PROFILE_URL + OnlineManager.getInstance().getUserId()).show();
                                dialog.dismiss();
                                return null;
                            })
                            .addButton("No", dialog -> {
                                dialog.dismiss();
                                return null;
                            })
                            .show();
                    }
                    return true;
                }
                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    moved = true;
                    this.setColor(0.2f, 0.2f, 0.2f, 0.5f);
                }
                return false;
            }
        };
        rect.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        attachChild(rect);

        Rectangle avatarFooter = new Rectangle(0, 0, 110, 110);
        avatarFooter.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        attachChild(avatarFooter);
		
		/*Rectangle rightFooter = new Rectangle(Utils.toRes(410), 0, Utils.toRes(614), Utils.toRes(110));
		rightFooter.setColor(0.3f, 0.3f, 0.3f, 0.35f);
		attachChild(rightFooter);*/

        rankText = new ChangeableText(0, 0,
                ResourceManager.getInstance().getFont("CaptionFont"), "#1",
                HorizontalAlign.RIGHT, 12);
        rankText.setColor(0.6f, 0.6f, 0.6f, 0.9f);
        rankText.setScaleCenterX(0);
        rankText.setScale(1.7f);
        rankText.setPosition(390 + 10 - rankText.getWidthScaled(), 55);
        onlineLayer.attachChild(rankText);

        nameText = new ChangeableText(120, 5,
                ResourceManager.getInstance().getFont("CaptionFont"), "Guest", 16);
        onlineLayer.attachChild(nameText);
        ppText = new ChangeableText(120, 50,
                ResourceManager.getInstance().getFont("smallFont"), "Performance: 0pp",
                HorizontalAlign.LEFT, 25);
        ppText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(ppText);

        accText = new ChangeableText(120, 75,
                ResourceManager.getInstance().getFont("smallFont"), "Accuracy: 0.00%",
                HorizontalAlign.LEFT, 17);
        accText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(accText);

        messageText = new ChangeableText(110, 5,
                ResourceManager.getInstance().getFont("CaptionFont"), "Logging in...", 16);
        messageLayer.attachChild(messageText);

        submessageText = new ChangeableText(110, 60,
                ResourceManager.getInstance().getFont("smallFont"), "Connecting to server...", 40);
        messageLayer.attachChild(submessageText);

        attachChild(messageLayer);
        attachChild(frontLayer);

    }

    void setMessage(final String message, final String submessage) {
        messageText.setText(message);
        submessageText.setText(submessage);

        messageLayer.detachSelf();
        onlineLayer.detachSelf();
        attachChild(messageLayer);
    }

    public void setInfo() {
        nameText.setText(OnlineManager.getInstance().getUsername());

        ppText.setText(String.format(Locale.US, "Performance: %,dpp", Math.round(OnlineManager.getInstance().getPP())));

        accText.setText(String.format("Accuracy: %.2f%%",
                OnlineManager.getInstance().getAccuracy() * 100f));
        rankText.setScale(1);
        rankText.setText(String.format("#%d", OnlineManager.getInstance().getRank()));
        rankText.setPosition(390 + 10 - rankText.getWidth() * 1.7f, 55);
        rankText.setScaleCenterX(0);
        rankText.setScale(1.7f);

        messageLayer.detachSelf();
        onlineLayer.detachSelf();
        attachChild(onlineLayer);
    }

    public void setAvatar()
    {
        var avatarUrl = OnlineManager.getInstance().getAvatarURL();
        var textureName = OnlineScoring.getInstance().isAvatarLoaded() && !avatarUrl.isEmpty() ? avatarUrl : null;
        setAvatar(textureName);
    }

    void setAvatar(final String texname) {
        if (avatar != null)
            avatar.detachSelf();
        avatar = null;
        if (texname == null) return;
        TextureRegion tex = ResourceManager.getInstance().getAvatarTextureIfLoaded(texname);
        if (tex == null) return;

        Debug.i("Avatar is set!");
        avatar = new Sprite(0, 0, 110, 110, tex);
        frontLayer.attachChild(avatar);
    }
}