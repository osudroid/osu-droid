
                    val response = request.execute().json
                    val imageLink = response.getString("ImageLink")

                    bannerUrl = response.getString("Url")

                    if (imageLink.isNotBlank()) {
                        WebRequest(imageLink).use { imageRequest ->
                            bannerFile.createNewFile()
                            imageRequest.execute().response.body!!.byteStream().writeToFile(bannerFile)
                        }
                    }
                }

            } catch (e: Exception) {
                bannerFile.delete()
                Log.e("BannerManager", "Failed to get banner while requesting server.", e)
            }

            if (bannerFile.exists()) {
                ResourceManager.getInstance().loadHighQualityFile("banner", bannerFile)

                val bannerSprite = object : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("banner")) {

                    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                        if (event.isActionDown) {
                            clearEntityModifiers()
                            registerEntityModifier(ScaleModifier(0.1f, scaleX, 0.95f))
                        }

                        if (event.isActionUp || event.isActionCancel || event.isActionOutside) {
                            clearEntityModifiers()
                            registerEntityModifier(ScaleModifier(0.1f, scaleX, 1f))

                            if (event.isActionUp && bannerUrl.isNotBlank()) {
                                GlobalManager.getInstance().mainActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bannerUrl)))
                            }
                        }

                        return true
                    }
                }
                bannerSprite.setScaleCenter(bannerSprite.width / 2f, bannerSprite.height / 2f)
                bannerSprite.setPosition(Config.getRES_WIDTH() - bannerSprite.width - 10f, Config.getRES_HEIGHT() - bannerSprite.height - 10f)
                bannerSprite.alpha = 0f
                bannerSprite.registerEntityModifier(AlphaModifier(0.2f, 0f, 1f))

                scene.attachChild(bannerSprite)
                scene.registerTouchArea(bannerSprite)
            }

        }
    }

}