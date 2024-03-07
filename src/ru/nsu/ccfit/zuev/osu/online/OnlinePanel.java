package ru.nsu.ccfit.zuev.osu.online;

import com.edlplan.ui.fragment.ConfirmDialogFragment;
import com.edlplan.ui.fragment.WebViewFragment;
import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.util.debug.Debug;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.math.MathUtils;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osuplus.R;

public class OnlinePanel extends Entity {
    private Entity onlineLayer = new Entity();
    private Entity messageLayer = new Entity();
    private Entity frontLayer = new Entity();

    public Rectangle rect;

    private Text rankText, nameText, scoreText, accText;
    private Text messageText, submessageText;
    private Sprite avatar = null;

    public OnlinePanel() {
        rect = new Rectangle(0, 0, Utils.toRes(410), Utils.toRes(110), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager()) {
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
                                .showForResult(isAccepted -> GlobalManager.getInstance().getMainActivity().runOnUiThread(() -> new WebViewFragment().setURL(
                                        WebViewFragment.PROFILE_URL + OnlineManager.getInstance().getUserId())
                                    .show()));
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

        Rectangle avatarFooter = new Rectangle(0, 0, Utils.toRes(110), Utils.toRes(110), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        avatarFooter.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        attachChild(avatarFooter);
		
		/*Rectangle rightFooter = new Rectangle(Utils.toRes(410), 0, Utils.toRes(614), Utils.toRes(110));
		rightFooter.setColor(0.3f, 0.3f, 0.3f, 0.35f);
		attachChild(rightFooter);*/

        rankText = new Text(0, 0,
                ResourceManager.getInstance().getFont("CaptionFont"), "#1", 12,
                new TextOptions(HorizontalAlign.RIGHT), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        rankText.setColor(0.6f, 0.6f, 0.6f, 0.9f);
        rankText.setScaleCenterX(0);
        rankText.setScale(1.7f);
        rankText.setPosition(Utils.toRes(390 + 10) - rankText.getWidthScaled(), Utils.toRes(55));
        onlineLayer.attachChild(rankText);

        nameText = new Text(Utils.toRes(120), Utils.toRes(5),
                ResourceManager.getInstance().getFont("CaptionFont"), "Guest", 16, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        onlineLayer.attachChild(nameText);
        scoreText = new Text(Utils.toRes(120), Utils.toRes(50),
                ResourceManager.getInstance().getFont("smallFont"), "Score: 0",
                22, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        scoreText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(scoreText);

        accText = new Text(Utils.toRes(120), Utils.toRes(75),
                ResourceManager.getInstance().getFont("smallFont"), "Accuracy: 0.00%",
                17, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        accText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(accText);

        messageText = new Text(Utils.toRes(110), Utils.toRes(5),
                ResourceManager.getInstance().getFont("CaptionFont"), "Logging in...", 16, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        messageLayer.attachChild(messageText);

        submessageText = new Text(Utils.toRes(110), Utils.toRes(60),
                ResourceManager.getInstance().getFont("smallFont"), "Connecting to server...", 40, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
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
        avatar = new Sprite(0, 0, Utils.toRes(110), Utils.toRes(110), tex, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        frontLayer.attachChild(avatar);
    }
}