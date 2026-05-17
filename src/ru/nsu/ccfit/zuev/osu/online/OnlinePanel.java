package ru.nsu.ccfit.zuev.osu.online;

import com.edlplan.ui.fragment.WebViewFragment;
import com.reco1l.osu.ui.MessageDialog;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.math.MathUtils;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;

import java.util.Locale;

public class OnlinePanel extends Entity {
    private final Entity onlineLayer = new Entity();
    private final Entity messageLayer = new Entity();
    private final Entity frontLayer = new Entity();

    public Rectangle rect;

    private final Text rankText, nameText, ppText, accText;
    private final Text messageText, submessageText;
    private Sprite profileBanner = null;
    private Sprite avatar = null;

    public OnlinePanel() {
        final VertexBufferObjectManager vbo = GlobalManager.getInstance().getEngine().getVertexBufferObjectManager();
        rect = new Rectangle(0, 0, 410, 110, vbo) {
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

        Rectangle avatarFooter = new Rectangle(0, 0, 110, 110, vbo);
        avatarFooter.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        attachChild(avatarFooter);
		
		/*Rectangle rightFooter = new Rectangle(Utils.toRes(410), 0, Utils.toRes(614), Utils.toRes(110));
		rightFooter.setColor(0.3f, 0.3f, 0.3f, 0.35f);
		attachChild(rightFooter);*/

        rankText = new Text(0, 0,
                ResourceManager.getInstance().getFont("CaptionFont"), "#1",
                12, new TextOptions(HorizontalAlign.RIGHT), vbo);
        rankText.setColor(0.6f, 0.6f, 0.6f, 0.9f);
        rankText.setScaleCenterX(0);
        rankText.setScale(1.7f);
        rankText.setPosition(390 + 10 - rankText.getWidthScaled(), 55);
        onlineLayer.attachChild(rankText);

        nameText = new Text(120, 5,
                ResourceManager.getInstance().getFont("CaptionFont"), "Guest", 16, vbo);
        onlineLayer.attachChild(nameText);
        ppText = new Text(120, 50,
                ResourceManager.getInstance().getFont("smallFont"), "Performance: 0pp",
                25, new TextOptions(HorizontalAlign.LEFT), vbo);
        ppText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(ppText);

        accText = new Text(120, 75,
                ResourceManager.getInstance().getFont("smallFont"), "Accuracy: 0.00%",
                17, new TextOptions(HorizontalAlign.LEFT), vbo);
        accText.setColor(0.85f, 0.85f, 0.9f);
        onlineLayer.attachChild(accText);

        messageText = new Text(110, 5,
                ResourceManager.getInstance().getFont("CaptionFont"), "Logging in...", 16, vbo);
        messageLayer.attachChild(messageText);

        submessageText = new Text(110, 60,
                ResourceManager.getInstance().getFont("smallFont"), "Connecting to server...", 40, vbo);
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

    public void setProfile(final String avatarTexName) {
        if (profileBanner != null) {
            profileBanner.detachSelf();
        }

        if (avatar != null) {
            avatar.detachSelf();
        }

        profileBanner = null;
        avatar = null;

        final VertexBufferObjectManager vbo = GlobalManager.getInstance().getEngine().getVertexBufferObjectManager();

        var profileBannerUrl = OnlineManager.getInstance().getProfileBannerURL();

        if (profileBannerUrl != null && !profileBannerUrl.isEmpty()) {
            var bannerTexture = ResourceManager.getInstance().getProfileBannerTextureIfLoaded(profileBannerUrl);

            if (bannerTexture != null) {
                profileBanner = new Sprite(0, 0, 410, 110, bannerTexture, vbo);
                profileBanner.setColor(0.6f, 0.6f, 0.6f);
                frontLayer.attachChild(profileBanner);
            }
        }

        if (avatarTexName == null) {
            return;
        }

        var avatarTexture = ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarTexName);

        if (avatarTexture == null) {
            return;
        }

        avatar = new Sprite(0, 0, 110, 110, avatarTexture, vbo);
        frontLayer.attachChild(avatar);
    }
}