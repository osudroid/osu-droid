package ru.nsu.ccfit.zuev.osu.online;

import com.edlplan.ui.fragment.ConfirmDialogFragment;
import com.edlplan.ui.fragment.WebViewFragment;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.MathUtils;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osuplus.R;

public class OnlinePanel extends Entity {
    private Entity onlineLayer = new Entity();
    private Entity messageLayer = new Entity();
    private Entity frontLayer = new Entity();

    public Rectangle rect;

    private ChangeableText rankText, nameText, scoreText, accText;
    private ChangeableText messageText, submessageText;
    private Sprite avatar = null;

    public OnlinePanel() {
        rect = new Rectangle(0, 0, Utils.toRes(410), Utils.toRes(110)) {
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
                    if (!moved) {
                        if(OnlineManager.getInstance().isStayOnline()) {
                            new ConfirmDialogFragment()
                                .setMessage(R.string.dialog_visit_profile_page)
                                .showForResult(isAccepted -> {
                                    GlobalManager.getInstance().getMainActivity().runOnUiThread(() -> {
                                        new WebViewFragment().setURL(
                                            WebViewFragment.PROFILE_URL + OnlineManager.getInstance().getUserId())
                                        .show();
                                    });
                                });
                        }
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

        Rectangle avatarFooter = new Rectangle(0, 0, Utils.toRes(110), Utils.toRes(110));
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
        rankText.setPosition(Utils.toRes(390 + 10) - rankText.getWidthScaled(), Utils.toRes(55));
        onlineLayer.attachChild(rankText);

        nameText = new ChangeableText(Utils.toRes(120), Utils.toRes(5),
                ResourceManager.getInstance().getFont("CaptionFont"), "Guest", 16);
        onlineLayer.attachChild(nameText);
        scoreText = new ChangeableText(Utils.toRes(120), Utils.toRes(50),
                ResourceManager.getInstance().getFont("smallFont"), "Score: 0",
                HorizontalAlign.LEFT, 22);
        scoreText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(scoreText);

        accText = new ChangeableText(Utils.toRes(120), Utils.toRes(75),
                ResourceManager.getInstance().getFont("smallFont"), "Accuracy: 0.00%",
                HorizontalAlign.LEFT, 17);
        accText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(accText);

        messageText = new ChangeableText(Utils.toRes(110), Utils.toRes(5),
                ResourceManager.getInstance().getFont("CaptionFont"), "Logging in...", 16);
        messageLayer.attachChild(messageText);

        submessageText = new ChangeableText(Utils.toRes(110), Utils.toRes(60),
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

    void setInfo() {
        nameText.setText(OnlineManager.getInstance().getUsername());
        StringBuilder scoreBuilder = new StringBuilder("Score: ");
        scoreBuilder.append(OnlineManager.getInstance().getScore());
        for (int i = scoreBuilder.length() - 3; i > 7; i -= 3) {
            scoreBuilder.insert(i, ' ');
        }

        scoreText.setText(scoreBuilder.toString());

        accText.setText(String.format("Accuracy: %.2f%%",
                OnlineManager.getInstance().getAccuracy() * 100f));
        rankText.setScale(1);
        rankText.setText(String.format("#%d", OnlineManager.getInstance().getRank()));
        rankText.setPosition(Utils.toRes(390 + 10) - rankText.getWidth() * 1.7f, Utils.toRes(55));
        rankText.setScaleCenterX(0);
        rankText.setScale(1.7f);

        messageLayer.detachSelf();
        onlineLayer.detachSelf();
        attachChild(onlineLayer);
    }

    void setAvatar(final String texname) {
        if (avatar != null)
            avatar.detachSelf();
        avatar = null;
        if (texname == null) return;
        TextureRegion tex = ResourceManager.getInstance().getTextureIfLoaded(texname);
        if (tex == null) return;

        Debug.i("Avatar is set!");
        avatar = new Sprite(0, 0, Utils.toRes(110), Utils.toRes(110), tex);
        frontLayer.attachChild(avatar);
    }
}