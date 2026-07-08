import java.util.*;

//Class for the information that have been matched by a Tregex pattern
public class MatchPattern {
    public String textMatch;
    public String typeOfPattern;
    public List<TupleConcept> conceptsInText;
    public String passiveVoice;
    public String passiveVoiceSR;
    public String verCondition;

    public MatchPattern(String text, String type)
    {
        this.textMatch = text;
        this.typeOfPattern = type;
        this.conceptsInText = new ArrayList<>();
    }

    public void setTextMatch(String textMatch) {
        this.textMatch = textMatch;
    }
    public String getTextMatch() {
        return textMatch;
    }

    public void setTypeOfPattern(String typeOfPattern) {
        this.typeOfPattern = typeOfPattern;
    }

    public void setConceptsInText(List<TupleConcept> conceptsInText) {
        this.conceptsInText = conceptsInText;
    }
    public List<TupleConcept> getConceptsInText() {
        return conceptsInText;
    }

    public void setPassiveVoice(String passiveVoiceType1){this.passiveVoice = passiveVoiceType1; }
    public String getPassiveVoice(){return passiveVoice;}

    public void setPassiveVoiceSR(String passiveVoiceSR1){this.passiveVoiceSR = passiveVoiceSR1; }
    public String getPassiveVoiceSR(){return passiveVoiceSR;}

    public String getTypeOfPattern() {
        return typeOfPattern;
    }

    public String getVerCondition() {
        return verCondition;
    }

    public void setVerCondition(String verCondition) {
        this.verCondition = verCondition;
    }
}
