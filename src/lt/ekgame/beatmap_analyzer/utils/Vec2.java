package lt.ekgame.beatmap_analyzer.utils;

public class Vec2 {

    double positionX, positionY;

    public Vec2(double positionX, double positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public Vec2 add(Vec2 position) {
        return new Vec2(positionX + position.getX(), positionY + position.getY());
    }

    public Vec2 subract(Vec2 position) {
        return new Vec2(positionX - position.getX(), positionY - position.getY());
    }

    public Vec2 scale(double scale) {
        return new Vec2(positionX * scale, positionY * scale);
    }

    public double length() {
        return Math.sqrt(positionX * positionX + positionY * positionY);
    }

    public double distance(Vec2 position) {
        return position.subract(this).length();
    }

    public double getX() {
        return positionX;
    }

    public void setX(double x) {
        this.positionX = x;
    }

    public double getY() {
        return positionY;
    }

    public void setY(double y) {
        this.positionY = y;
    }

    public Vec2 clone() {
        return new Vec2(positionX, positionY);
    }
}
