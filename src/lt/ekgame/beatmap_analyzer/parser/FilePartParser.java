package lt.ekgame.beatmap_analyzer.parser;

public abstract class FilePartParser<T> {

    protected abstract T parseLine(String line);


}
