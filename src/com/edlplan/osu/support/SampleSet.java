package com.edlplan.osu.support;

public enum SampleSet {
    None("None"), Soft("Soft"), Normal("Normal"), Drum("Drum");
    private final String value;

    SampleSet(String v) {
        value = v;
    }

    public static SampleSet parse(String s) {
        return switch (s) {
            case "0", "None" -> None;
            case "1", "Normal" -> Normal;
            case "2", "Soft" -> Soft;
            case "3", "Drum" -> Drum;
            default -> null;
        };
    }

    public static SampleSet fromName(String s) {
        return switch (s) {
            case "None" -> None;
            case "Normal" -> Normal;
            case "Soft" -> Soft;
            case "Drum" -> Drum;
            default -> null;
        };
    }

    public String value() {
        return value;
    }

	/*
	public SampleSet(){

	}

	public SampleSet(ValueType t){
		setType(t);
	}

	public void setType(ValueType type){
		this.type=type;
	}

	public ValueType getType(){
		return type;
	}*/

    @Override
    public String toString() {
        return value;
    }
}
