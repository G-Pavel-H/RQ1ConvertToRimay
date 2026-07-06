import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;

public class SegmentAndType {

    private ArrayList<TaggedWord> sentTagged;
    private String typeSeg;
    private Condition condition;

    public SegmentAndType(ArrayList<TaggedWord> s, String t){
        this.sentTagged = s;
        this.typeSeg = t;
    }

    public ArrayList<TaggedWord> getSentTagged() {
        return sentTagged;
    }

    public String getTypeSeg() {
        return typeSeg;
    }

    public void setSentTagged(ArrayList<TaggedWord> sentTagged) {
        this.sentTagged = sentTagged;
    }

    public void setTypeSeg(String typeSeg) {
        this.typeSeg = typeSeg;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }
}
